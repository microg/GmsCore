/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Handler
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebViewClientCompat
import com.google.android.gms.recaptcha.RecaptchaHandle
import com.google.android.gms.recaptcha.RecaptchaResultData
import com.google.android.gms.recaptcha.internal.ExecuteParams
import com.google.android.gms.recaptcha.internal.InitParams
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okio.ByteString
import org.microg.gms.profile.Build
import org.microg.gms.profile.ProfileManager
import org.microg.gms.tasks.TaskImpl
import org.microg.gms.utils.toBase64
import java.io.ByteArrayInputStream
import java.lang.reflect.Array
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.Locale
import java.util.Queue
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "RecaptchaWeb"

@RequiresApi(19)
class RecaptchaWebImpl(private val context: Context, private val packageName: String, private val lifecycle: Lifecycle) : RecaptchaImpl, LifecycleOwner {
    private var webView: WebView? = null
    private var lastRequestToken: String? = null
    private var initFinished = AtomicBoolean(true)
    private var initContinuation: Continuation<Unit>? = null
    private var executeFinished = AtomicBoolean(true)
    private var executeContinuation: Continuation<String>? = null

    override fun getLifecycle(): Lifecycle = lifecycle

    override suspend fun init(params: InitParams): RecaptchaHandle {
        lastRequestToken = UUID.randomUUID().toString()
        ProfileManager.ensureInitialized(context)
        FakeHandler.setDecryptKeyPrefix(IntArray(0))
        FakeApplication.context = context
        FakeApplication.packageNameOverride = packageName
        suspendCoroutine { continuation ->
            initFinished.set(false)
            initContinuation = continuation
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                addJavascriptInterface(RNJavaScriptInterface(this@RecaptchaWebImpl, CodeInterpreter(this@RecaptchaWebImpl)), "RN")
                webViewClient = object : WebViewClientCompat() {
                    fun String.isRecaptchaUrl() = startsWith("https://www.recaptcha.net/") || startsWith("https://www.gstatic.com/recaptcha/")

                    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
                        if (url.isRecaptchaUrl()) {
                            return null
                        }
                        return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(byteArrayOf()))
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                        return !url.isRecaptchaUrl()
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                    }
                }
                postUrl(
                    MWV_URL, ("" +
                            "k=${URLEncoder.encode(params.siteKey, "UTF-8")}&" +
                            "pk=${URLEncoder.encode(packageName, "UTF-8")}&" +
                            "mst=ANDROID_ONPLAY&" +
                            "msv=18.1.1&" +
                            "msi=${URLEncoder.encode(lastRequestToken, "UTF-8")}&" +
                            "mov=${Build.VERSION.SDK_INT}"
                            ).toByteArray()
                )
            }
            lifecycleScope.launch {
                delay(10000)
                if (!initFinished.getAndSet(true)) {
                    try {
                        continuation.resumeWithException(RuntimeException("Timeout reached"))
                    } catch (_: Exception) {}
                }
            }
        }
        initContinuation = null
        return RecaptchaHandle(params.siteKey, packageName, emptyList())
    }

    override suspend fun execute(params: ExecuteParams): RecaptchaResultData {
        if (webView == null) {
            init(InitParams().apply { siteKey = params.handle.siteKey; version = params.version })
        }
        val additionalArgs = mutableMapOf<String, String>()
        for (key in params.action.additionalArgs.keySet()) {
            additionalArgs[key] = params.action.additionalArgs.getString(key)!!
        }
        val request = RecaptchaExecuteRequest(token = lastRequestToken, action = params.action.toString(), additionalArgs = additionalArgs).encode().toBase64(Base64.URL_SAFE, Base64.NO_WRAP)
        val token = suspendCoroutine { continuation ->
            executeFinished.set(false)
            executeContinuation = continuation
            eval("recaptcha.m.Main.execute(\"${request}\")")
            lifecycleScope.launch {
                delay(10000)
                if (!executeFinished.getAndSet(true)) {
                    try {
                        continuation.resumeWithException(RuntimeException("Timeout reached"))
                    } catch (_: Exception) {}
                }
            }
        }
        return RecaptchaResultData(token)
    }

    override suspend fun close(handle: RecaptchaHandle): Boolean {
        if (handle.clientPackageName != null && handle.clientPackageName != packageName) throw IllegalArgumentException("invalid handle")
        val closed = webView != null
        webView?.stopLoading()
        webView?.loadUrl("about:blank")
        webView = null
        return closed
    }

    private fun eval(script: String) {
        Log.d(TAG, "eval: $script")
        webView?.let {
            Handler(context.mainLooper).post {
                it.evaluateJavascript(script, null)
            }
        }
    }

    protected fun finalize() {
        FakeApplication.packageNameOverride = ""
    }

    companion object {
        private const val MWV_URL = "https://www.recaptcha.net/recaptcha/api3/mwv"
        private const val DEBUG = true
        object FakeApplication : Application() {
            var context: Context
                get() = baseContext
                set(value) { try { attachBaseContext(value.applicationContext) } catch (_: Exception) { } }
            var packageNameOverride: String = ""
            override fun getPackageName(): String {
                return packageNameOverride
            }
        }
        var codeDecryptKeyPrefix = emptyList<Int>()
            private set

        class FakeHandler : Exception() {
            private var cloudProjectNumber: Long? = 0
            private var nonce: String? = null

            @Keep
            fun requestIntegrityToken(request: FakeHandler): Task<FakeHandler> {
                return Tasks.forException(FakeHandler())
            }

            @Keep
            fun setCloudProjectNumber(cloudProjectNumber: Long): FakeHandler {
                this.cloudProjectNumber = cloudProjectNumber
                return this
            }

            @Keep
            fun setNonce(nonce: String): FakeHandler {
                this.nonce = nonce
                return this
            }

            @Keep
            fun build(): FakeHandler {
                return this
            }

            @Keep
            fun cloudProjectNumber(): Long? {
                return cloudProjectNumber
            }

            @Keep
            fun nonce(): String? {
                return nonce
            }

            @Keep
            fun getErrorCode(): Int = -1
            
            companion object {
                @Keep
                @JvmStatic
                fun setDecryptKeyPrefix(newKeyPrefix: IntArray) {
                    codeDecryptKeyPrefix = newKeyPrefix.asList()
                }

                @Keep
                @JvmStatic
                fun getFakeApplication(): Application = FakeApplication

                @Keep
                @JvmStatic
                fun createFakeIntegrityManager(context: Context): FakeHandler {
                    return FakeHandler()
                }
                @Keep
                @JvmStatic
                fun createFakeIntegrityTokenRequestBuilder(): FakeHandler {
                    return FakeHandler()
                }
            }
        }

        private class CodeInterpreter(private val impl: RecaptchaWebImpl) {
            val dict = mutableMapOf<Int, Any?>()
            var errorHandler = ""
            var xorSecret = IntRange(0, 127).random().toByte()

            private val intToClassMap = mapOf(
                1 to java.lang.Integer.TYPE,
                2 to java.lang.Short.TYPE,
                3 to java.lang.Byte.TYPE,
                4 to java.lang.Long.TYPE,
                5 to java.lang.Character.TYPE,
                6 to java.lang.Float.TYPE,
                7 to java.lang.Double.TYPE,
                8 to java.lang.Boolean.TYPE,
                9 to FakeHandler::class.java
            )

            private fun getClass(name: String): Class<*>? = when (name) {
                "[I" -> IntArray::class.java
                "[B" -> ByteArray::class.java
                "android.os.Build" -> Build::class.java
                "android.os.Build\$VERSION" -> Build.VERSION::class.java
                "android.app.ActivityThread" -> FakeHandler::class.java
                "com.google.android.play.core.integrity.IntegrityManager" -> FakeHandler::class.java
                "com.google.android.play.core.integrity.IntegrityManagerFactory" -> FakeHandler::class.java
                "com.google.android.play.core.integrity.IntegrityTokenRequest" -> FakeHandler::class.java
                "com.google.android.play.core.integrity.IntegrityTokenResponse" -> FakeHandler::class.java
                "android.content.Intent", "android.content.IntentFilter", "android.content.BroadcastReceiver",
                "android.content.Context", "android.content.pm.PackageManager", "android.content.ContentResolver",
                "java.lang.String", "java.lang.CharSequence", "java.lang.Long",
                "java.nio.charset.Charset", "java.nio.charset.StandardCharsets",
                "android.text.format.DateFormat", "java.util.Date", "java.util.Locale", "java.nio.ByteBuffer",
                "android.os.BatteryManager", "android.media.AudioManager",
                "com.google.android.gms.tasks.OnCompleteListener",
                "android.provider.Settings\$System" -> Class.forName(name)

                else -> {
                    Log.w(TAG, "Not providing class $name", Exception())
                    if (DEBUG) Class.forName(name) else null
                }
            }

            private fun getMethod(cls: Class<*>, name: String, params: kotlin.Array<Class<*>?>): Method? = when {
                cls == FakeHandler::class.java && name == "acx" -> FakeHandler::class.java.getMethod("setDecryptKeyPrefix", *params)
                cls == FakeHandler::class.java && name == "currentApplication" -> FakeHandler::class.java.getMethod("getFakeApplication", *params)
                cls == FakeHandler::class.java && name == "create" -> FakeHandler::class.java.getMethod("createFakeIntegrityManager", *params)
                cls == FakeHandler::class.java && name == "builder" -> FakeHandler::class.java.getMethod("createFakeIntegrityTokenRequestBuilder", *params)
                cls == FakeHandler::class.java -> cls.getMethod(name, *params)
                cls == FakeApplication.javaClass && name == "getContentResolver" -> cls.getMethod(name, *params)
                cls == FakeApplication.javaClass && name == "getSystemService" -> cls.getMethod(name, *params)
                cls == FakeApplication.javaClass && name == "registerReceiver" -> cls.getMethod(name, *params)
                cls == PackageManager::class.java && name == "checkPermission" -> cls.getMethod(name, *params)
                cls == Context::class.java && name == "checkSelfPermission" -> cls.getMethod(name, *params)
                cls == AudioManager::class.java && name == "getStreamVolume" -> cls.getMethod(name, *params)
                cls == Settings.System::class.java && name == "getInt" -> cls.getMethod(name, *params)
                cls == DateFormat::class.java -> cls.getMethod(name, *params)
                cls == Locale::class.java -> cls.getMethod(name, *params)
                cls == Intent::class.java -> cls.getMethod(name, *params)
                cls == String::class.java -> cls.getMethod(name, *params)
                cls == ByteBuffer::class.java -> cls.getMethod(name, *params)
                cls == TaskImpl::class.java -> cls.getMethod(name, *params)
                name == "toString" -> cls.getMethod(name, *params)
                name == "parseLong" -> cls.getMethod(name, *params)
                else -> {
                    Log.w(TAG, "Not providing method $name in ${cls.display()}", Exception())
                    if (DEBUG) cls.getMethod(name, *params) else null
                }
            }

            private fun getField(cls: Class<*>, name: String): Field? = when {
                cls == Build::class.java -> cls.getField(name)
                cls == Build.VERSION::class.java -> cls.getField(name)
                cls == Settings.System::class.java && cls.getField(name).modifiers.and(Modifier.STATIC) > 0 -> cls.getField(name)
                cls == BatteryManager::class.java && cls.getField(name).modifiers.and(Modifier.STATIC) > 0 -> cls.getField(name)
                cls == AudioManager::class.java && cls.getField(name).modifiers.and(Modifier.STATIC) > 0 -> cls.getField(name)
                cls == StandardCharsets::class.java && cls.getField(name).modifiers.and(Modifier.STATIC) > 0 -> cls.getField(name)
                else -> {
                    Log.w(TAG, "Not providing field $name in ${cls.display()}", Exception())
                    if (DEBUG) cls.getField(name) else null
                }
            }

            private operator fun Any?.rem(other: Any?): Any? = when {
                this is IntArray && other is Int -> map { it % other }.toIntArray()
                else -> throw UnsupportedOperationException("rem ${this?.javaClass} % ${other?.javaClass}")
            }

            private infix fun Any?.xor(other: Any?): Any? = when {
                this is String && other is Int -> map { it.code xor other }.toIntArray()
                this is String && other is Byte -> encodeToByteArray().map { (it.toInt() xor other.toInt()).toByte() }.toByteArray()
                this is Long && other is Long -> this xor other
                else -> throw UnsupportedOperationException("xor ${this?.javaClass} ^ ${other?.javaClass}")
            }

            private fun Any?.join(): Any? = when (this) {
                is ByteArray -> decodeToString()
                is CharArray -> concatToString()
                is IntArray -> joinToString(",", "[", "]")
                is LongArray -> joinToString(",", "[", "]")
                is ShortArray -> joinToString(",", "[", "]")
                is FloatArray -> joinToString(",", "[", "]")
                is DoubleArray -> joinToString(",", "[", "]")
                is kotlin.Array<*> -> joinToString(",", "[", "]")
                is Iterable<*> -> joinToString(",", "[", "]")
                else -> this
            }

            private fun String.deXor(): String = map { Char(it.code xor xorSecret.toInt()) }.toCharArray().concatToString()

            private fun Any?.deXor(): Any? = when {
                this is RecaptchaWebCode.Arg && this.asObject() is String -> this.asObject()!!.deXor()
                this is String -> this.deXor()
                else -> this
            }

            private fun Any.asClass(): Class<*>? = when (this) {
                is RecaptchaWebCode.Arg -> asObject()!!.asClass()
                is Int -> intToClassMap[this]!!
                is String -> getClass(this)
                is Class<*> -> this
                else -> throw UnsupportedOperationException("$this.asClass()")
            }

            private fun Any?.getClass(): Class<*> = when (this) {
                is RecaptchaWebCode.Arg -> asObject().getClass()
                is Class<*> -> this
                null -> Unit.javaClass
                else -> this.javaClass
            }

            private fun Any?.display(): String = when (this) {
                is RecaptchaWebCode.Arg -> asObject().display() + if (index != null) " (d[$index])" else ""
                is Int, is Boolean -> "${this}"
                is Byte -> "${this}b"
                is Short -> "${this}s"
                is Long -> "${this}l"
                is Double -> "${this}d"
                is Float -> "${this}f"
                is String -> if (any { !it.isLetterOrDigit() && it !in listOf('.', '=', '-', '_') }) "<string with complex chars>" else "\"${this}\""
                is Class<*> -> name
                is Constructor<*> -> "{new ${declaringClass.name}(${parameterTypes.joinToString { it.name }})}"
                is Method -> "{${declaringClass.name}.$name(${parameterTypes.joinToString { it.name }})}"
                is Field -> "{${declaringClass.name}.$name}"
                is IntArray -> joinToString(prefix = "[", postfix = "]")
                is ByteArray -> joinToString(prefix = "[", postfix = "]b")
                is ShortArray -> joinToString(prefix = "[", postfix = "]s")
                is LongArray -> joinToString(prefix = "[", postfix = "]l")
                is FloatArray -> joinToString(prefix = "[", postfix = "]f")
                is DoubleArray -> joinToString(prefix = "[", postfix = "]d")
                is BooleanArray -> joinToString(prefix = "[", postfix = "]")
                null -> "null"
                else -> "@{${this.javaClass.name}}"
            }

            private fun RecaptchaWebCode.Arg.asObject(): Any? = when {
                index != null -> dict[index]
                bol != null -> bol
                bt != null -> bt[0]
                chr != null -> chr[0]
                sht != null -> sht.toShort()
                i != null -> i
                l != null -> l
                flt != null -> flt
                dbl != null -> dbl
                str != null -> str
                else -> null
            }

            private fun Any.asListValue(): RecaptchaWebList.Value = when(this) {
                is Int -> RecaptchaWebList.Value(i = this)
                is Short -> RecaptchaWebList.Value(sht = this.toInt())
                is Byte -> RecaptchaWebList.Value(bt = ByteString.of(this))
                is Long -> RecaptchaWebList.Value(l = this)
                is Double -> RecaptchaWebList.Value(dbl = this)
                is Float -> RecaptchaWebList.Value(flt = this)
                is Boolean -> RecaptchaWebList.Value(bol = this)
                is Char -> RecaptchaWebList.Value(chr = this.toString())
                is String -> RecaptchaWebList.Value(str = this)
                else -> RecaptchaWebList.Value(str = toString())
            }

            fun execute(code: RecaptchaWebCode) {
                for (op in code.ops) {
                    when (op.code) {
                        1 -> {
                            // d[i] = a0
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${op.args[0].display()}")
                            dict[op.arg1!!] = op.args[0].asObject()
                        }

                        2 -> {
                            // d[i] = a0 .. a1
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = \"${op.args[0].display()}${op.args[1].display()}\"")
                            dict[op.arg1!!] = "${op.args[0].asObject()}${op.args[1].asObject()}"
                        }

                        3 -> {
                            // d[i] = Class(a0)
                            val cls = op.args[0].asObject().deXor()?.asClass()
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = $cls")
                            dict[op.arg1!!] = cls
                        }

                        4 -> {
                            // d[i] = Class(a0).getConstructor(a1 ...)
                            val constructor = op.args[0].asClass()!!.getConstructor(*op.args.subList(1, op.args.size).map { it.asClass() }.toTypedArray())
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${constructor.display()}")
                            dict[op.arg1!!] = constructor
                        }

                        5 -> {
                            // d[i] = Class(a0).getMethod(a1, a2 ...)
                            val methodName = (op.args[1].asObject().deXor() as String)
                            val cls = op.args[0].getClass()
                            val method = getMethod(cls, methodName, op.args.subList(2, op.args.size).map { it.asClass() }.toTypedArray())
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${method.display()}")
                            dict[op.arg1!!] = method
                        }

                        6 -> {
                            // d[i] = Class(a0).getField(a1)
                            val fieldName = (op.args[1].asObject().deXor() as String)
                            val cls = op.args[0].getClass()
                            val field = getField(cls, fieldName)
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${field.display()}")
                            dict[op.arg1!!] = field
                        }

                        7 -> {
                            // d[i] = Constructor(a0).newInstance(a1 ...)
                            if (DEBUG) Log.d(
                                TAG,
                                "d[${op.arg1}] = new ${(op.args[0].asObject() as Constructor<*>).name}(${
                                    op.args.subList(1, op.args.size).joinToString { it.display() }
                                })"
                            )
                            dict[op.arg1!!] =
                                (op.args[0].asObject() as Constructor<*>).newInstance(*op.args.subList(1, op.args.size).map { it.asObject() }.toTypedArray())
                        }

                        8 -> {
                            // d[i] = Method(a0).invoke(a1, a2 ...)
                            if (DEBUG) Log.d(
                                TAG,
                                "d[${op.arg1}] = (${op.args[1].display()}).${(op.args[0].asObject() as Method).name}(${
                                    op.args.subList(2, op.args.size).joinToString { it.display() }
                                })"
                            )
                            dict[op.arg1!!] = (op.args[0].asObject() as Method).invoke(
                                op.args[1].asObject(),
                                *op.args.subList(2, op.args.size).map { it.asObject() }.toTypedArray()
                            )
                        }

                        9 -> {
                            // d[i] = Method(a0).invoke(null, a1 ...)
                            if (DEBUG) Log.d(
                                TAG,
                                "d[${op.arg1}] = ${(op.args[0].asObject() as Method).declaringClass.name}.${(op.args[0].asObject() as Method).name}(${
                                    op.args.subList(
                                        1,
                                        op.args.size
                                    ).joinToString { it.display() }
                                })"
                            )
                            dict[op.arg1!!] =
                                (op.args[0].asObject() as Method).invoke(null, *op.args.subList(1, op.args.size).map { it.asObject() }.toTypedArray())
                        }

                        10 -> {
                            // d[i] = Field(a0).get(a1)
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = (${op.args[1].display()}).${(op.args[0].asObject() as Field).name}")
                            dict[op.arg1!!] = (op.args[0].asObject() as Field).get(op.args[1].asObject())
                        }

                        11 -> {
                            // d[i] = Field(a0).get(null)
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${(op.args[0].asObject() as Field).declaringClass.name}.${(op.args[0].asObject() as Field).name}")
                            dict[op.arg1!!] = (op.args[0].asObject() as Field).get(null)
                        }

                        12 -> {
                            // Field(a0).set(a1, a2)
                            if (DEBUG) Log.d(TAG, "(${op.args[1].display()}).${(op.args[0].asObject() as Field).name} = ${op.args[2].display()}")
                            (op.args[0].asObject() as Field).set(op.args[1].asObject(), op.args[2].asObject())
                        }

                        13 -> {
                            // Field(a0).set(null, a1)
                            if (DEBUG) Log.d(
                                TAG,
                                "(${(op.args[0].asObject() as Field).declaringClass.name}).${(op.args[0].asObject() as Field).name} = ${op.args[1].display()}"
                            )
                            (op.args[0].asObject() as Field).set(null, op.args[1].asObject())
                        }

                        15 -> {
                            // eval(a0(a1))
                            impl.eval("${op.args[0].str}(\"${op.args[1].asObject()}\")")
                        }

                        17 -> {
                            // d[i] = new a0[a1]
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = new ${op.args[0].asClass()!!.name}[${op.args[1].display()}]")
                            dict[op.arg1!!] = Array.newInstance(op.args[0].asClass(), op.args[1].asObject() as Int)
                        }

                        18 -> {
                            // d[i] = new a1() { * a2(args) { eval(a0(args)); return a3; } }
                            val callbackName = op.args[0].asObject() as String
                            val methodName = (op.args[2].asObject() as String).deXor()
                            val cls = op.args[1].asObject().deXor()?.asClass()
                            val returnValue = op.args[3].asObject()
                            val argsTarget = (if (op.args.size == 5) op.args[4].asObject() as? Int else null) ?: -1
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = new ${cls?.name}() { * ${methodName}(*) { js:$callbackName(*); return ${returnValue.display()}; } }")
                            dict[op.arg1!!] =
                                Proxy.newProxyInstance(cls!!.classLoader, arrayOf(cls)) { obj: Any, method: Method, args: kotlin.Array<Any>? ->
                                    if (method.name == methodName) {
                                        if (argsTarget != -1) dict[argsTarget] = args
                                        val encoded = RecaptchaWebList(args.orEmpty().map { it.asListValue() }).encode().toBase64(Base64.URL_SAFE, Base64.NO_WRAP)
                                        impl.eval("${callbackName}(\"$encoded\")")
                                        returnValue
                                    } else {
                                        null
                                    }
                                }
                        }

                        19 -> {
                            // d[i] = new Queue(a1)
                            // d[a0] = new a2() { * a3(args) { d[i].add(args); return a4; } }
                            val methodName = (op.args[3].asObject() as String).deXor()
                            val maxSize = op.args[1].asObject() as Int
                            val queue = ArrayDeque<List<Any>>(maxSize)
                            val limitedQueue = object : Queue<List<Any>> by queue {
                                override fun add(element: List<Any>?): Boolean {
                                    if (maxSize == 0) return true
                                    if (size == maxSize) remove()
                                    queue.add(element)
                                    return true
                                }
                            }
                            val returnValue = if (op.args.size == 5) op.args[4].asObject() else null
                            val cls = op.args[2].asObject().deXor()?.asClass()
                            dict[op.arg1!!] = limitedQueue
                            dict[op.args[0].asObject() as Int] = Proxy.newProxyInstance(cls!!.classLoader, arrayOf(cls)) { obj: Any, method: Method, args: kotlin.Array<Any>? ->
                                if (method.name == methodName) {
                                    limitedQueue.add(args?.asList().orEmpty())
                                    returnValue
                                } else {
                                    null
                                }
                            }
                        }

                        20 -> {
                            // unset(d, a0 ...)
                            if (DEBUG) Log.d(TAG, "d[${op.args.joinToString { it.index.toString() }}] = @@@")
                            for (arg in op.args) {
                                dict.remove(arg.index)
                            }
                        }

                        26 -> {
                            // e = a0
                            errorHandler = op.args[0].str!!
                        }

                        27 -> {
                            // clear(d)
                            dict.clear()
                        }

                        30 -> {
                            // d[i] = encode(a0 ...)
                            val res = RecaptchaWebList(op.args.map { it.asObject()!!.asListValue() }).encode().toBase64(Base64.URL_SAFE, Base64.NO_WRAP)
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${res.display()}")
                            dict[op.arg1!!] = res
                        }

                        31 -> {
                            // a0[a1] = a2
                            if (DEBUG) Log.d(TAG, "d[${op.args[0].index}][${op.args[1].display()}] = ${op.args[2].display()}")
                            Array.set(op.args[0].asObject()!!, op.args[1].asObject() as Int, op.args[2].asObject())
                        }

                        32 -> {
                            // d[i] = a0[a1]
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${op.args[0].display()}[${op.args[1].display()}]")
                            val arr = op.args[0].asObject()
                            val idx = op.args[1].asObject() as Int
                            val res = when (arr) {
                                is String -> arr[idx]
                                is List<*> -> arr[idx]
                                else -> Array.get(arr, idx)
                            }
                            dict[op.arg1!!] = res
                        }

                        34 -> {
                            // d[i] = a0 % a1
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${op.args[0].display()} % ${op.args[1].display()}")
                            dict[op.arg1!!] = op.args[0].asObject() % op.args[1].asObject()
                        }

                        35 -> {
                            // d[i] = a0 ^ a1
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${op.args[0].display()} ^ ${op.args[1].display()}")
                            dict[op.arg1!!] = op.args[0].asObject() xor op.args[1].asObject()
                        }

                        37 -> {
                            // d[i] = String(a1[*a0])
                            val str = op.args[1].asObject() as String
                            val res = (op.args[0].asObject() as IntArray).map { str[it] }.toCharArray().concatToString()
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${res.display()}")
                            dict[op.arg1!!] = res
                        }

                        38 -> {
                            // x = a0
                            xorSecret = op.args[0].asObject() as Byte
                        }

                        39 -> {
                            // d[i] = join(a0)
                            val res = op.args[0].asObject().join()
                            if (DEBUG) Log.d(TAG, "d[${op.arg1}] = ${res.display()}")
                            dict[op.arg1!!] = res
                        }

                        else -> {
                            Log.w(TAG, "Op ${op.encode().toBase64(Base64.URL_SAFE, Base64.NO_WRAP)} not implemented (code=${op.code})")
                        }
                    }
                }
            }
        }

        private class RNJavaScriptInterface(private val impl: RecaptchaWebImpl, private val interpreter: CodeInterpreter) {

            @JavascriptInterface
            fun zzoed(input: String) {
                val result = RecaptchaWebResult.ADAPTER.decode(Base64.decode(input, Base64.URL_SAFE))
                if (DEBUG) Log.d(TAG, "zzoed: $result")
                if (!impl.executeFinished.getAndSet(true) && impl.lastRequestToken == result.requestToken) {
                    if (result.code == 1 && result.token != null) {
                        impl.executeContinuation?.resume(result.token)
                    } else {
                        impl.executeContinuation?.resumeWithException(RuntimeException("Status ${result.code}"))
                    }
                }
            }

            @JavascriptInterface
            fun zzoid(input: String) {
                val status = RecaptchaWebStatusCode.ADAPTER.decode(Base64.decode(input, Base64.URL_SAFE))
                if (DEBUG) Log.d(TAG, "zzoid: $status")
                if (!impl.initFinished.getAndSet(true)) {
                    if (status.code == 1) {
                        impl.initContinuation?.resume(Unit)
                    } else {
                        impl.initContinuation?.resumeWithException(RuntimeException("Status ${status.code}"))
                    }
                }
            }

            @JavascriptInterface
            fun zzrp(input: String) {
                val callback = RecaptchaWebEncryptedCallback.ADAPTER.decode(Base64.decode(input, Base64.URL_SAFE))
                var key = (codeDecryptKeyPrefix + callback.key).reduce { a, b -> a xor b }
                fun next(): Int {
                    key = ((key * 4391) + 277) % 32779
                    return key % 255
                }

                val decrypted = callback.data_?.map { Char(it.code xor next()) }?.toCharArray()?.concatToString()
                if (DEBUG) Log.d(TAG, "zzrp: $decrypted")
                val code = RecaptchaWebCode.ADAPTER.decode(Base64.decode(decrypted, Base64.URL_SAFE + Base64.NO_PADDING))
                interpreter.execute(code)
            }
        }

    }
}