package com.yape.yapenotificaciones.notif

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import com.yape.yapenotificaciones.data.AppDatabase
import com.yape.yapenotificaciones.data.Yapeo
import com.yape.yapenotificaciones.parser.YapeNotificationParser
import com.yape.yapenotificaciones.util.sha256
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class YapeNotificationListener : NotificationListenerService() {

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val parser = YapeNotificationParser()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: return
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()

        val rawText = buildString {
            if (title.isNotBlank()) append(title).append(". ")
            if (text.isNotBlank()) append(text).append(" ")
            if (big.isNotBlank()) append(big)
        }.trim()

        // Filtrado por package o keywords
        val looksLikeYape = pkg.contains("yape", ignoreCase = true) || parser.containsYapeHints(rawText)
        if (!looksLikeYape) return

        val parsed = parser.parse(rawText)
        if (parsed == null) return

        val ts = sbn.postTime
        val unique = sha256("$pkg|$ts|$rawText")

        val entity = Yapeo(
            timestamp = ts,
            packageName = pkg,
            direction = parsed.direction,
            amount = parsed.amount,
            currency = parsed.currency,
            counterpart = parsed.counterpart,
            rawText = rawText,
            uniqueKey = unique
        )

        ioScope.launch {
            AppDatabase.getInstance(applicationContext).yapeoDao().insertIgnore(entity)
        }
    }
}
