package com.yape.yapenotificaciones.data

import android.content.Context
import com.yape.yapenotificaciones.parser.YapeNotificationParser
import com.yape.yapenotificaciones.util.sha256
import kotlinx.coroutines.flow.Flow

class YapeRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).yapeoDao()
    private val parser = YapeNotificationParser()

    fun observeAll(): Flow<List<Yapeo>> = dao.getAll()

    suspend fun clearAll() = dao.clearAll()

    suspend fun insertParsed(packageName: String, timestamp: Long, rawText: String) {
        val parsed = parser.parse(rawText) ?: return
        val unique = sha256("$packageName|$timestamp|$rawText")
        val entity = Yapeo(
            timestamp = timestamp,
            packageName = packageName,
            direction = parsed.direction,
            amount = parsed.amount,
            currency = parsed.currency,
            counterpart = parsed.counterpart,
            rawText = rawText,
            uniqueKey = unique
        )
        dao.insertIgnore(entity)
    }

    suspend fun getBetween(startMillis: Long, endMillis: Long, onlyReceived: Boolean): List<Yapeo> {
        return if (onlyReceived) dao.getReceivedBetween(startMillis, endMillis)
        else dao.getBetween(startMillis, endMillis)
    }
}
