package com.example.timeapk.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RescheduleBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: "unknown"
        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                RescheduleAllWorker.enqueue(context, action)
            }
        }
    }
}
