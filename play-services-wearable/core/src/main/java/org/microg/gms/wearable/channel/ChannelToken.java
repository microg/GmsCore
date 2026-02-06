package org.microg.gms.wearable.channel;

import android.util.Base64;

import com.google.android.gms.wearable.internal.ChannelParcelable;

import org.microg.gms.wearable.proto.AppKey;

public final class ChannelToken {
    private static final String TAG = "ChannelToken";
    private static final String TOKEN_PREFIX = "chl-";

    public final String nodeId;
    public final AppKey appKey;
    public final long channelId;
    public final boolean thisNodeWasOpener;
    public final boolean isReliable;

    public ChannelToken(String nodeId, AppKey appKey, long channelId,
                        boolean thisNodeWasOpener, boolean isReliable) {
        if (nodeId == null) throw new NullPointerException("nodeId is null");
        if (appKey == null) throw new NullPointerException("appKey is null");
        if (channelId < 0) throw new IllegalArgumentException("Negative channelId: " + channelId);

        this.nodeId = nodeId;
        this.appKey = appKey;
        this.channelId = channelId;
        this.thisNodeWasOpener = thisNodeWasOpener;
        this.isReliable = isReliable;
    }

    public static ChannelToken fromString(AppKey expectedAppKey, String tokenString)
            throws InvalidChannelTokenException {
        if (expectedAppKey == null) throw new NullPointerException("expectedAppKey is null");
        if (tokenString == null || !tokenString.startsWith(TOKEN_PREFIX)) {
            throw new InvalidChannelTokenException("Invalid token prefix");
        }

        try {
            byte[] data = Base64.decode(tokenString.substring(TOKEN_PREFIX.length()), Base64.DEFAULT);
            ChannelTokenProto proto = ChannelTokenProto.parseFrom(data);

            if (proto.nodeId == null || proto.packageName == null ||
                    proto.signatureDigest == null || proto.channelId < 0) {
                throw new InvalidChannelTokenException("Missing required fields");
            }

            AppKey tokenAppKey = new AppKey(proto.packageName, proto.signatureDigest);
            if (!expectedAppKey.equals(tokenAppKey)) {
                throw new InvalidChannelTokenException("AppKey mismatch");
            }

            return new ChannelToken(
                    proto.nodeId,
                    tokenAppKey,
                    proto.channelId,
                    proto.thisNodeWasOpener,
                    proto.isReliable
            );
        } catch (InvalidChannelTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidChannelTokenException("Failed to parse token", e);
        }
    }

    public String toTokenString() {
        ChannelTokenProto proto = new ChannelTokenProto();
        proto.nodeId = nodeId;
        proto.packageName = appKey.packageName;
        proto.signatureDigest = appKey.signatureDigest;
        proto.channelId = channelId;
        proto.thisNodeWasOpener = thisNodeWasOpener;
        proto.isReliable = isReliable;

        return TOKEN_PREFIX + Base64.encodeToString(proto.toByteArray(), Base64.NO_WRAP);
    }

    public ChannelParcelable toParcelable(String path) {
        return new ChannelParcelable(toTokenString(), nodeId, path);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ChannelToken)) return false;
        ChannelToken other = (ChannelToken) obj;
        return channelId == other.channelId
                && thisNodeWasOpener == other.thisNodeWasOpener
                && appKey.equals(other.appKey)
                && nodeId.equals(other.nodeId);
    }

    @Override
    public int hashCode() {
        int result = ((nodeId.hashCode() + 527) * 31) + appKey.hashCode();
        result = (result * 31) + Long.hashCode(channelId);
        return (result * 31) + (thisNodeWasOpener ? 1 : 0);
    }

    @Override
    public String toString() {
        return "ChannelToken[nodeId='" + nodeId + "', appKey=" + appKey +
                ", channelId=" + channelId + ", thisNodeWasOpener=" + thisNodeWasOpener + "]";
    }

    static class ChannelTokenProto {
        String nodeId;
        String packageName;
        String signatureDigest;
        long channelId;
        boolean thisNodeWasOpener;
        boolean isReliable;

        static ChannelTokenProto parseFrom(byte[] data) throws Exception {
            ChannelTokenProto proto = new ChannelTokenProto();
            java.io.DataInputStream dis = new java.io.DataInputStream(
                    new java.io.ByteArrayInputStream(data));
            proto.nodeId = dis.readUTF();
            proto.packageName = dis.readUTF();
            proto.signatureDigest = dis.readUTF();
            proto.channelId = dis.readLong();
            proto.thisNodeWasOpener = dis.readBoolean();

            if (dis.available() > 0) {
                proto.isReliable = dis.readBoolean();
            } else {
                proto.isReliable = true;
            }

            return proto;
        }

        byte[] toByteArray() {
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.DataOutputStream dos = new java.io.DataOutputStream(baos);
                dos.writeUTF(nodeId);
                dos.writeUTF(packageName);
                dos.writeUTF(signatureDigest);
                dos.writeLong(channelId);
                dos.writeBoolean(thisNodeWasOpener);
                dos.writeBoolean(isReliable);
                return baos.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}