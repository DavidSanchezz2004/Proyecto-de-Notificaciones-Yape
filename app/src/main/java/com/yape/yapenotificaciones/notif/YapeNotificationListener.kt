package com.yape.yapenotificaciones.notif

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.util.Log
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
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        // Construir texto raw evitando duplicaciones
        val parts = mutableSetOf<String>()

        if (title.isNotBlank()) {
            parts.add(title.trim())
        }
        if (text.isNotBlank() && text.trim() != title.trim()) {
            parts.add(text.trim())
        }
        if (big.isNotBlank() && big.trim() != title.trim() && big.trim() != text.trim()) {
            parts.add(big.trim())
        }

        val rawText = parts.joinToString(". ").let { combined ->
            // Asegurar que termina con punto si no tiene puntuación
            if (combined.isNotBlank() && !combined.endsWith(".") && !combined.endsWith("!") && !combined.endsWith("?")) {
                "$combined."
            } else {
                combined
            }
        }

        // Log para debug - quitar después si quieres
        Log.d("YapeListener", "Package: $pkg, Title: '$title', Text: '$text'")

        // Filtrado específico para Yape
        val packageContainsYape = pkg.contains("yape", ignoreCase = true)
        val isYapeNotification = packageContainsYape ||
                title.equals("Yape", ignoreCase = true) ||
                rawText.contains("confirmación de pago", ignoreCase = true)

        if (!isYapeNotification) {
            return
        }

        // Verificación adicional por contenido
        if (!parser.containsYapeHints(rawText)) {
            return
        }

        // Intentar parsear
        val parsed = parser.parse(rawText)
        if (parsed == null) {
            Log.d("YapeListener", "No se pudo parsear: '$rawText'")
            return
        }

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
            try {
                val result = AppDatabase.getInstance(applicationContext).yapeoDao().insertIgnore(entity)
                Log.d("YapeListener", "Inserted yapeo: $result, Amount: ${parsed.amount}")
            } catch (e: Exception) {
                Log.e("YapeListener", "Error inserting yapeo", e)
            }
        }
    }
}