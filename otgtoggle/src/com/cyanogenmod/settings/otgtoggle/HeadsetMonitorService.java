/*
 * Copyright (C) 2015 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.otgtoggle;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class HeadsetMonitorService extends Service {
    private static final String TAG = HeadsetMonitorService.class.getSimpleName();

    private static final int OTG_NOTIFICATION_ID = 1;
    private static final String ACTION_ENABLE_OTG =
            "com.cyanogenmod.settings.otgtoggle.ACTION_ENABLE_OTG";
    private static final String OTG_TOGGLE_FILE = "/sys/devices/soc.0/78d9000.usb/OTG_status";

    private final BroadcastReceiver mHeadsetStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = intent.getIntExtra("state", 0) != 0;
            boolean microphone = intent.getIntExtra("microphone", 0) != 0;

            Log.d(TAG, "Got headset state notification: connected "
                    + connected + ", microphone " + microphone);
            updateOtgNotification(connected && !microphone);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetStateReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mHeadsetStateReceiver);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand - intent " + intent);
        final String action = intent != null ? intent.getAction() : null;

        if (ACTION_ENABLE_OTG.equals(action)) {
            setOtgEnabled(true);
        }

        return START_STICKY;
    }

    private boolean setOtgEnabled(boolean enable) {
        try {
            String value = enable ? "1" : "0";
            FileOutputStream fos = new FileOutputStream(OTG_TOGGLE_FILE);
            fos.write(value.getBytes());
            fos.flush();
            fos.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Could not write to OTG toggle file", e);
            return false;
        }
    }

    private void updateOtgNotification(boolean show) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!show) {
            nm.cancel(OTG_NOTIFICATION_ID);
            return;
        }

        Intent clickIntent = new Intent(this, getClass());
        clickIntent.setAction(ACTION_ENABLE_OTG);
        final PendingIntent pi = PendingIntent.getService(this, 0, clickIntent, 0);

        final Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_headset_notification)
                .setLocalOnly(true)
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setContentTitle(getString(R.string.connection_notification_title))
	        .setContentText(getString(R.string.connection_notification_text));

        builder.setColor(getResources().getColor(
                com.android.internal.R.color.system_notification_accent_color));
        builder.addAction(new Notification.Action.Builder(
                    R.drawable.ic_usb,
                    getString(R.string.action_enable_otg), pi).build());

        // XXX: hide after some time
        nm.notify(OTG_NOTIFICATION_ID, builder.build());
    }
}
