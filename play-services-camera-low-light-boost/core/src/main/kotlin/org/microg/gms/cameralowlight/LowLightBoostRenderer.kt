/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.cameralowlight

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.EGLExt
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.ArrayDeque
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs

internal class LowLightBoostRenderer(
    private val outputSurface: Surface,
    private val captureWidth: Int,
    private val captureHeight: Int,
    private val onBoostStrengthChanged: (Float) -> Unit,
    private val onRendererFailure: () -> Unit,
    initialEnabled: Boolean,
) {
    private val thread = HandlerThread("LowLightBoostGL")
    private lateinit var handler: Handler

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var program = 0
    private var aPosition = 0
    private var aTexCoord = 0
    private var uStMatrix = 0
    private var uStrength = 0
    private var uTexture = 0

    private var oesTextureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    lateinit var inputSurface: Surface
        private set
    private var inputSurfaceReleased = false

    private var measureFbo = 0
    private var measureTextureId = 0

    private var outputWidth = captureWidth
    private var outputHeight = captureHeight

    @Volatile
    private var enabled = initialEnabled

    @Volatile
    private var releaseRequested = false

    @Volatile
    private var glReleased = false
    private var strength = 0f
    private var frameCounter = 0
    private var lastReportedBoostStrength = -1f
    private var rendererFailureReported = false
    private val timestampLock = Any()
    private val pendingCaptureTimestamps = ArrayDeque<Long>(MAX_PENDING_CAPTURE_TIMESTAMPS)
    private var lastPresentationTimestamp = 0L

    private val stMatrix = FloatArray(16)
    private val vertexBuffer: FloatBuffer = floatBuffer(FULLSCREEN_QUAD)
    private val texBuffer: FloatBuffer = floatBuffer(QUAD_TEXCOORDS)
    private val measurePixels: ByteBuffer =
        ByteBuffer.allocateDirect(MEASURE_SIZE * MEASURE_SIZE * 4).order(ByteOrder.nativeOrder())

    fun start(): Boolean {
        if (releaseRequested) return false
        thread.start()
        handler = Handler(thread.looper)
        val initialized = runOnGlThread("initialize renderer", ::initGl)
        if (!initialized) release()
        return initialized
    }

    fun setEnabled(enable: Boolean) {
        enabled = enable
    }

    fun queueCaptureTimestamp(timestampNanos: Long) {
        if (timestampNanos <= 0 || releaseRequested) return
        synchronized(timestampLock) {
            if (pendingCaptureTimestamps.size >= MAX_PENDING_CAPTURE_TIMESTAMPS) {
                pendingCaptureTimestamps.removeFirst()
            }
            pendingCaptureTimestamps.addLast(timestampNanos)
        }
    }

    private fun resolvePresentationTimestamp(surfaceTimestamp: Long): Long {
        val matchedCaptureTimestamp = synchronized(timestampLock) {
            var bestMatch: Long? = null
            var bestDelta = Long.MAX_VALUE
            val iterator = pendingCaptureTimestamps.iterator()
            while (iterator.hasNext()) {
                val candidate = iterator.next()
                if (candidate <= lastPresentationTimestamp ||
                    surfaceTimestamp > 0 && candidate < surfaceTimestamp - TIMESTAMP_MATCH_TOLERANCE_NANOS
                ) {
                    iterator.remove()
                    continue
                }
                if (surfaceTimestamp > 0) {
                    val delta = abs(candidate - surfaceTimestamp)
                    if (delta <= TIMESTAMP_MATCH_TOLERANCE_NANOS && delta < bestDelta) {
                        bestMatch = candidate
                        bestDelta = delta
                    }
                }
            }
            bestMatch?.also { pendingCaptureTimestamps.remove(it) }
        }

        val candidate = matchedCaptureTimestamp ?: surfaceTimestamp
        if (candidate <= 0) return 0
        val monotonicTimestamp = if (candidate > lastPresentationTimestamp) candidate else lastPresentationTimestamp + 1
        lastPresentationTimestamp = monotonicTimestamp
        return monotonicTimestamp
    }

    @Synchronized
    fun release(): Boolean {
        if (!::handler.isInitialized && !thread.isAlive) {
            glReleased = true
            return true
        }
        if (glReleased && !thread.isAlive) return true
        releaseRequested = true
        if (::handler.isInitialized && thread.isAlive) {
            runOnGlThread("release renderer", ::releaseGl)
        } else if (!thread.isAlive) {
            releaseGl()
        }
        if (!glReleased) return false
        if (!thread.isAlive) return true
        thread.quitSafely()
        if (Looper.myLooper() == thread.looper) return false
        try {
            thread.join(GL_OPERATION_TIMEOUT_MILLIS)
            if (thread.isAlive) {
                Log.w(TAG, "Timed out waiting for GL thread to stop; interrupting it")
                thread.interrupt()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.w(TAG, "Interrupted while waiting for GL thread to stop", e)
        }
        return glReleased && !thread.isAlive
    }

    private fun runOnGlThread(operation: String, block: () -> Unit): Boolean {
        if (!::handler.isInitialized || !thread.isAlive) return false
        if (Looper.myLooper() == thread.looper) {
            return try {
                block()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to $operation", e)
                false
            }
        }

        val latch = CountDownLatch(1)
        var failure: Throwable? = null
        if (!handler.post {
                try {
                    block()
                } catch (t: Throwable) {
                    failure = t
                } finally {
                    latch.countDown()
                }
            }) {
            Log.w(TAG, "GL thread rejected request to $operation")
            return false
        }

        val completed = try {
            latch.await(GL_OPERATION_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.w(TAG, "Interrupted while waiting to $operation", e)
            false
        }
        if (!completed) {
            Log.e(TAG, "Timed out waiting to $operation")
            return false
        }
        failure?.let { cause ->
            if (cause is Error) throw cause
            Log.e(TAG, "Failed to $operation", cause)
            return false
        }
        return true
    }

    private fun initGl() {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "no EGL display" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) { "eglInitialize failed" }

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
            "eglChooseConfig failed"
        }
        val eglConfig = checkNotNull(configs[0]) { "no EGL config" }

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)
        check(eglContext != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed" }

        eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, eglConfig, outputSurface, intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "eglCreateWindowSurface failed" }
        check(EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) { "eglMakeCurrent failed" }

        val dim = IntArray(1)
        if (EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_WIDTH, dim, 0) && dim[0] > 0) outputWidth = dim[0]
        if (EGL14.eglQuerySurface(eglDisplay, eglSurface, EGL14.EGL_HEIGHT, dim, 0) && dim[0] > 0) outputHeight = dim[0]

        program = buildProgram()
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        uStMatrix = GLES20.glGetUniformLocation(program, "uStMatrix")
        uStrength = GLES20.glGetUniformLocation(program, "uStrength")
        uTexture = GLES20.glGetUniformLocation(program, "uTexture")
        check(aPosition >= 0 && aTexCoord >= 0 && uStMatrix >= 0 && uStrength >= 0 && uTexture >= 0) {
            "required shader location is missing"
        }

        oesTextureId = genTexture()
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        setupMeasureTarget()

        val st = SurfaceTexture(oesTextureId)
        surfaceTexture = st
        st.setDefaultBufferSize(captureWidth, captureHeight)
        st.setOnFrameAvailableListener { drawFrame() }
        inputSurface = Surface(st)
        checkGlError("initialize renderer")
    }

    private fun drawFrame() {
        if (releaseRequested) return
        val st = surfaceTexture ?: return
        try {
            st.updateTexImage()
            st.getTransformMatrix(stMatrix)

            if (frameCounter++ % MEASURE_EVERY_N_FRAMES == 0) {
                val target = if (enabled) {
                    val brightness = measureBrightness()
                    ((BOOST_PIVOT - brightness) / BOOST_PIVOT).coerceIn(0f, 1f)
                } else {
                    0f
                }
                strength += (target - strength) * STRENGTH_SMOOTHING
                reportBoostStrength(strength)
            }

            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glViewport(0, 0, outputWidth, outputHeight)
            drawQuad(strength)
            checkGlError("render output frame")

            val presentationTimestamp = resolvePresentationTimestamp(st.timestamp)
            if (presentationTimestamp > 0) {
                check(EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, presentationTimestamp)) {
                    "eglPresentationTimeANDROID failed"
                }
            }
            check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) { "eglSwapBuffers failed" }
        } catch (t: Throwable) {
            reportRendererFailure(t)
            if (t is Error) throw t
        }
    }

    private fun reportRendererFailure(cause: Throwable) {
        if (rendererFailureReported || releaseRequested) return
        rendererFailureReported = true
        Log.e(TAG, "Low light boost renderer failed", cause)
        try {
            onRendererFailure()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to report renderer failure", e)
            release()
        }
    }

    private fun measureBrightness(): Float {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, measureFbo)
        GLES20.glViewport(0, 0, MEASURE_SIZE, MEASURE_SIZE)
        drawQuad(0f)
        measurePixels.rewind()
        GLES20.glReadPixels(0, 0, MEASURE_SIZE, MEASURE_SIZE, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, measurePixels)
        checkGlError("measure frame brightness")
        measurePixels.rewind()
        var sum = 0.0
        val count = MEASURE_SIZE * MEASURE_SIZE
        repeat(count) {
            val r = measurePixels.get().toInt() and 0xFF
            val g = measurePixels.get().toInt() and 0xFF
            val b = measurePixels.get().toInt() and 0xFF
            measurePixels.get()
            sum += (0.299 * r + 0.587 * g + 0.114 * b)
        }
        return (sum / count / 255.0).toFloat()
    }

    private fun drawQuad(boostStrength: Float) {
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(uTexture, 0)
        GLES20.glUniformMatrix4fv(uStMatrix, 1, false, stMatrix, 0)
        GLES20.glUniform1f(uStrength, boostStrength)

        vertexBuffer.rewind()
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        texBuffer.rewind()
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, texBuffer)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
    }

    private fun reportBoostStrength(boostStrength: Float) {
        if (lastReportedBoostStrength < 0f || abs(boostStrength - lastReportedBoostStrength) > BOOST_STRENGTH_REPORT_DELTA) {
            lastReportedBoostStrength = boostStrength
            onBoostStrengthChanged(boostStrength)
        }
    }

    private fun setupMeasureTarget() {
        measureTextureId = genTexture()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, measureTextureId)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, MEASURE_SIZE, MEASURE_SIZE, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        val fbo = IntArray(1)
        GLES20.glGenFramebuffers(1, fbo, 0)
        measureFbo = fbo[0]
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, measureFbo)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, measureTextureId, 0
        )
        check(GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) == GLES20.GL_FRAMEBUFFER_COMPLETE) {
            "brightness framebuffer is incomplete"
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        checkGlError("initialize brightness target")
    }

    private fun releaseGl() {
        if (glReleased) return
        try {
            surfaceTexture?.setOnFrameAvailableListener(null)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear frame listener during renderer cleanup", e)
        }
        if (::inputSurface.isInitialized && !inputSurfaceReleased) {
            try {
                inputSurface.release()
                inputSurfaceReleased = true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release renderer input surface", e)
            }
        }
        surfaceTexture?.let { texture ->
            try {
                texture.release()
                surfaceTexture = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release renderer surface texture", e)
            }
        }
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            var contextCurrent = false
            if (eglSurface != EGL14.EGL_NO_SURFACE && eglContext != EGL14.EGL_NO_CONTEXT) {
                contextCurrent = EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                if (!contextCurrent) logEglFailure("eglMakeCurrent for cleanup")
            }
            if (contextCurrent) {
                if (program != 0) GLES20.glDeleteProgram(program)
                if (measureFbo != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(measureFbo), 0)
                val textures = intArrayOf(oesTextureId, measureTextureId).filter { it != 0 }.toIntArray()
                if (textures.isNotEmpty()) GLES20.glDeleteTextures(textures.size, textures, 0)
                val glError = GLES20.glGetError()
                if (glError != GLES20.GL_NO_ERROR) {
                    Log.w(TAG, "OpenGL cleanup reported error 0x${glError.toString(16)}")
                }
            }

            if (!EGL14.eglMakeCurrent(
                    eglDisplay,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_SURFACE,
                    EGL14.EGL_NO_CONTEXT,
                )
            ) {
                logEglFailure("eglMakeCurrent detach")
            }

            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                if (EGL14.eglDestroySurface(eglDisplay, eglSurface)) {
                    eglSurface = EGL14.EGL_NO_SURFACE
                } else {
                    val error = logEglFailure("eglDestroySurface")
                    if (error == EGL14.EGL_BAD_SURFACE || error == EGL14.EGL_BAD_DISPLAY) {
                        eglSurface = EGL14.EGL_NO_SURFACE
                    }
                }
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                if (EGL14.eglDestroyContext(eglDisplay, eglContext)) {
                    eglContext = EGL14.EGL_NO_CONTEXT
                } else {
                    val error = logEglFailure("eglDestroyContext")
                    if (error == EGL14.EGL_BAD_CONTEXT || error == EGL14.EGL_BAD_DISPLAY) {
                        eglContext = EGL14.EGL_NO_CONTEXT
                    }
                }
            }
            if (EGL14.eglTerminate(eglDisplay)) {
                markEglDisplayReleased()
            } else {
                val error = logEglFailure("eglTerminate")
                if (error == EGL14.EGL_BAD_DISPLAY || error == EGL14.EGL_NOT_INITIALIZED) {
                    markEglDisplayReleased()
                }
            }
        } else {
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
        }
        glReleased = (!::inputSurface.isInitialized || inputSurfaceReleased) &&
                surfaceTexture == null &&
                eglDisplay == EGL14.EGL_NO_DISPLAY &&
                eglContext == EGL14.EGL_NO_CONTEXT &&
                eglSurface == EGL14.EGL_NO_SURFACE
    }

    private fun markEglDisplayReleased() {
        program = 0
        measureFbo = 0
        oesTextureId = 0
        measureTextureId = 0
        eglDisplay = EGL14.EGL_NO_DISPLAY
        eglContext = EGL14.EGL_NO_CONTEXT
        eglSurface = EGL14.EGL_NO_SURFACE
    }

    private fun logEglFailure(operation: String): Int {
        val error = EGL14.eglGetError()
        Log.e(TAG, "$operation failed with EGL error 0x${error.toString(16)}")
        return error
    }

    private fun checkGlError(operation: String) {
        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) { "$operation failed with OpenGL error 0x${error.toString(16)}" }
    }

    private fun genTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        check(ids[0] != 0) { "glGenTextures returned no texture" }
        return ids[0]
    }

    private fun buildProgram(): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        var fs = 0
        try {
            fs = compileShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)
            val prog = GLES20.glCreateProgram()
            check(prog != 0) { "glCreateProgram failed" }
            GLES20.glAttachShader(prog, vs)
            GLES20.glAttachShader(prog, fs)
            GLES20.glLinkProgram(prog)
            val status = IntArray(1)
            GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, status, 0)
            if (status[0] != GLES20.GL_TRUE) {
                val message = GLES20.glGetProgramInfoLog(prog)
                GLES20.glDeleteProgram(prog)
                error("link failed: $message")
            }
            return prog
        } finally {
            GLES20.glDeleteShader(vs)
            if (fs != 0) GLES20.glDeleteShader(fs)
        }
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "glCreateShader failed" }
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] != GLES20.GL_TRUE) {
            val message = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("compile failed: $message")
        }
        return shader
    }

    companion object {
        // EGL_ANDROID_recordable is available before the SDK exposed EGLExt.EGL_RECORDABLE_ANDROID.
        private const val EGL_RECORDABLE_ANDROID = 0x3142
        private const val GL_OPERATION_TIMEOUT_MILLIS = 5_000L
        private const val MAX_PENDING_CAPTURE_TIMESTAMPS = 32
        private const val TIMESTAMP_MATCH_TOLERANCE_NANOS = 1_000_000L
        private const val MEASURE_SIZE = 16
        private const val MEASURE_EVERY_N_FRAMES = 8
        private const val BOOST_PIVOT = 0.55f
        private const val STRENGTH_SMOOTHING = 0.15f
        private const val BOOST_STRENGTH_REPORT_DELTA = 0.02f

        private val FULLSCREEN_QUAD = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        private val QUAD_TEXCOORDS = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f, 1f, 1f)

        private const val VERTEX_SHADER = """
            uniform mat4 uStMatrix;
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uStMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES uTexture;
            uniform float uStrength;
            varying vec2 vTexCoord;
            void main() {
                vec4 color = texture2D(uTexture, vTexCoord);
                float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                float gain = 1.0 + uStrength * (1.0 - luma) * 2.0;
                vec3 boosted = color.rgb * gain;
                float invGamma = 1.0 / (1.0 + uStrength * 0.6);
                boosted = pow(clamp(boosted, 0.0, 1.0), vec3(invGamma));
                gl_FragColor = vec4(boosted, color.a);
            }
        """

        private fun floatBuffer(data: FloatArray): FloatBuffer =
            ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
                put(data)
                position(0)
            }
    }
}
