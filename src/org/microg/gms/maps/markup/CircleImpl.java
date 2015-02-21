/*
 * Copyright 2013-2015 µg Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.maps.markup;

import android.content.Context;
import android.os.RemoteException;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.internal.ICircleDelegate;

import org.oscim.android.gl.AndroidGL;
import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Color;
import org.oscim.core.Box;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.MapRenderer;

import static org.oscim.core.MercatorProjection.groundResolution;
import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

public class CircleImpl extends ICircleDelegate.Stub implements Markup {

    private final String id;
    private final CircleOptions options;
    private final MarkupListener listener;
    private CircleLayer layer;
    private Point point;
    private float drawRadius;

    public CircleImpl(String id, CircleOptions options, MarkupListener listener) {
        this.id = id;
        this.listener = listener;
        this.options = options == null ? new CircleOptions() : options;
        LatLng center = this.options.getCenter();
        if (center != null) {
            point = new Point(longitudeToX(center.longitude), latitudeToY(center.latitude));
            drawRadius = (float) (options.getRadius() / groundResolution(center.latitude, 1));
        }
    }

    @Override
    public void remove() throws RemoteException {
        listener.remove(this);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setCenter(LatLng center) throws RemoteException {
        options.center(center);
        point = new Point(longitudeToX(center.longitude), latitudeToY(center.latitude));
        drawRadius = (float) (options.getRadius() / groundResolution(center.latitude, 1));
    }

    @Override
    public LatLng getCenter() throws RemoteException {
        return options.getCenter();
    }

    @Override
    public void setRadius(double radius) throws RemoteException {
        options.radius(radius);
        if (point != null) {
            this.drawRadius = (float) (options.getRadius() / groundResolution(options.getCenter().latitude, 1));
        }
    }

    @Override
    public double getRadius() throws RemoteException {
        return options.getRadius();
    }

    @Override
    public void setStrokeWidth(float width) throws RemoteException {
        options.strokeWidth(width);
    }

    @Override
    public float getStrokeWidth() throws RemoteException {
        return options.getStrokeWidth();
    }

    @Override
    public void setStrokeColor(int color) throws RemoteException {
        options.strokeColor(color);
    }

    @Override
    public int getStrokeColor() throws RemoteException {
        return options.getStrokeColor();
    }

    @Override
    public void setFillColor(int color) throws RemoteException {
        options.fillColor(color);
        listener.update(this);
    }

    @Override
    public int getFillColor() throws RemoteException {
        return options.getFillColor();
    }

    @Override
    public void setZIndex(float zIndex) throws RemoteException {
        options.zIndex(zIndex);
    }

    @Override
    public float getZIndex() throws RemoteException {
        return options.getZIndex();
    }

    @Override
    public void setVisible(boolean visible) throws RemoteException {
        options.visible(visible);
    }

    @Override
    public boolean isVisible() throws RemoteException {
        return options.isVisible();
    }

    @Override
    public boolean equalsRemote(ICircleDelegate other) throws RemoteException {
        return other != null && other.getId().equals(getId());
    }

    @Override
    public int hashCodeRemote() throws RemoteException {
        return id.hashCode();
    }

    @Override
    public MarkerItem getMarkerItem(Context context) {
        return null;
    }

    @Override
    public Layer getLayer(Context context, Map map) {
        if (layer == null) {
            layer = new CircleLayer(map);
        }
        return layer;
    }

    @Override
    public Type getType() {
        return Type.LAYER;
    }

    private class CircleLayer extends Layer {

        public CircleLayer(Map map) {
            super(map);
            mRenderer = new CircleRenderer();
        }

        private class CircleRenderer extends LayerRenderer {
            private final Box bBox = new Box();
            private final Point screenPoint = new Point();
            private final Point indicatorPosition = new Point();
            private AndroidGL GL = new AndroidGL();

            private int shader;
            private int vertexPosition;
            private int matrixPosition;
            private int phase;
            private int scale;
            private int direction;
            private int color;

            @Override
            public void update(GLViewport viewport) {
                if (!isEnabled()) {
                    setReady(false);
                    return;
                }

                if (!viewport.changed() && isReady()) return;

                setReady(true);

                int width = mMap.getWidth();
                int height = mMap.getHeight();

                // clamp location to a position that can be
                // savely translated to screen coordinates
                viewport.getBBox(bBox, 0);

                double x = point.x;
                double y = point.y;

                // get position of Location in pixel relative to
                // screen center
                viewport.toScreenPoint(x, y, screenPoint);

                x = screenPoint.x + width / 2;
                y = screenPoint.y + height / 2;

                viewport.fromScreenPoint(x, y, indicatorPosition);
            }

            @Override
            public void render(GLViewport viewport) {

                GLState.useProgram(shader);
                GLState.blend(true);
                GLState.test(false, false);

                GLState.enableVertexArrays(vertexPosition, -1);
                MapRenderer.bindQuadVertexVBO(vertexPosition);

                float radius = (float) (drawRadius * viewport.pos.scale);
                GL.uniform1f(scale, radius);

                double x = indicatorPosition.x - viewport.pos.x;
                double y = indicatorPosition.y - viewport.pos.y;
                double tileScale = Tile.SIZE * viewport.pos.scale;

                viewport.mvp.setTransScale((float) (x * tileScale), (float) (y * tileScale), 1);
                viewport.mvp.multiplyMM(viewport.viewproj, viewport.mvp);
                viewport.mvp.setAsUniform(matrixPosition);
                GL.uniform1f(phase, 1);
                GL.uniform2f(direction, 0, 0);
                float alpha = Color.aToFloat(options.getFillColor());
                GL.uniform4f(color,
                        Color.rToFloat(options.getFillColor()) * alpha,
                        Color.gToFloat(options.getFillColor()) * alpha,
                        Color.bToFloat(options.getFillColor()) * alpha,
                        alpha);

                GL.drawArrays(GL20.GL_TRIANGLE_STRIP, 0, 4);
            }

            @Override
            public boolean setup() {
                shader = GLShader.createProgram(vShaderStr, fShaderStr);
                if (shader == 0)
                    return false;
                vertexPosition = GL.getAttribLocation(shader, "a_pos");
                matrixPosition = GL.getUniformLocation(shader, "u_mvp");
                phase = GL.getUniformLocation(shader, "u_phase");
                scale = GL.getUniformLocation(shader, "u_scale");
                direction = GL.getUniformLocation(shader, "u_dir");
                color = GL.getUniformLocation(shader, "u_color");
                return true;
            }
        }
    }


    private final static String vShaderStr = ""
            + "precision mediump float;"
            + "uniform mat4 u_mvp;"
            + "uniform float u_phase;"
            + "uniform float u_scale;"
            + "attribute vec2 a_pos;"
            + "varying vec2 v_tex;"
            + "void main() {"
            + "  gl_Position = u_mvp * vec4(a_pos * u_scale * u_phase, 0.0, 1.0);"
            + "  v_tex = a_pos;"
            + "}";

    private final static String fShaderStr = ""
            + "precision mediump float;"
            + "varying vec2 v_tex;"
            + "uniform float u_scale;"
            + "uniform float u_phase;"
            + "uniform vec2 u_dir;"
            + "uniform vec4 u_color;"

            + "void main() {"
            + "  float len = 1.0 - length(v_tex);"
            + "  float a = smoothstep(0.0, 2.0 / u_scale, len);"
            + "  gl_FragColor = u_color * a;"
            + "}";
}
