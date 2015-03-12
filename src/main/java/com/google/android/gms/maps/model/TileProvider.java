package com.google.android.gms.maps.model;

/**
 * An interface for a class that provides the tile images for a TileOverlay. For information about
 * the tile coordinate system, see TileOverlay.
 * <p/>
 * Calls to methods in this interface might be made from multiple threads so implementations of
 * this interface must be threadsafe.
 */
public interface TileProvider {
    public static final Tile NO_TILE = new Tile(-1, -1, null);

    /**
     * Returns the tile to be used for this tile coordinate.
     *
     * @param x    The x coordinate of the tile. This will be in the range [0, 2^(zoom - 1)] inclusive.
     * @param y    The y coordinate of the tile. This will be in the range [0, 2^(zoom - 1)] inclusive.
     * @param zoom The zoom level of the tile.
     * @return the {@link Tile} to be used for this tile coordinate. If you do not wish to provide
     * a tile for this tile coordinate, return {@link #NO_TILE}. If the tile could not be found at
     * this point in time, return null and further requests might be made with an exponential
     * backoff.
     */
    public Tile getTile(int x, int y, int zoom);
}
