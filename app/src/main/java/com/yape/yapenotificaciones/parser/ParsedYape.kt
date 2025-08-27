package com.yape.yapenotificaciones.parser

import com.yape.yapenotificaciones.data.Direction
import java.util.Locale
import java.util.regex.Pattern

data class ParsedYape(
    val direction: Direction,
    val amount: Double,
    val currency: String,
    val counterpart: String
)

class YapeNotificationParser {

    private val penRegex: Pattern = Pattern.compile(
        // Ej: "S/ 1.234,56" | "S/ 25.50" | "S/ 3,5"
        "(S\\/)?\\s*([0-9]{1,3}(?:\\.[0-9]{3})*(?:[\\.,][0-9]{1,2})?|[0-9]+(?:[\\.,][0-9]{1,2})?)"
    )

    private val receivedHints = listOf(
        "has recibido", "te envió un pago", "te yapeó", "te lleg", "recibiste", "confirmación de pago"
    )
    private val sentHints = listOf(
        "yapeaste", "enviaste", "pagaste", "realizaste un pago"
    )

    fun containsYapeHints(text: String): Boolean {
        val t = text.lowercase(Locale.getDefault())
        return (receivedHints + sentHints).any { t.contains(it) } || t.contains("yape")
    }

    fun parse(raw: String): ParsedYape? {
        val t = raw.trim()
        if (t.isBlank()) return null

        val dir = detectDirection(t)
        val (currency, amount) = extractAmount(t) ?: return null
        val counterpart = extractCounterpart(t, dir).ifBlank { "Desconocido" }

        return ParsedYape(
            direction = dir,
            amount = amount,
            currency = currency,
            counterpart = counterpart
        )
    }

    private fun detectDirection(text: String): Direction {
        val lower = text.lowercase(Locale.getDefault())
        return when {
            receivedHints.any { lower.contains(it) } -> Direction.RECEIVED
            sentHints.any { lower.contains(it) } -> Direction.SENT
            else -> Direction.RECEIVED // por defecto en Yape suele interesar recibidos
        }
    }

    private fun extractAmount(text: String): Pair<String, Double>? {
        val m = penRegex.matcher(text)
        var foundNumeric: String? = null
        while (m.find()) {
            val maybeCurrency = m.group(1) ?: ""
            val numeric = m.group(2) ?: continue
            // Tomo la primera cifra que tenga sentido (>0)
            val normalized = normalizeNumber(numeric)
            if (normalized > 0) {
                foundNumeric = numeric
                val currency = if (maybeCurrency.isNotEmpty()) "S/" else "S/"
                return currency to normalized
            }
        }
        return null
    }

    private fun normalizeNumber(numText: String): Double {
        // Eliminar separador de miles ".", y convertir coma decimal a punto
        val cleaned = numText.replace(".", "").replace(',', '.')
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun extractCounterpart(text: String, dir: Direction): String {
        // Casos típicos:
        // "Confirmación de Pago — {Nombre}. te envió un pago por S/ 0.1 ..."
        // "... de {Nombre} ..." (cuando recibes)  /  "... a {Nombre} ..." (cuando envías)
        val emDashParts = text.split("—").map { it.trim() }
        if (emDashParts.size >= 2) {
            val left = emDashParts[1] // a la derecha suele venir el nombre
            val name = left.substringBefore(".").trim()
            if (name.isNotBlank() && name.length <= 80) return name
        }

        val lower = text.lowercase(Locale.getDefault())
        val marker = if (dir == Direction.RECEIVED) "de " else "a "
        val idx = lower.indexOf(marker)
        if (idx >= 0) {
            val after = text.substring(idx + marker.length)
            val name = after.substringBefore(".").substringBefore(",").substringBefore(" por ").trim()
            if (name.isNotBlank() && name.length <= 80) return name
        }
        return ""
    }
}
