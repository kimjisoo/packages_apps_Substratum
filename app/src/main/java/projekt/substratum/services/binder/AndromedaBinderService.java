/*
 * Copyright (c) 2016-2017 Projekt Substratum
 * This file is part of Substratum.
 *
 * Substratum is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Substratum is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Substratum.  If not, see <http://www.gnu.org/licenses/>.
 */

package projekt.substratum.services.binder;

import android.app.NotificationManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import projekt.andromeda.IAndromedaInterface;
import projekt.substratum.R;
import projekt.substratum.common.References;
import projekt.substratum.common.platform.AndromedaService;

import static projekt.substratum.common.References.ANDROMEDA_BINDED;
import static projekt.substratum.common.References.ANDROMEDA_PACKAGE;

public class AndromedaBinderService extends Service implements ServiceConnection {

    private static final String TAG = "AndromedaBinderService";
    private static AndromedaBinderService andromedaBinderService;
    private IAndromedaInterface iAndromedaInterface;
    private boolean mBound;

    public static AndromedaBinderService getInstance() {
        return andromedaBinderService;
    }

    public IAndromedaInterface getAndromedaInterface() {
        return iAndromedaInterface;
    }

    public void bindAndromeda() {
        if (References.checkAndromeda(this) && !mBound) {
            Intent intent = new Intent(ANDROMEDA_BINDED);
            intent.setPackage(ANDROMEDA_PACKAGE);
            bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindAndromeda() {
        if (References.checkAndromeda(this) && mBound) {
            unbindService(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        andromedaBinderService = this;

        new Thread(() -> {
            while (!mBound) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (AndromedaService.checkServerActivity()) {
                boolean failed = false;
                while (!failed) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if (!AndromedaService.checkServerActivity()) {
                        sendBadNotification(new NotificationCompat.Builder(
                                getApplicationContext(),
                                References.DEFAULT_NOTIFICATION_CHANNEL_ID));
                        failed = true;
                    }
                }
            } else {
                sendBadNotification(new NotificationCompat.Builder(
                        getApplicationContext(),
                        References.DEFAULT_NOTIFICATION_CHANNEL_ID));
            }
        }).start();
        bindAndromeda();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Substratum has disconnected from Andromeda!");
        unbindAndromeda();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        iAndromedaInterface = IAndromedaInterface.Stub.asInterface(service);
        mBound = true;
        Log.d(TAG, "Substratum has successfully binded with the Andromeda module.");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        iAndromedaInterface = null;
        mBound = false;
        stopSelf();
        Log.d(TAG, "Substratum has successfully unbinded with the Andromeda module.");
    }

    public void sendBadNotification(NotificationCompat.Builder mBuilder) {
        NotificationManager mNotifyMgr =
                (NotificationManager)
                        getApplicationContext().getSystemService(NOTIFICATION_SERVICE);

        boolean isBadNotificationShowing = false;
        StatusBarNotification[] notifications;
        int badNotificationId = 2017;
        if (mNotifyMgr != null) {
            notifications = mNotifyMgr.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == badNotificationId) {
                    isBadNotificationShowing = true;
                }
            }
        }
        if (mNotifyMgr != null && !isBadNotificationShowing) {
            mBuilder.setContentTitle(
                    getApplicationContext().getString(
                            R.string.andromeda_notification_title_negation));
            mBuilder.setContentText(
                    getApplicationContext().getString(
                            R.string.andromeda_notification_text_negation));
            mBuilder.setOngoing(false);
            mBuilder.setSmallIcon(R.drawable.notification_warning_icon);
            mNotifyMgr.notify(badNotificationId, mBuilder.build());
        }
        System.exit(0);
    }
}