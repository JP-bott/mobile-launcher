package com.liquidlauncher

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.compose.runtime.mutableStateMapOf

/**
 * Optional notification counts. Users must explicitly enable this service in
 * Android settings. No notification text or content is persisted or transmitted.
 */
class LauncherNotificationService : NotificationListenerService() {
    companion object {
        val badgeCounts = mutableStateMapOf<String, Int>()
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        refresh()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        refresh()
    }

    override fun onListenerDisconnected() {
        badgeCounts.clear()
        super.onListenerDisconnected()
    }

    private fun refresh() {
        val active = runCatching { activeNotifications?.toList().orEmpty() }.getOrDefault(emptyList())
        val updated = active.filter {
            (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 &&
                (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
        }.groupingBy { it.packageName }.eachCount()
        badgeCounts.keys.toList().filterNot { it in updated }.forEach { badgeCounts.remove(it) }
        updated.forEach { (name, count) -> badgeCounts[name] = count }
    }
}
