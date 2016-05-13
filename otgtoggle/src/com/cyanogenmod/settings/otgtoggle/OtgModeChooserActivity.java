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
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class OtgModeChooserActivity extends Activity implements
        DialogInterface.OnDismissListener, DialogInterface.OnClickListener, View.OnClickListener {
    public static final String EXTRA_ENABLED = "enabled";

    private AlertDialog mDialog;
    private LayoutInflater mLayoutInflater;

    private final BroadcastReceiver mHeadsetStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean connected = intent.getIntExtra("state", 0) != 0;
            boolean microphone = intent.getIntExtra("microphone", 0) != 0;

            if (!connected || microphone) {
                mDialog.dismiss();
            }
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
        boolean otgEnabled = getIntent().getBooleanExtra(EXTRA_ENABLED, false);

        inflateOption(R.string.headset_title, R.string.headset_summary,
                false, !otgEnabled, container);
        inflateOption(R.string.otg_title, R.string.otg_summary,
                true, otgEnabled, container);
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
        Boolean enableOtg = (Boolean) v.getTag();

        startService(new Intent(this, HeadsetMonitorService.class)
                .setAction(HeadsetMonitorService.ACTION_SET_OTG_STATE)
                .putExtra(HeadsetMonitorService.EXTRA_ENABLED, enableOtg));

        CheckBox defaultCb = (CheckBox) mDialog.findViewById(R.id.as_default);
        if (defaultCb.isChecked()) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit()
                    .putBoolean(HeadsetMonitorService.PREF_OTG_AS_DEFAULT, enableOtg)
                    .apply();
        }

        mDialog.dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(AudioManager.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadsetStateReceiver, filter);
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mHeadsetStateReceiver);
        super.onStop();
    }

    private void inflateOption(final int titleResId, final int summaryResId,
            boolean enableOtg, boolean selected, LinearLayout container) {
        View v = mLayoutInflater.inflate(R.layout.radio_with_summary, container, false);
        CheckedTextView title = (CheckedTextView) v.findViewById(android.R.id.title);
        TextView summary = (TextView) v.findViewById(android.R.id.summary);

        title.setText(titleResId);
        title.setChecked(selected);
        summary.setText(summaryResId);

        v.setTag(enableOtg);
        v.setOnClickListener(this);
        container.addView(v);
    }
}
