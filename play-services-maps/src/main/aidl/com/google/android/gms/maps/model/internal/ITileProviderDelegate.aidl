package com.google.android.gms.maps.model.internal;

import com.google.android.gms.maps.model.Tile;

interface ITileProviderDelegate {
    Tile getTile(int x, int y, int zoom);
}
