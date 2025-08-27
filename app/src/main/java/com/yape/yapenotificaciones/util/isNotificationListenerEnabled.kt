package com.yape.yapenotificaciones.util

import android.provider.Settings
import android.content.Context

fun isNotificationListenerEnabled(context: Context, componentFlatten: String): Boolean {
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    ) ?: return false
    return flat.split(":").any { it.equals(componentFlatten, ignoreCase = true) }
}
