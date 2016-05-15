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

import android.os.Message;

import com.android.internal.util.State;
import com.android.internal.util.StateMachine;

public class UsbPortStateMachine extends StateMachine {
    private Disconnected mDisconnected = new Disconnected();
    private DetectWait mDetectWait = new DetectWait();
    private HeadsetMode mHeadsetMode = new HeadsetMode();
    private OtgMode mOtgMode = new OtgMode();

    private UsbDeviceMonitorService mService;

    static final int HEADSET_CONNECTED = 1;
    static final int HEADSET_DISCONNECTED = 2;
    static final int USB_DEVICE_CONNECTED = 3;
    static final int ALL_USB_DEVICES_DISCONNECTED = 4;
    static final int DETECTION_MODE_CHANGED = 5;
    private static final int USB_DETECT_TIMEOUT = 6;

    private static final int USB_DETECT_TIMEOUT_DELAY = 2000;

    public UsbPortStateMachine(UsbDeviceMonitorService service) {
        super("UsbPortStateMachine");
        mService = service;

        addState(mDisconnected);
        addState(mDetectWait);
        addState(mHeadsetMode);
        addState(mOtgMode);
        setInitialState(mDisconnected);
    }

    public void stop() {
        quitNow();
    }

    private class Disconnected extends State {
        @Override
        public void enter() {
            mService.setOtgEnabled(false);
            mService.fireDevicesDisconnected();
            mService.updateNotification(false, -1);
        }

        @Override
        public boolean processMessage(Message msg) {
            if (msg.what == HEADSET_DISCONNECTED) {
                // happens when connecting a headset with microphone
                return HANDLED;
            } else if (msg.what != HEADSET_CONNECTED && msg.what != DETECTION_MODE_CHANGED) {
                return NOT_HANDLED;
            }

            int detectionMode = msg.what == DETECTION_MODE_CHANGED
                    ? msg.arg1 : mService.getDetectionMode();
            switch (detectionMode) {
                case UsbDeviceMonitorService.MODE_AUTO:
                    mService.setOtgEnabled(true);
                    transitionTo(mDetectWait);
                    sendMessageDelayed(USB_DETECT_TIMEOUT, USB_DETECT_TIMEOUT_DELAY);
                    break;
                case UsbDeviceMonitorService.MODE_HEADSET:
                    transitionTo(mHeadsetMode);
                    break;
                case UsbDeviceMonitorService.MODE_OTG:
                    mService.setOtgEnabled(true);
                    transitionTo(mOtgMode);
                    break;
            }

            return HANDLED;
        }
    }

    private class DetectWait extends State {
        @Override
        public void enter() {
            mService.updateNotification(true, -1);
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case HEADSET_DISCONNECTED:
                    // headset disconnecting is normal after enabling OTG -> ignore
                    return HANDLED;
                case DETECTION_MODE_CHANGED:
                    deferMessage(msg);
                    mService.setOtgEnabled(false);
                    transitionTo(mDisconnected);
                    return HANDLED;
                case USB_DEVICE_CONNECTED:
                    removeMessages(USB_DETECT_TIMEOUT);
                    transitionTo(mOtgMode);
                    return HANDLED;
                case USB_DETECT_TIMEOUT:
                    mService.setOtgEnabled(false);
                    transitionTo(mHeadsetMode);
                    return HANDLED;
            }

            return NOT_HANDLED;
        }
    }

    private class HeadsetMode extends State {
        @Override
        public void enter() {
            mService.updateNotification(true, UsbDeviceMonitorService.MODE_HEADSET);
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case HEADSET_CONNECTED:
                case ALL_USB_DEVICES_DISCONNECTED:
                    // normal if we came from OTG -> ignore
                    return HANDLED;
                case HEADSET_DISCONNECTED:
                    transitionTo(mDisconnected);
                    return HANDLED;
                case DETECTION_MODE_CHANGED:
                    deferMessage(msg);
                    transitionTo(mDisconnected);
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }

    private class OtgMode extends State {
        @Override
        public void enter() {
            mService.updateNotification(true, UsbDeviceMonitorService.MODE_OTG);
        }

        @Override
        public boolean processMessage(Message msg) {
            switch (msg.what) {
                case HEADSET_DISCONNECTED:
                case USB_DEVICE_CONNECTED:
                    // normal if we came from headset -> ignore
                    return HANDLED;
                case ALL_USB_DEVICES_DISCONNECTED:
                    transitionTo(mDisconnected);
                    return HANDLED;
                case DETECTION_MODE_CHANGED:
                    deferMessage(msg);
                    transitionTo(mDisconnected);
                    return HANDLED;
            }
            return NOT_HANDLED;
        }
    }
}
