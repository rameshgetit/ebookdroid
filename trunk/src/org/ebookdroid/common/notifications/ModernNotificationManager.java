package org.ebookdroid.common.notifications;

import org.ebookdroid.R;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;

import org.emdev.BaseDroidApp;

@TargetApi(11)
class ModernNotificationManager extends AbstractNotificationManager {

    @Override
    public int notify(final CharSequence title, final CharSequence message, final Intent intent) {
        final Notification.Builder nb = new Notification.Builder(BaseDroidApp.context);

        nb.setSmallIcon(R.drawable.icon);
        nb.setAutoCancel(true);
        nb.setWhen(System.currentTimeMillis());
        nb.setDefaults(Notification.DEFAULT_ALL & (~Notification.DEFAULT_VIBRATE));

        nb.setContentIntent(getIntent(intent));

        nb.setContentTitle(title);
        nb.setTicker(message);
        nb.setContentText(message);

        final Notification notification = nb.build();
        final int id = SEQ.getAndIncrement();
        getManager().notify(id, notification);

        return id;
    }
}
