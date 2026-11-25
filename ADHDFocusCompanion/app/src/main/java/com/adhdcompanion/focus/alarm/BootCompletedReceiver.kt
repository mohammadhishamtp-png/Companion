package com.adhdcompanion.focus.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adhdcompanion.focus.data.prefs.SecurePrefsManager

/**
 * AlarmManager alarms are cleared on reboot; this restores the daily morning
 * briefing alarm (if the user has one configured) after the device restarts.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = SecurePrefsManager(context)
            if (!prefs.morningBriefingTime.isNullOrBlank()) {
                AlarmScheduler(context).scheduleNextMorningBriefing()
            }
        }
    }
}
