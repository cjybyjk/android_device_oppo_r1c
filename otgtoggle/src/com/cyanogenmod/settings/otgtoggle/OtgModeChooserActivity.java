/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.annotation.Nullable;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OtgModeChooserActivity extends Activity implements
        DialogInterface.OnDismissListener, DialogInterface.OnClickListener, View.OnClickListener {
    public static final String EXTRA_CURRENT_MODE = "current_mode";

    private AlertDialog mDialog;
    private LayoutInflater mLayoutInflater;

    private final BroadcastReceiver mDeviceDisconnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mDialog.dismiss();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLayoutInflater = LayoutInflater.from(this);
        mDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.selection_dialog_title)
                .setView(R.layout.selection_dialog)
                .setOnDismissListener(this)
                .setNegativeButton(android.R.string.cancel, this)
                .show();

        LinearLayout container = (LinearLayout) mDialog.findViewById(R.id.container);
        int currentMode = getIntent().getIntExtra(EXTRA_CURRENT_MODE, -1);

        inflateOption(R.string.auto_detect_title, R.string.auto_detect_summary,
                UsbDeviceMonitorService.MODE_AUTO, currentMode, container);
        inflateOption(R.string.headset_title, R.string.headset_summary,
                UsbDeviceMonitorService.MODE_HEADSET, currentMode, container);
        inflateOption(R.string.otg_title, R.string.otg_summary,
                UsbDeviceMonitorService.MODE_OTG, currentMode, container);

        IntentFilter filter = new IntentFilter(UsbDeviceMonitorService.ACTION_DEVICES_DISCONNECTED);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mDeviceDisconnectionReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mDeviceDisconnectionReceiver);
        super.onDestroy();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        dialog.dismiss();
    }

    @Override
    public void onClick(View v) {
        int mode = (Integer) v.getTag();
        CheckBox defaultCb = (CheckBox) mDialog.findViewById(R.id.as_default);

        startService(new Intent(this, UsbDeviceMonitorService.class)
                .setAction(UsbDeviceMonitorService.ACTION_SET_DETECTION_MODE)
                .putExtra(UsbDeviceMonitorService.EXTRA_MODE, mode)
                .putExtra(UsbDeviceMonitorService.EXTRA_PERMANENT, defaultCb.isChecked()));

        mDialog.dismiss();
    }

    private void inflateOption(final int titleResId, final int summaryResId,
            int mode, int currentMode, LinearLayout container) {
        View v = mLayoutInflater.inflate(R.layout.radio_with_summary, container, false);
        CheckedTextView title = (CheckedTextView) v.findViewById(android.R.id.title);
        TextView summary = (TextView) v.findViewById(android.R.id.summary);

        title.setText(titleResId);
        title.setChecked(mode == currentMode);
        summary.setText(summaryResId);

        v.setTag(mode);
        v.setOnClickListener(this);
        container.addView(v);
    }
}
