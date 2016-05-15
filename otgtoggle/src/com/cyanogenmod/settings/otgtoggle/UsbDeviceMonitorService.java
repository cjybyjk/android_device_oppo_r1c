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
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;

public class UsbDeviceMonitorService extends Service {
    private static final String TAG = UsbDeviceMonitorService.class.getSimpleName();

    /* broadcast action */
    public static final String ACTION_DEVICES_DISCONNECTED =
            "com.cyanogenmod.settings.otgtoggle.action.DEVICES_DISCONNECTED";

    /* service action */
    public static final String ACTION_SET_DETECTION_MODE =
            "com.cyanogenmod.settings.otgtoggle.action.SET_DETECTION_MODE";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_PERMANENT = "permanent";

    public static final int MODE_AUTO = 0;
    public static final int MODE_HEADSET = 1;
    public static final int MODE_OTG = 2;

    private static final String PREF_DETECTION_MODE = "detection_mode";

    private static final int OTG_NOTIFICATION_ID = 1;
    private static final String OTG_TOGGLE_FILE = "/sys/devices/soc.0/78d9000.usb/OTG_status";

    private final BroadcastReceiver mDeviceStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (AudioManager.ACTION_HEADSET_PLUG.equals(action)) {
                handleHeadsetIntent(intent);
            } else {
                handleUsbDeviceIntent(intent);
            }
        }

        private void handleHeadsetIntent(Intent intent) {
            boolean connected = intent.getIntExtra("state", 0) != 0;
            boolean microphone = intent.getIntExtra("microphone", 0) != 0;

            Log.d(TAG, "Got headset state notification: connected "
                    + connected + ", microphone " + microphone);
            boolean nowConnected = connected && !microphone;
            mStateMachine.sendMessage(nowConnected
                    ? UsbPortStateMachine.HEADSET_CONNECTED
                    : UsbPortStateMachine.HEADSET_DISCONNECTED);
        }

        private void handleUsbDeviceIntent(Intent intent) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                if (!mUsbDeviceConnected) {
                    mUsbDeviceConnected = true;
                    mStateMachine.sendMessage(UsbPortStateMachine.USB_DEVICE_CONNECTED);
                }
            } else {
                boolean anyDeviceConnected = !mUsbManager.getDeviceList().isEmpty();
                if (mUsbDeviceConnected && !anyDeviceConnected) {
                    mUsbDeviceConnected = false;
                    mStateMachine.sendMessage(UsbPortStateMachine.ALL_USB_DEVICES_DISCONNECTED);
                }
            }
        }
    };

    private SharedPreferences mPrefs;
    private boolean mUsbDeviceConnected;
    private UsbPortStateMachine mStateMachine;
    private UsbManager mUsbManager;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        mStateMachine = new UsbPortStateMachine(this);
        mStateMachine.setDbg(true); // XXX
        mStateMachine.start();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mDeviceStateReceiver, filter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mDeviceStateReceiver);
        mStateMachine.stop();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String action = intent != null ? intent.getAction() : null;

        if (ACTION_SET_DETECTION_MODE.equals(action)) {
            int mode = intent.getIntExtra(EXTRA_MODE, -1);
            boolean permanent = intent.getBooleanExtra(EXTRA_PERMANENT, false);
            if (mode >= MODE_AUTO && mode <= MODE_OTG) {
                if (permanent) {
                    mPrefs.edit().putInt(PREF_DETECTION_MODE, mode).apply();
                }
                mStateMachine.sendMessage(UsbPortStateMachine.DETECTION_MODE_CHANGED, mode);
            }
        }

        return START_STICKY;
    }

    int getDetectionMode() {
        return mPrefs.getInt(PREF_DETECTION_MODE, MODE_AUTO);
    }

    boolean setOtgEnabled(boolean enable) {
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

    void fireDevicesDisconnected() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.sendBroadcast(new Intent(ACTION_DEVICES_DISCONNECTED));
    }

    void updateNotification(boolean connected, int mode) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (!connected) {
            nm.cancel(OTG_NOTIFICATION_ID);
            return;
        }

        final Intent clickIntent = new Intent(this, OtgModeChooserActivity.class)
                .putExtra(OtgModeChooserActivity.EXTRA_CURRENT_MODE, getDetectionMode());
        final PendingIntent clickPi = PendingIntent.getActivity(this, 0,
                clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final int titleResId;
        switch (mode) {
            case MODE_HEADSET: titleResId = R.string.connection_notification_title_headset; break;
            case MODE_OTG: titleResId = R.string.connection_notification_title_otg; break;
            default: titleResId = R.string.connection_notification_title_detect; break;
        }

        final Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.ic_headset_notification)
                .setLocalOnly(true)
                .setOngoing(true)
                .setWhen(0)
                .setDefaults(0)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_MIN)
                .setColor(getResources().getColor(
                        com.android.internal.R.color.system_notification_accent_color))
                .setContentTitle(getString(titleResId))
                .setContentText(getString(R.string.connection_notification_text))
                .setContentIntent(clickPi);

        nm.notify(OTG_NOTIFICATION_ID, builder.build());
    }
}
