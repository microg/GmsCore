package com.google.android.gms.cast.framework.media;

interface IMediaNotificationService {
    void setNotificationActionsProvider(in com.google.android.gms.cast.framework.media.INotificationActionsProvider provider);
    void updateNotification(in android.app.Notification notification);
}