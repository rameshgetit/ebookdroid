package org.ebookdroid.common.notifications;

import org.ebookdroid.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;

import org.emdev.BaseDroidApp;

@TargetApi(4)
class CompatibilityNotificationManager extends AbstractNotificationManager {

    @Override
    public int notify(final CharSequence title, final CharSequence message) {
        final NotificationCompat.Builder nb = new NotificationCompat.Builder(BaseDroidApp.context);

        nb.setSmallIcon(R.drawable.icon).setContentTitle(title);
        nb.setTicker(message).setContentText(message);
        nb.setAutoCancel(true).setWhen(System.currentTimeMillis()).setDefaults(Notification.DEFAULT_ALL);

        final Notification notification = nb.getNotification();
        final int id = SEQ.getAndIncrement();
        getManager().notify(id, notification);

        return id;
    }
}
