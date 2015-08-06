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

import org.oscim.layers.Layer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.map.Map;

public interface Markup {
    public MarkerItem getMarkerItem(Context context);

    public Layer getLayer(Context context, Map map);

    public Type getType();

    public String getId();

    public boolean onClick();

    public boolean isValid();

    public static enum Type {
        MARKER, LAYER, DRAWABLE
    }

    public static interface MarkupListener {
        void update(Markup markup);

        void remove(Markup markup);

        boolean onClick(Markup markup);
    }
}
