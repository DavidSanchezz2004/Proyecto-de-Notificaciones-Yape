package com.yape.yapenotificaciones.excel
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.yape.yapenotificaciones.data.Yapeo
import com.yape.yapenotificaciones.util.formatLocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import java.io.ByteArrayOutputStream
import java.time.*
import java.time.format.DateTimeFormatter
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class ExcelExporter(private val context: Context) {
    private val mimeXlsx = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    private val zone = ZoneId.of("America/Lima")

    /** —— API 29+: guarda directo en Downloads con MediaStore —— */
    suspend fun exportDay(day: LocalDate, items: List<Yapeo>): Uri? = withContext(Dispatchers.IO) {
        val fileName = "Yape_Recibidos_Dia_${day.format(DateTimeFormatter.ISO_DATE)}.xlsx"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportXlsx(fileName) { wb ->
                buildDayWorkbook(wb, items.filter { it.direction.name == "RECEIVED" })
            }
        } else {
            // En API 26–28 devuelve null para que la UI use SAF (CreateDocument)
            null
        }
    }

    /** —— API 29+: guarda directo en Downloads con MediaStore —— */
    suspend fun exportMonth(yearMonth: YearMonth, items: List<Yapeo>): Uri? = withContext(Dispatchers.IO) {
        val fileName = "Yape_Recibidos_Mes_${yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))}.xlsx"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportXlsx(fileName) { wb ->
                buildMonthWorkbook(wb, yearMonth, items.filter { it.direction.name == "RECEIVED" })
            }
        } else {
            null
        }
    }

    /** —— API 26–28: genera bytes para que la Activity los guarde vía SAF —— */
    fun buildDayBytes(day: LocalDate, items: List<Yapeo>): Pair<String, ByteArray> {
        val fileName = "Yape_Recibidos_Dia_${day.format(DateTimeFormatter.ISO_DATE)}.xlsx"
        val bytes = makeBytes { wb -> buildDayWorkbook(wb, items.filter { it.direction.name == "RECEIVED" }) }
        return fileName to bytes
    }

    /** —— API 26–28: genera bytes para que la Activity los guarde vía SAF —— */
    fun buildMonthBytes(ym: YearMonth, items: List<Yapeo>): Pair<String, ByteArray> {
        val fileName = "Yape_Recibidos_Mes_${ym.format(DateTimeFormatter.ofPattern("yyyy-MM"))}.xlsx"
        val bytes = makeBytes { wb -> buildMonthWorkbook(wb, ym, items.filter { it.direction.name == "RECEIVED" }) }
        return fileName to bytes
    }

    // ———————————————— Workbook builders ————————————————
    private fun buildDayWorkbook(wb: XSSFWorkbook, items: List<Yapeo>) {
        val styles = buildStyles(wb)
        val sh = wb.createSheet("Detalle Día")
        createHeader(sh, listOf("FechaHora", "Contraparte", "Monto", "Moneda", "Texto"), styles)

        items.sortedBy { it.timestamp }.forEachIndexed { idx, y ->
            val row = sh.createRow(idx + 1)
            row.createCell(0).apply {
                setCellValue(formatLocalDateTime(y.timestamp, zone))
                cellStyle = styles.dateStyle
            }
            row.createCell(1).setCellValue(y.counterpart)
            row.createCell(2).apply {
                setCellValue(y.amount)
                cellStyle = styles.moneyStyle
            }
            row.createCell(3).setCellValue(y.currency)
            row.createCell(4).setCellValue(y.rawText)
        }
        postFormatSheet(sh)

        val resumen = wb.createSheet("Resumen Día")
        createHeader(resumen, listOf("Métrica", "Valor"), styles)
        val total = items.sumOf { it.amount }
        resumen.createRow(1).apply {
            createCell(0).setCellValue("Cantidad de yapes")
            createCell(1).setCellValue(items.size.toDouble())
        }
        resumen.createRow(2).apply {
            createCell(0).setCellValue("Total (S/)")
            createCell(1).apply {
                setCellValue(total)
                cellStyle = styles.moneyStyle
            }
        }
        postFormatSheet(resumen)
    }

    private fun buildMonthWorkbook(wb: XSSFWorkbook, ym: YearMonth, items: List<Yapeo>) {
        val styles = buildStyles(wb)
        val sh = wb.createSheet("Detalle Mes")
        createHeader(sh, listOf("FechaHora", "Contraparte", "Monto", "Moneda", "Texto"), styles)

        items.sortedBy { it.timestamp }.forEachIndexed { idx, y ->
            val row = sh.createRow(idx + 1)
            row.createCell(0).apply {
                setCellValue(formatLocalDateTime(y.timestamp, zone))
                cellStyle = styles.dateStyle
            }
            row.createCell(1).setCellValue(y.counterpart)
            row.createCell(2).apply {
                setCellValue(y.amount)
                cellStyle = styles.moneyStyle
            }
            row.createCell(3).setCellValue(y.currency)
            row.createCell(4).setCellValue(y.rawText)
        }
        postFormatSheet(sh)

        val resumen = wb.createSheet("Resumen Mes")
        createHeader(resumen, listOf("Día", "Cantidad", "Total"), styles)
        val byDay = items.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        }.toSortedMap()

        var rowIndex = 1
        var grandTotal = 0.0
        var grandCount = 0
        byDay.forEach { (day, list) ->
            val total = list.sumOf { it.amount }
            grandTotal += total
            grandCount += list.size
            resumen.createRow(rowIndex++).apply {
                createCell(0).setCellValue(day.toString())
                createCell(1).setCellValue(list.size.toDouble())
                createCell(2).apply {
                    setCellValue(total)
                    cellStyle = styles.moneyStyle
                }
            }
        }

        resumen.createRow(rowIndex).apply {
            createCell(0).setCellValue("Total del mes")
            createCell(1).setCellValue(grandCount.toDouble())
            createCell(2).apply {
                setCellValue(grandTotal)
                cellStyle = styles.moneyStyle
            }
        }
        postFormatSheet(resumen)
    }

    private data class Styles(
        val header: XSSFCellStyle,
        val dateStyle: XSSFCellStyle,
        val moneyStyle: XSSFCellStyle
    )

    private fun buildStyles(wb: XSSFWorkbook): Styles {
        val bold = wb.createFont().apply { bold = true }

        val header = (wb.createCellStyle() as XSSFCellStyle).apply {
            setFont(bold)
            alignment = HorizontalAlignment.CENTER
        }

        val dateStyle = (wb.createCellStyle() as XSSFCellStyle).apply {
            dataFormat = wb.creationHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss")
        }

        val moneyStyle = (wb.createCellStyle() as XSSFCellStyle).apply {
            dataFormat = wb.creationHelper.createDataFormat().getFormat("S/ #,##0.00")
        }

        return Styles(header, dateStyle, moneyStyle)
    }

    private fun createHeader(sheet: Sheet, titles: List<String>, styles: Styles) {
        val row = sheet.createRow(0)
        titles.forEachIndexed { i, t ->
            row.createCell(i).apply {
                setCellValue(t)
                cellStyle = styles.header
            }
        }
    }

    private fun postFormatSheet(sheet: Sheet) {
        val header = sheet.getRow(0) ?: return
        val lastColIndex = (header.lastCellNum.toInt().coerceAtLeast(1) - 1).coerceAtLeast(0)

        // --- Ancho de columnas sin AWT (aprox. por largo de texto) ---
        // 1 unidad = 1/256 de carácter; máx ancho permitido 255*256.
        for (c in 0..lastColIndex) {
            var maxLen = 0
            for (r in 0..sheet.lastRowNum) {
                val cell = sheet.getRow(r)?.getCell(c) ?: continue
                val len = when (cell.cellType) {
                    CellType.STRING -> cell.stringCellValue.length
                    CellType.NUMERIC -> cell.numericCellValue.toString().length
                    CellType.BOOLEAN -> 5 // "true"/"false"
                    CellType.FORMULA -> (cell.cellFormula?.length ?: 0)
                    else -> 0
                }
                if (len > maxLen) maxLen = len
            }
            // margen de 2 caracteres, límite 255
            val widthChars = (maxLen + 2).coerceAtMost(255)
            sheet.setColumnWidth(c, widthChars * 256)
        }

        // AutoFilter sobre la fila 0 (A1 .. última columna)
        val range = CellRangeAddress(0, sheet.lastRowNum.coerceAtLeast(0), 0, lastColIndex)
        sheet.setAutoFilter(range)
        sheet.createFreezePane(0, 1)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun exportXlsx(
        fileNameBase: String,
        build: (XSSFWorkbook) -> Unit
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val uniqueName = ensureUnique(resolver, fileNameBase)
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, uniqueName)
            put(MediaStore.Downloads.MIME_TYPE, mimeXlsx)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@withContext null
        resolver.openOutputStream(uri)?.use { out ->
            XSSFWorkbook().use { wb ->
                build(wb)
                wb.write(out)
                out.flush()
            }
        }
        uri
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun ensureUnique(resolver: ContentResolver, base: String): String {
        var name = base
        var index = 2
        val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)

        fun exists(n: String): Boolean {
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
                arrayOf(n),
                null
            ).use { c -> return c != null && c.moveToFirst() }
        }

        while (exists(name) && index < 100) {
            val dot = base.lastIndexOf('.')
            name = if (dot > 0) {
                base.substring(0, dot) + "_(${index})" + base.substring(dot)
            } else {
                base + "_(${index})"
            }
            index++
        }

        if (exists(name)) {
            val dot = base.lastIndexOf('.')
            val suffix = "__" + DateTimeFormatter.ofPattern("HHmmss").format(LocalTime.now())
            name = if (dot > 0) base.substring(0, dot) + suffix + base.substring(dot) else base + suffix
        }
        return name
    }

    /** Utilidad para SAF (API 26–28): devuelve los bytes del workbook */
    private fun makeBytes(build: (XSSFWorkbook) -> Unit): ByteArray {
        val bos = ByteArrayOutputStream()
        XSSFWorkbook().use { wb ->
            build(wb)
            wb.write(bos)
        }
        return bos.toByteArray()
    }

    fun tryOpenUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeXlsx)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}