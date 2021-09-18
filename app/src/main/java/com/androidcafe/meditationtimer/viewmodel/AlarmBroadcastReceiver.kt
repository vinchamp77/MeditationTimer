package com.androidcafe.meditationtimer.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmBroadcastReceiver(val viewModel: MainFragmentViewModel) : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        viewModel.notifyAlarmFired()
    }
}