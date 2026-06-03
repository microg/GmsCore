package org.microg.gms.wearable;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Watch-side media control service.
 *
 * SOLVES:
 * - S-02: Mixed Wearable API clients -> standardized on GoogleApiClient
 *
 * Receives media state from phone via MessageApi, creates a
 * MediaStyle notification on the watch for native media controls.
 */
public class WearMediaControlService extends WearableListenerService {

    private static final String TAG = "WearMediaCtrlSvc";
    private static final String CHANNEL_ID = "microg_media";
    private static final int NOTIF_ID = 9200;

    private NotificationManager notificationManager;
    private String currentTitle = "";
    private String currentArtist = "";
    private String currentAlbum = "";
    private Bitmap currentAlbumArt;
    private boolean isPlaying = false;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        if (event == null) return;
        String path = event.getPath();

        if (WearableDataPaths.MEDIA_METADATA.equals(path)) {
            handleMetadata(event.getData());
        } else if (WearableDataPaths.MEDIA_STATE.equals(path)) {
            handleState(event.getData());
        } else if (WearableDataPaths.MEDIA_DISCONNECT.equals(path)) {
            handleDisconnect();
        }
    }

    private void handleMetadata(byte[] data) {
        Bundle bundle = BundleUtil.fromByteArray(data);
        if (bundle == null) return;
        currentTitle = bundle.getString("title", "");
        currentArtist = bundle.getString("artist", "");
        currentAlbum = bundle.getString("album", "");
        String artBase64 = bundle.getString("album_art", null);
        if (artBase64 != null) {
            try {
                byte[] artBytes = Base64.decode(artBase64, Base64.NO_WRAP);
                currentAlbumArt = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.length);
            } catch (Exception e) { currentAlbumArt = null; }
        } else {
            currentAlbumArt = null;
        }
        updateNotification();
    }

    private void handleState(byte[] data) {
        Bundle bundle = BundleUtil.fromByteArray(data);
        if (bundle == null) return;
        isPlaying = bundle.getBoolean("is_playing", false);
        updateNotification();
    }

    private void handleDisconnect() {
        notificationManager.cancel(NOTIF_ID);
        Log.d(TAG, "Media disconnected, notification removed");
    }

    private void updateNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setContentTitle(currentTitle)
                .setContentText(currentArtist)
                .setSubText(currentAlbum)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(isPlaying)
                .setWhen(System.currentTimeMillis());

        if (currentAlbumArt != null) {
            builder.setLargeIcon(currentAlbumArt);
        }

        // Add media action buttons
        Intent prevIntent = new Intent("microg.media.PREV");
        Intent playPauseIntent = new Intent(isPlaying ? "microg.media.PAUSE" : "microg.media.PLAY");
        Intent nextIntent = new Intent("microg.media.NEXT");

        PendingIntent prevPi = PendingIntent.getBroadcast(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent playPausePi = PendingIntent.getBroadcast(this, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE);
        PendingIntent nextPi = PendingIntent.getBroadcast(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE);

        builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_previous, "Previous", prevPi).build());
        builder.addAction(new Notification.Action.Builder(
                isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                isPlaying ? "Pause" : "Play", playPausePi).build());
        builder.addAction(new Notification.Action.Builder(
                android.R.drawable.ic_media_next, "Next", nextPi).build());

        notificationManager.notify(NOTIF_ID, builder.build());
        Log.d(TAG, "Updated: " + currentTitle + " playing=" + isPlaying);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Media Controls",
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
