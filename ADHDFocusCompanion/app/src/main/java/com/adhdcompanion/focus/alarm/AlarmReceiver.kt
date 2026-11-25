package com.adhdcompanion.focus.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.adhdcompanion.focus.notification.NotificationHelper

/**
 * Fires when the scheduled morning/transition alarm goes off. It posts a
 * notification immediately (reliable, no network needed) that opens straight
 * to the Now & Briefing screen, which then speaks the full AI-generated
 * briefing once opened/foregrounded.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Time to check in"
        NotificationHelper.showBriefingNotification(
            context = context,
            title = "Focus Companion",
            body = label
        )
        // Re-schedule tomorrow's recurring morning alarm if this was the daily one.
        if (intent.getBooleanExtra(EXTRA_IS_DAILY, false)) {
            AlarmScheduler(context).scheduleNextMorningBriefing()
        }
    }

    companion object {
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_IS_DAILY = "extra_is_daily"
    }
}
