package com.example.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.InspectionItem
import com.example.model.InspectionReport
import com.example.model.InspectionStatus
import com.example.model.RiskAssessmentEntry
import com.example.model.RiskLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfReportGenerator(private val context: Context) {

    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in PostScript points (72 dpi)
        private const val PAGE_HEIGHT = 842 // A4 height in PostScript points (72 dpi)
        private const val MARGIN_H = 24f
        private const val MARGIN_TOP = 24f
        private const val MARGIN_BOTTOM = 26f
        private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2) // 547f

        // Checklist column offsets relative to MARGIN_H
        private const val COL_Q_WIDTH = 342f
        private const val COL_STATUS_X_OFFSET = 344f
        private const val COL_STATUS_WIDTH = 48f
        private const val COL_NOTES_X_OFFSET = 396f
        private const val COL_NOTES_WIDTH = CONTENT_WIDTH - COL_NOTES_X_OFFSET - 4f // ~147f

        // Risk table column offsets relative to MARGIN_H
        private const val RISK_COL_EQUIP_WIDTH = 114f
        private const val RISK_COL_DEFECT_X_OFFSET = 120f
        private const val RISK_COL_DEFECT_WIDTH = 266f
        private const val RISK_COL_LEVEL_X_OFFSET = 392f
        private const val RISK_COL_ACTION_X_OFFSET = 450f
        private const val RISK_COL_ACTION_WIDTH = CONTENT_WIDTH - RISK_COL_ACTION_X_OFFSET - 4f

        private val COLOR_NAVY = Color.rgb(17, 43, 76)
        private val COLOR_NAVY_SUBTLE = Color.rgb(230, 236, 244)
        private val COLOR_CRIMSON_HEADER = Color.rgb(153, 27, 27)
        private val COLOR_TEXT_PRIMARY = Color.rgb(15, 23, 42)
        private val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139)
        private val COLOR_BORDER = Color.rgb(203, 213, 225)
        private val COLOR_ZEBRA = Color.rgb(248, 250, 252)

        private val COLOR_PASS_BG = Color.rgb(220, 252, 231)
        private val COLOR_PASS_FG = Color.rgb(21, 128, 61)
        private val COLOR_FAIL_BG = Color.rgb(254, 226, 226)
        private val COLOR_FAIL_FG = Color.rgb(185, 28, 28)
        private val COLOR_NA_BG = Color.rgb(226, 232, 240)
        private val COLOR_NA_FG = Color.rgb(71, 85, 105)
        private val COLOR_PENDING_BG = Color.rgb(254, 243, 199)
        private val COLOR_PENDING_FG = Color.rgb(180, 83, 9)

        private val COLOR_RISK_HIGH_BG = Color.rgb(254, 226, 226)
        private val COLOR_RISK_HIGH_FG = Color.rgb(185, 28, 28)
        private val COLOR_RISK_MED_BG = Color.rgb(254, 243, 199)
        private val COLOR_RISK_MED_FG = Color.rgb(180, 83, 9)
        private val COLOR_RISK_LOW_BG = Color.rgb(220, 252, 231)
        private val COLOR_RISK_LOW_FG = Color.rgb(21, 128, 61)
    }

    internal fun calculatePageCount(report: InspectionReport): Int {
        return renderReport(report = report, document = null, totalPages = 0)
    }

    fun generatePdf(report: InspectionReport): File {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val sanitizedSite = report.metadata.siteName
            .ifBlank { "Playground" }
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .take(28)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outputFile = File(exportDir, "PlaySafe_${sanitizedSite}_$timestamp.pdf")

        // Pass 1: compute exact page count for "Page X of Y"
        val totalPages = calculatePageCount(report)

        // Pass 2: render final A4 PDF
        val pdfDocument = PdfDocument()
        try {
            renderReport(report = report, document = pdfDocument, totalPages = totalPages)
            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            pdfDocument.close()
        }
        return outputFile
    }

    private fun renderReport(
        report: InspectionReport,
        document: PdfDocument?,
        totalPages: Int,
    ): Int {
        val state = RenderState(document = document, totalPages = totalPages, report = report)
        state.startFirstPage()

        // 1. Compact Header & Inspection Metadata
        drawDocumentHeader(state, report)
        drawMetadataBox(state, report)

        // 2. Full Checklist Section by Section
        val groupedItems = report.items.groupBy { it.sectionNumber to it.categoryTitle }
        groupedItems.forEach { (sectionKey, sectionItems) ->
            val (secNum, secTitle) = sectionKey
            // Reserve room for category header bar + at least one row
            state.ensureSpace(34f)
            drawCategoryHeaderWithColumns(state, "$secNum. ${secTitle.uppercase(Locale.getDefault())}")

            sectionItems.forEachIndexed { index, item ->
                drawChecklistRow(state, item, isAlternate = index % 2 == 1)
            }
            state.cursorY += 4f
        }

        // 3. Automated Risk Assessment & Action Plan Table
        state.ensureSpace(58f)
        state.cursorY += 3f
        drawSectionTitleBanner(
            state,
            "COMPILED RISK ASSESSMENT & ACTION PLAN",
            if (report.failCount > 0) COLOR_CRIMSON_HEADER else COLOR_PASS_FG,
        )

        val riskEntries = report.riskAssessmentEntries
        if (riskEntries.isEmpty()) {
            drawNoDefectsBox(state)
        } else {
            drawRiskTableHeader(state)
            riskEntries.forEachIndexed { idx, entry ->
                drawRiskTableRow(state, entry, isAlternate = idx % 2 == 1)
            }
        }

        // 4. Maintenance Log & Inspector Sign-Off
        state.ensureSpace(72f)
        state.cursorY += 6f
        drawSectionTitleBanner(state, "MAINTENANCE LOG & INSPECTOR SIGN-OFF", COLOR_NAVY)
        drawMaintenanceAndSignOff(state, report)

        state.finishCurrentPage()
        return state.pageNumber
    }

    private fun drawDocumentHeader(state: RenderState, report: InspectionReport) {
        val canvas = state.canvas
        val headerHeight = 36f
        val bgRect = RectF(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + headerHeight)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_NAVY }
        canvas?.drawRoundRect(bgRect, 3f, 3f, fillPaint)

        val orgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 230, 138)
            textSize = 7.4f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val refPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 7.2f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        canvas?.drawText(
            report.metadata.organizationName.uppercase(Locale.getDefault()),
            MARGIN_H + 10f,
            state.cursorY + 13f,
            orgPaint,
        )
        canvas?.drawText(
            report.metadata.checklistTitle,
            MARGIN_H + 10f,
            state.cursorY + 28f,
            titlePaint,
        )

        val refText = "REF: ${report.metadata.formRef}"
        val refWidth = refPaint.measureText(refText)
        canvas?.drawText(
            refText,
            PAGE_WIDTH - MARGIN_H - refWidth - 10f,
            state.cursorY + 13f,
            refPaint,
        )

        state.cursorY += headerHeight + 5f
    }

    private fun drawMetadataBox(state: RenderState, report: InspectionReport) {
        val boxTop = state.cursorY
        val boxHeight = 56f
        val canvas = state.canvas

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ZEBRA
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val rect = RectF(MARGIN_H, boxTop, PAGE_WIDTH - MARGIN_H, boxTop + boxHeight)
        canvas?.drawRoundRect(rect, 3f, 3f, boxPaint)
        canvas?.drawRoundRect(rect, 3f, 3f, strokePaint)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 7.6f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.2f
        }

        val col1X = MARGIN_H + 8f
        val col2X = MARGIN_H + (CONTENT_WIDTH * 0.54f)

        canvas?.drawText("Play Area / Site:", col1X, boxTop + 13f, labelPaint)
        canvas?.drawText(
            report.metadata.siteName.ifBlank { "Not Specified" }.take(40),
            col1X + 74f,
            boxTop + 13f,
            valPaint,
        )

        canvas?.drawText("Inspector:", col1X, boxTop + 26f, labelPaint)
        canvas?.drawText(
            report.metadata.inspectorName.ifBlank { "Not Specified" }.take(40),
            col1X + 74f,
            boxTop + 26f,
            valPaint,
        )

        canvas?.drawText("Date & Time:", col2X, boxTop + 13f, labelPaint)
        canvas?.drawText(
            "${report.metadata.inspectionDate}  ${report.metadata.inspectionTime}",
            col2X + 58f,
            boxTop + 13f,
            valPaint,
        )

        canvas?.drawText("Weather:", col2X, boxTop + 26f, labelPaint)
        canvas?.drawText(
            report.metadata.weatherConditions.ifBlank { "Not Specified" }.take(28),
            col2X + 58f,
            boxTop + 26f,
            valPaint,
        )

        canvas?.drawLine(
            MARGIN_H + 6f,
            boxTop + 33f,
            PAGE_WIDTH - MARGIN_H - 6f,
            boxTop + 33f,
            strokePaint,
        )

        // Compact summary pills row
        val pillY = boxTop + 37.5f
        var pillX = MARGIN_H + 8f
        pillX += drawPill(canvas, pillX, pillY, "CHECKS: ${report.items.size}", COLOR_NAVY_SUBTLE, COLOR_NAVY) + 6f
        pillX += drawPill(canvas, pillX, pillY, "YES: ${report.passCount}", COLOR_PASS_BG, COLOR_PASS_FG) + 6f
        pillX += drawPill(canvas, pillX, pillY, "NO: ${report.failCount}", COLOR_FAIL_BG, COLOR_FAIL_FG) + 6f
        pillX += drawPill(canvas, pillX, pillY, "N/A: ${report.naCount}", COLOR_NA_BG, COLOR_NA_FG) + 6f

        val riskLabel = report.highestRiskLevel?.let { level ->
            val peakScore = report.highestRiskScore
            if (peakScore != null) {
                "PEAK RISK: $peakScore/25 (${level.label.uppercase(Locale.US)})"
            } else {
                "RISK: ${level.label.uppercase(Locale.US)}"
            }
        } ?: "RISK: NONE"
        val (riskBg, riskFg) = when (report.highestRiskLevel) {
            RiskLevel.HIGH -> COLOR_RISK_HIGH_BG to COLOR_RISK_HIGH_FG
            RiskLevel.MEDIUM -> COLOR_RISK_MED_BG to COLOR_RISK_MED_FG
            RiskLevel.LOW -> COLOR_RISK_LOW_BG to COLOR_RISK_LOW_FG
            null -> COLOR_PASS_BG to COLOR_PASS_FG
        }
        drawPill(canvas, pillX, pillY, riskLabel, riskBg, riskFg)

        state.cursorY += boxHeight + 6f
    }

    private fun drawPill(
        canvas: Canvas?,
        x: Float,
        y: Float,
        text: String,
        bgColor: Int,
        fgColor: Int,
        height: Float = 13.5f,
        textSize: Float = 6.9f,
    ): Float {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            this.textSize = textSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(text)
        val width = textWidth + 10f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas?.drawRoundRect(RectF(x, y, x + width, y + height), height / 2f, height / 2f, bgPaint)
        val textOffsetY = (height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas?.drawText(text, x + 5f, y + textOffsetY, textPaint)
        return width
    }

    private fun drawSectionTitleBanner(state: RenderState, title: String, bgColor: Int) {
        state.ensureSpace(22f)
        val canvas = state.canvas
        val h = 16f
        val bannerPaint = Paint().apply { color = bgColor }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + h, bannerPaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText(title, MARGIN_H + 6f, state.cursorY + 11f, textPaint)
        state.cursorY += h
    }

    private fun drawCategoryHeaderWithColumns(state: RenderState, title: String) {
        val canvas = state.canvas
        val h = 15.5f
        val bgPaint = Paint().apply { color = COLOR_NAVY }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + h, bgPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.6f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val colPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 6.9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        canvas?.drawText(title.take(58), MARGIN_H + 5f, state.cursorY + 10.6f, titlePaint)
        canvas?.drawText("STATUS", MARGIN_H + COL_STATUS_X_OFFSET + 6f, state.cursorY + 10.4f, colPaint)
        canvas?.drawText("NOTES / DEFECTS", MARGIN_H + COL_NOTES_X_OFFSET + 4f, state.cursorY + 10.4f, colPaint)
        state.cursorY += h
    }

    private fun drawChecklistRow(
        state: RenderState,
        item: InspectionItem,
        isAlternate: Boolean,
    ) {
        val qPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.4f
        }
        val nPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (item.status == InspectionStatus.FAIL) COLOR_FAIL_FG else COLOR_TEXT_MUTED
            textSize = 7.0f
        }

        val noteText = buildString {
            if (item.status == InspectionStatus.FAIL) {
                append(item.defectDetails.trim().ifBlank { "Defect recorded" })
                val score = item.computedRiskScore
                val psLabel = if (item.probability != null && item.severity != null) {
                    "P${item.probability}×S${item.severity}=${score}/25"
                } else {
                    item.effectiveRiskLevel?.label ?: "Med"
                }
                val actionStr = item.actionRequired?.label ?: "Monitor"
                append(" [$psLabel • $actionStr]")
            } else if (item.inlineNote.isNotBlank()) {
                append(item.inlineNote.trim())
            } else {
                append("—")
            }
        }

        val qLayout = buildLayout("${item.referenceCode} ${item.question}", qPaint, COL_Q_WIDTH.toInt() - 8)
        val nLayout = buildLayout(noteText, nPaint, COL_NOTES_WIDTH.toInt() - 4)
        val rowHeight = maxOf(qLayout.height.toFloat(), nLayout.height.toFloat(), 11f) + 5.5f

        state.ensureSpace(rowHeight + 2f) {
            drawCategoryHeaderWithColumns(
                state,
                "${item.sectionNumber}. ${item.categoryTitle.uppercase(Locale.getDefault())} (CONT.)",
            )
        }

        val canvas = state.canvas
        val rowTop = state.cursorY
        val rowBottom = rowTop + rowHeight

        val bgColor = when {
            item.status == InspectionStatus.FAIL -> Color.rgb(255, 245, 245)
            isAlternate -> COLOR_ZEBRA
            else -> Color.WHITE
        }
        val bgPaint = Paint().apply { color = bgColor }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, bgPaint)
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, borderPaint)

        // Subtle column dividers
        canvas?.drawLine(
            MARGIN_H + COL_STATUS_X_OFFSET,
            rowTop,
            MARGIN_H + COL_STATUS_X_OFFSET,
            rowBottom,
            borderPaint,
        )
        canvas?.drawLine(
            MARGIN_H + COL_NOTES_X_OFFSET,
            rowTop,
            MARGIN_H + COL_NOTES_X_OFFSET,
            rowBottom,
            borderPaint,
        )

        // Vertically centered question & notes layouts
        val qY = rowTop + ((rowHeight - qLayout.height) / 2f)
        drawStaticLayout(canvas, qLayout, MARGIN_H + 5f, qY)

        // Compact Status Pill centered in Status column
        val (badgeBg, badgeFg) = when (item.status) {
            InspectionStatus.PASS -> COLOR_PASS_BG to COLOR_PASS_FG
            InspectionStatus.FAIL -> COLOR_FAIL_BG to COLOR_FAIL_FG
            InspectionStatus.NA -> COLOR_NA_BG to COLOR_NA_FG
            InspectionStatus.UNANSWERED -> COLOR_PENDING_BG to COLOR_PENDING_FG
        }
        val statusLabel = when (item.status) {
            InspectionStatus.PASS -> "YES"
            InspectionStatus.FAIL -> "NO"
            InspectionStatus.NA -> "N/A"
            InspectionStatus.UNANSWERED -> "—"
        }
        val pillH = 11.5f
        val pillY = rowTop + ((rowHeight - pillH) / 2f)
        drawPill(
            canvas = canvas,
            x = MARGIN_H + COL_STATUS_X_OFFSET + 8f,
            y = pillY,
            text = statusLabel,
            bgColor = badgeBg,
            fgColor = badgeFg,
            height = pillH,
            textSize = 6.5f,
        )

        val nY = rowTop + ((rowHeight - nLayout.height) / 2f)
        drawStaticLayout(canvas, nLayout, MARGIN_H + COL_NOTES_X_OFFSET + 4f, nY)

        state.cursorY = rowBottom
    }

    private fun drawNoDefectsBox(state: RenderState) {
        state.ensureSpace(26f)
        val canvas = state.canvas
        val h = 22f
        val rect = RectF(MARGIN_H, state.cursorY + 2f, PAGE_WIDTH - MARGIN_H, state.cursorY + 2f + h)
        val bgPaint = Paint().apply { color = COLOR_PASS_BG }
        val strokePaint = Paint().apply {
            color = COLOR_PASS_FG
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas?.drawRoundRect(rect, 3f, 3f, bgPaint)
        canvas?.drawRoundRect(rect, 3f, 3f, strokePaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PASS_FG
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText(
            "Zero failed checks recorded. All inspected play equipment and surfacing met safety standards.",
            MARGIN_H + 8f,
            state.cursorY + 15.5f,
            textPaint,
        )
        state.cursorY += h + 4f
    }

    private fun drawRiskTableHeader(state: RenderState) {
        val canvas = state.canvas
        val h = 14f
        val bgPaint = Paint().apply { color = Color.rgb(127, 29, 29) }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + h, bgPaint)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 7.0f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText("Ref & Equipment", MARGIN_H + 5f, state.cursorY + 9.8f, headerPaint)
        canvas?.drawText("Failed Check & Defect Details", MARGIN_H + RISK_COL_DEFECT_X_OFFSET, state.cursorY + 9.8f, headerPaint)
        canvas?.drawText("P×S / 25", MARGIN_H + RISK_COL_LEVEL_X_OFFSET, state.cursorY + 9.8f, headerPaint)
        canvas?.drawText("Action Required", MARGIN_H + RISK_COL_ACTION_X_OFFSET, state.cursorY + 9.8f, headerPaint)
        state.cursorY += h
    }

    private fun drawRiskTableRow(
        state: RenderState,
        entry: RiskAssessmentEntry,
        isAlternate: Boolean,
    ) {
        val equipPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 7.3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val defectPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.3f
        }
        val actionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val equipLayout = buildLayout("${entry.referenceCode} ${entry.categoryTitle}", equipPaint, RISK_COL_EQUIP_WIDTH.toInt())
        val defectLayout = buildLayout(
            "${entry.question}\nDefect: ${entry.defectDetails}",
            defectPaint,
            RISK_COL_DEFECT_WIDTH.toInt(),
        )
        val actionLayout = buildLayout(entry.actionRequired.label, actionPaint, RISK_COL_ACTION_WIDTH.toInt())
        val rowHeight = maxOf(equipLayout.height, defectLayout.height, actionLayout.height, 20) + 7f

        state.ensureSpace(rowHeight + 2f) {
            drawSectionTitleBanner(state, "COMPILED RISK ASSESSMENT & ACTION PLAN (CONT.)", COLOR_CRIMSON_HEADER)
            drawRiskTableHeader(state)
        }

        val canvas = state.canvas
        val rowTop = state.cursorY
        val rowBottom = rowTop + rowHeight

        val bgPaint = Paint().apply {
            color = if (isAlternate) Color.rgb(255, 245, 245) else Color.WHITE
        }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, bgPaint)
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, borderPaint)

        drawStaticLayout(canvas, equipLayout, MARGIN_H + 5f, rowTop + 3.5f)
        drawStaticLayout(canvas, defectLayout, MARGIN_H + RISK_COL_DEFECT_X_OFFSET, rowTop + 3.5f)

        val (riskBg, riskFg) = when (entry.riskLevel) {
            RiskLevel.HIGH -> COLOR_RISK_HIGH_BG to COLOR_RISK_HIGH_FG
            RiskLevel.MEDIUM -> COLOR_RISK_MED_BG to COLOR_RISK_MED_FG
            RiskLevel.LOW -> COLOR_RISK_LOW_BG to COLOR_RISK_LOW_FG
        }
        drawPill(
            canvas = canvas,
            x = MARGIN_H + RISK_COL_LEVEL_X_OFFSET - 4f,
            y = rowTop + 3.5f,
            text = "${entry.probability}×${entry.severity}=${entry.riskScore}/25",
            bgColor = riskBg,
            fgColor = riskFg,
            height = 12f,
            textSize = 6.4f,
        )
        drawStaticLayout(canvas, actionLayout, MARGIN_H + RISK_COL_ACTION_X_OFFSET, rowTop + 3.5f)

        state.cursorY = rowBottom
    }

    private fun drawMaintenanceAndSignOff(state: RenderState, report: InspectionReport) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 7.5f
        }
        val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        if (report.maintenanceLogs.isNotEmpty()) {
            report.maintenanceLogs.forEach { log ->
                val line = "• ${log.equipmentItem.ifBlank { "Equipment" }}: ${log.repairCarriedOut.ifBlank { "Maintenance recorded" }}" +
                    (if (log.partsUsed.isNotBlank()) " | Parts: ${log.partsUsed}" else "") +
                    (if (log.completedByDate.isNotBlank()) " | Completed: ${log.completedByDate}" else "")
                val layout = buildLayout(line, textPaint, CONTENT_WIDTH.toInt() - 12)
                state.ensureSpace(layout.height + 5f)
                drawStaticLayout(state.canvas, layout, MARGIN_H + 6f, state.cursorY + 2f)
                state.cursorY += layout.height + 4f
            }
        }

        val commentsText = report.metadata.overallComments.ifBlank { "None recorded." }
        val commentsLayout = buildLayout("Overall Comments: $commentsText", textPaint, CONTENT_WIDTH.toInt() - 14)
        val signBoxHeight = commentsLayout.height + 34f

        state.ensureSpace(signBoxHeight + 6f)
        val canvas = state.canvas
        val boxTop = state.cursorY + 3f
        val rect = RectF(MARGIN_H, boxTop, PAGE_WIDTH - MARGIN_H, boxTop + signBoxHeight)
        val bgPaint = Paint().apply { color = COLOR_ZEBRA }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas?.drawRoundRect(rect, 3f, 3f, bgPaint)
        canvas?.drawRoundRect(rect, 3f, 3f, borderPaint)

        drawStaticLayout(canvas, commentsLayout, MARGIN_H + 7f, boxTop + 6f)

        val footerLineY = boxTop + commentsLayout.height + 16f
        canvas?.drawText(
            "Overall Action Required: ${report.metadata.overallActionRequired.label}",
            MARGIN_H + 7f,
            footerLineY,
            boldPaint,
        )
        canvas?.drawText(
            "Inspector Sign-Off: ${report.metadata.inspectorSignature.ifBlank { report.metadata.inspectorName.ifBlank { "Pending Signature" } }}",
            MARGIN_H + 260f,
            footerLineY,
            boldPaint,
        )
        canvas?.drawText(
            "Return Form To: Sport & Leisure Admin, Orkney Islands Council",
            MARGIN_H + 7f,
            footerLineY + 11f,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_MUTED
                textSize = 7.0f
            },
        )
        state.cursorY = boxTop + signBoxHeight + 4f
    }

    private fun buildLayout(text: String, paint: TextPaint, widthPx: Int): StaticLayout {
        val safeWidth = widthPx.coerceAtLeast(36)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0.8f, 0.94f)
            .setIncludePad(false)
            .build()
    }

    private fun drawStaticLayout(canvas: Canvas?, layout: StaticLayout, x: Float, y: Float) {
        if (canvas == null) return
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private inner class RenderState(
        private val document: PdfDocument?,
        private val totalPages: Int,
        private val report: InspectionReport,
    ) {
        var pageNumber: Int = 0
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var cursorY: Float = MARGIN_TOP

        fun startFirstPage() {
            startNewPage()
        }

        fun ensureSpace(neededHeight: Float, onNewPageHeader: (() -> Unit)? = null) {
            if (cursorY + neededHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                finishCurrentPage()
                startNewPage()
                onNewPageHeader?.invoke()
            }
        }

        private fun startNewPage() {
            pageNumber += 1
            if (document != null) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                currentPage = document.startPage(pageInfo)
                canvas = currentPage?.canvas
            }
            cursorY = MARGIN_TOP
            if (pageNumber > 1) {
                drawRunningHeader()
            }
        }

        private fun drawRunningHeader() {
            val c = canvas
            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_MUTED
                textSize = 7.0f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = COLOR_BORDER
                strokeWidth = 0.5f
            }
            c?.drawText(
                "${report.metadata.siteName.ifBlank { "PLAY AREA" }.uppercase(Locale.getDefault())} — SAFETY INSPECTION",
                MARGIN_H,
                16f,
                paint,
            )
            val dateStr = "${report.metadata.inspectionDate}  ${report.metadata.inspectionTime}"
            val dateWidth = paint.measureText(dateStr)
            c?.drawText(
                dateStr,
                PAGE_WIDTH - MARGIN_H - dateWidth,
                16f,
                paint,
            )
            c?.drawLine(MARGIN_H, 19f, PAGE_WIDTH - MARGIN_H, 19f, linePaint)
            cursorY = 23f
        }

        fun finishCurrentPage() {
            val c = canvas
            if (c != null) {
                val linePaint = Paint().apply {
                    color = COLOR_BORDER
                    strokeWidth = 0.5f
                }
                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_TEXT_MUTED
                    textSize = 7.0f
                }
                val footerY = PAGE_HEIGHT - 13f
                c.drawLine(MARGIN_H, footerY - 8f, PAGE_WIDTH - MARGIN_H, footerY - 8f, linePaint)
                c.drawText(
                    "Form Ref: ${report.metadata.formRef}  |  PlaySafe Audit Offline Report",
                    MARGIN_H,
                    footerY,
                    footerPaint,
                )
                val pageLabel = if (totalPages > 0) "Page $pageNumber of $totalPages" else "Page $pageNumber"
                val labelWidth = footerPaint.measureText(pageLabel)
                c.drawText(pageLabel, PAGE_WIDTH - MARGIN_H - labelWidth, footerY, footerPaint)
            }
            currentPage?.let { page ->
                document?.finishPage(page)
            }
            currentPage = null
            canvas = null
        }
    }
}
