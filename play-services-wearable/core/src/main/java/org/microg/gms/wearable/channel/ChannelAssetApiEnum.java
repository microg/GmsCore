package org.microg.gms.wearable.channel;

public enum ChannelAssetApiEnum {
    ORIGIN_CHANNEL_API(0),
    ORIGIN_LARGE_ASSET_API(1);

    public final int id;

    ChannelAssetApiEnum(int id) {
        this.id = id;
    }

    public static ChannelAssetApiEnum fromId(int id) {
        for (ChannelAssetApiEnum v : values()) {
            if (v.id == id) return v;
        }
        return ORIGIN_CHANNEL_API;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }
}
