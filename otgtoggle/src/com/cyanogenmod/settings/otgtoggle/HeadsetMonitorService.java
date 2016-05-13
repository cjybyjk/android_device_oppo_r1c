/*
 * Copyright (C) 2016 The CyanogenMod Project
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

public class HeadsetMonitorService extends Service {
    private static final String TAG = HeadsetMonitorService.class.getSimpleName();

    public static final String ACTION_SET_OTG_STATE =
            "com.cyanogenmod.settings.otgtoggle.ACTION_SET_OTG_STATE";
    public static final String EXTRA_ENABLED = "enabled";

    private static final int OTG_NOTIFICATION_ID = 1;
    private static final String OTG_TOGGLE_FILE = "/sys/devices/soc.0/78d9000.usb/OTG_status";
    private static final long LOWER_PRIORITY_AFTER_CONNECT_DELAY = 30000;

    private static final int MSG_LOWER_PRIORITY = 1;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_LOWER_PRIORITY:
                    mNotificationPriority = Notification.PRIORITY_MIN;
                    updateNotification();
                    break;
            }
        }
    };

    private final BroadcastReceiver mHeadsetStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = intent.getIntExtra("state", 0) != 0;
            boolean microphone = intent.getIntExtra("microphone", 0) != 0;

            Log.d(TAG, "Got headset state notification: connected "
                    + connected + ", microphone " + microphone);

            boolean deviceNowConnected = connected && !microphone;
            if (deviceNowConnected != mDeviceConnected) {
                mDeviceConnected = deviceNowConnected;
                mNotificationPriority = Notification.PRIORITY_DEFAULT;
                updateNotification();
                if (deviceNowConnected) {
                    mHandler.removeMessages(MSG_LOWER_PRIORITY);
                    mHandler.sendEmptyMessageDelayed(MSG_LOWER_PRIORITY,
                            LOWER_PRIORITY_AFTER_CONNECT_DELAY);
                }
            }
        }
    };

    private int mNotificationPriority;
    private boolean mDeviceConnected;

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
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand - intent " + intent);
        final String action = intent != null ? intent.getAction() : null;

        if (ACTION_SET_OTG_STATE.equals(action)) {
            setOtgEnabled(intent.getBooleanExtra(EXTRA_ENABLED, false));
            mHandler.sendEmptyMessage(MSG_LOWER_PRIORITY);
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

    private boolean isOtgEnabled() {
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(OTG_TOGGLE_FILE), 512);
            String line = reader.readLine();
            return "1".equals(line);
        } catch (IOException e) {
            Log.e(TAG, "Could not read from OTG toggle file", e);
            return false;
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                // ignored, not much we can do anyway
            }
        }
    }

    private void updateNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!mDeviceConnected) {
            nm.cancel(OTG_NOTIFICATION_ID);
            return;
        }

        final boolean otgEnabled = isOtgEnabled();

        final Intent clickIntent = new Intent(this, OtgModeChooserActivity.class)
                .putExtra(EXTRA_ENABLED, otgEnabled);
        final PendingIntent clickPi = PendingIntent.getActivity(this, 0,
                clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final Intent modeChangeIntent = new Intent(this, getClass())
                .setAction(ACTION_SET_OTG_STATE)
                .putExtra(EXTRA_ENABLED, !otgEnabled);
        final PendingIntent modeChangePi = PendingIntent.getService(this, 0,
                modeChangeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        final CharSequence title = getString(otgEnabled
                ? R.string.connection_notification_title_otg
                : R.string.connection_notification_title_headset);

        final Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_headset_notification)
                .setLocalOnly(true)
                .setOngoing(true)
                .setWhen(0)
                .setDefaults(0)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(mNotificationPriority)
                .setColor(getResources().getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(title)
	        .setContentText(getString(R.string.connection_notification_text))
                .setContentIntent(clickPi);

        final int actionDrawable = otgEnabled ? R.drawable.ic_headset : R.drawable.ic_usb;
        final int bigText = otgEnabled
                ? R.string.connection_notification_big_text_otg
                : R.string.connection_notification_big_text_headset;
        final int actionText = otgEnabled
                 ? R.string.action_enable_headset : R.string.action_enable_otg;

        builder.setStyle(new Notification.BigTextStyle()
                .setBigContentTitle(getString(R.string.connection_notification_big_title))
                .bigText(getString(bigText)));
        builder.addAction(new Notification.Action.Builder(actionDrawable,
                getString(actionText), modeChangePi).build());

        nm.notify(OTG_NOTIFICATION_ID, builder.build());
    }
}
