package org.microg.gms.maps.markup;

import android.content.Context;
import org.oscim.layers.marker.MarkerItem;

public interface Markup {
    public MarkerItem getMarkerItem(Context context);

    public String getId();

    public static interface MarkupListener {
        void update(Markup markup);

        void remove(Markup markup);
    }
}
