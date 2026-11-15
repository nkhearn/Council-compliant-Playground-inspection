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
        private const val MARGIN_H = 34f
        private const val MARGIN_TOP = 36f
        private const val MARGIN_BOTTOM = 44f
        private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_H * 2)

        private val COLOR_NAVY = Color.rgb(17, 43, 76)
        private val COLOR_NAVY_SUBTLE = Color.rgb(230, 236, 244)
        private val COLOR_SLATE = Color.rgb(71, 85, 105)
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
        private val COLOR_RISK_LOW_BG = Color.rgb(224, 242, 254)
        private val COLOR_RISK_LOW_FG = Color.rgb(3, 105, 161)
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

        // Pass 1: calculate exact page count
        val totalPages = calculatePageCount(report)

        // Pass 2: render real PDF pages
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

        // 1. Top Banner & Inspection Metadata
        drawDocumentHeader(state, report)
        drawMetadataBox(state, report)

        // 2. Full Checklist Section by Section
        drawSectionTitleBanner(state, "FULL ROUTINE INSPECTION CHECKLIST", COLOR_NAVY)

        val groupedItems = report.items.groupBy { it.sectionNumber to it.categoryTitle }
        groupedItems.forEach { (sectionKey, sectionItems) ->
            val (secNum, secTitle) = sectionKey
            // Ensure space for category bar + table header + at least one row
            state.ensureSpace(78f)
            drawCategoryHeader(state, "$secNum. ${secTitle.uppercase(Locale.getDefault())}")
            drawChecklistTableHeader(state)

            sectionItems.forEachIndexed { index, item ->
                drawChecklistRow(state, item, isAlternate = index % 2 == 1)
            }
            state.cursorY += 10f
        }

        // 3. Automated Risk Assessment & Action Plan Table
        state.ensureSpace(95f)
        state.cursorY += 4f
        drawSectionTitleBanner(
            state,
            "COMPILED RISK ASSESSMENT & ACTION PLAN (FAILED CHECKS)",
            if (report.failCount > 0) Color.rgb(153, 27, 27) else COLOR_PASS_FG,
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
        state.ensureSpace(120f)
        state.cursorY += 12f
        drawSectionTitleBanner(state, "MAINTENANCE LOG & INSPECTOR SIGN-OFF", COLOR_NAVY)
        drawMaintenanceAndSignOff(state, report)

        state.finishCurrentPage()
        return state.pageNumber
    }

    private fun drawDocumentHeader(state: RenderState, report: InspectionReport) {
        val canvas = state.canvas
        val bgRect = RectF(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + 54f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = COLOR_NAVY }
        canvas?.drawRoundRect(bgRect, 4f, 4f, fillPaint)

        val orgPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(253, 230, 138)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 8.5f
        }

        canvas?.drawText(
            report.metadata.organizationName.uppercase(Locale.getDefault()),
            MARGIN_H + 12f,
            state.cursorY + 16f,
            orgPaint,
        )
        canvas?.drawText(
            report.metadata.checklistTitle,
            MARGIN_H + 12f,
            state.cursorY + 33f,
            titlePaint,
        )
        canvas?.drawText(
            "Form Ref: ${report.metadata.formRef}   |   Offline Field Audit Report",
            MARGIN_H + 12f,
            state.cursorY + 46f,
            subPaint,
        )
        state.cursorY += 62f
    }

    private fun drawMetadataBox(state: RenderState, report: InspectionReport) {
        val boxTop = state.cursorY
        val boxHeight = 82f
        val canvas = state.canvas

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_ZEBRA
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val rect = RectF(MARGIN_H, boxTop, PAGE_WIDTH - MARGIN_H, boxTop + boxHeight)
        canvas?.drawRoundRect(rect, 4f, 4f, boxPaint)
        canvas?.drawRoundRect(rect, 4f, 4f, strokePaint)

        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 9.5f
        }

        val col1X = MARGIN_H + 10f
        val col2X = MARGIN_H + (CONTENT_WIDTH * 0.54f)

        canvas?.drawText("Play Area / Site:", col1X, boxTop + 17f, labelPaint)
        canvas?.drawText(
            report.metadata.siteName.ifBlank { "Not Specified" }.take(38),
            col1X + 86f,
            boxTop + 17f,
            valPaint,
        )

        canvas?.drawText("Inspector Name:", col1X, boxTop + 34f, labelPaint)
        canvas?.drawText(
            report.metadata.inspectorName.ifBlank { "Not Specified" }.take(38),
            col1X + 86f,
            boxTop + 34f,
            valPaint,
        )

        canvas?.drawText("Date & Time:", col2X, boxTop + 17f, labelPaint)
        canvas?.drawText(
            "${report.metadata.inspectionDate}  ${report.metadata.inspectionTime}",
            col2X + 68f,
            boxTop + 17f,
            valPaint,
        )

        canvas?.drawText("Weather:", col2X, boxTop + 34f, labelPaint)
        canvas?.drawText(
            report.metadata.weatherConditions.ifBlank { "Not Specified" }.take(26),
            col2X + 68f,
            boxTop + 34f,
            valPaint,
        )

        // Divider line inside metadata box
        canvas?.drawLine(
            MARGIN_H + 8f,
            boxTop + 45f,
            PAGE_WIDTH - MARGIN_H - 8f,
            boxTop + 45f,
            strokePaint,
        )

        // Summary pills
        val pillY = boxTop + 53f
        var pillX = MARGIN_H + 10f
        pillX += drawPill(canvas, pillX, pillY, "TOTAL: ${report.items.size}", COLOR_NAVY_SUBTLE, COLOR_NAVY) + 8f
        pillX += drawPill(canvas, pillX, pillY, "PASS (YES): ${report.passCount}", COLOR_PASS_BG, COLOR_PASS_FG) + 8f
        pillX += drawPill(canvas, pillX, pillY, "FAIL (NO): ${report.failCount}", COLOR_FAIL_BG, COLOR_FAIL_FG) + 8f
        pillX += drawPill(canvas, pillX, pillY, "N/A: ${report.naCount}", COLOR_NA_BG, COLOR_NA_FG) + 8f

        val riskLabel = report.highestRiskLevel?.let { "RISK: ${it.label.uppercase(Locale.US)}" } ?: "RISK: NONE"
        val (riskBg, riskFg) = when (report.highestRiskLevel) {
            RiskLevel.HIGH -> COLOR_RISK_HIGH_BG to COLOR_RISK_HIGH_FG
            RiskLevel.MEDIUM -> COLOR_RISK_MED_BG to COLOR_RISK_MED_FG
            RiskLevel.LOW -> COLOR_RISK_LOW_BG to COLOR_RISK_LOW_FG
            null -> COLOR_PASS_BG to COLOR_PASS_FG
        }
        drawPill(canvas, pillX, pillY, riskLabel, riskBg, riskFg)

        state.cursorY += boxHeight + 12f
    }

    private fun drawPill(
        canvas: Canvas?,
        x: Float,
        y: Float,
        text: String,
        bgColor: Int,
        fgColor: Int,
    ): Float {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val textWidth = textPaint.measureText(text)
        val width = textWidth + 14f
        val height = 18f
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        canvas?.drawRoundRect(RectF(x, y, x + width, y + height), 9f, 9f, bgPaint)
        canvas?.drawText(text, x + 7f, y + 12.2f, textPaint)
        return width
    }

    private fun drawSectionTitleBanner(state: RenderState, title: String, bgColor: Int) {
        state.ensureSpace(32f)
        val canvas = state.canvas
        val bannerPaint = Paint().apply { color = bgColor }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + 20f, bannerPaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText(title, MARGIN_H + 8f, state.cursorY + 13.5f, textPaint)
        state.cursorY += 24f
    }

    private fun drawCategoryHeader(state: RenderState, title: String) {
        val canvas = state.canvas
        val paint = Paint().apply { color = COLOR_NAVY_SUBTLE }
        val borderPaint = Paint().apply {
            color = COLOR_NAVY
            strokeWidth = 2f
        }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + 18f, paint)
        canvas?.drawLine(MARGIN_H, state.cursorY, MARGIN_H, state.cursorY + 18f, borderPaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText(title, MARGIN_H + 8f, state.cursorY + 12.5f, textPaint)
        state.cursorY += 18f
    }

    private fun drawChecklistTableHeader(state: RenderState) {
        val canvas = state.canvas
        val bgPaint = Paint().apply { color = COLOR_SLATE }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + 16f, bgPaint)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val colQuestionX = MARGIN_H + 6f
        val colStatusX = MARGIN_H + 292f
        val colNotesX = MARGIN_H + 356f

        canvas?.drawText("Ref & Inspection Check", colQuestionX, state.cursorY + 11f, headerPaint)
        canvas?.drawText("Status", colStatusX, state.cursorY + 11f, headerPaint)
        canvas?.drawText("Inspector Notes / Defect Summary", colNotesX, state.cursorY + 11f, headerPaint)
        state.cursorY += 16f
    }

    private fun drawChecklistRow(
        state: RenderState,
        item: InspectionItem,
        isAlternate: Boolean,
    ) {
        val questionWidth = 278f
        val notesWidth = CONTENT_WIDTH - 362f

        val qPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.4f
        }
        val nPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (item.status == InspectionStatus.FAIL) COLOR_FAIL_FG else COLOR_TEXT_MUTED
            textSize = 8.0f
        }

        val noteText = buildString {
            if (item.status == InspectionStatus.FAIL) {
                if (item.defectDetails.isNotBlank()) {
                    append(item.defectDetails.trim())
                } else {
                    append("Defect recorded (details pending)")
                }
                val riskStr = item.riskLevel?.label ?: "Medium"
                val actionStr = item.actionRequired?.label ?: "Monitor"
                append("  [Risk: $riskStr | Action: $actionStr]")
            } else if (item.inlineNote.isNotBlank()) {
                append(item.inlineNote.trim())
            } else {
                append("—")
            }
        }

        val qLayout = buildLayout("${item.referenceCode}  ${item.question}", qPaint, questionWidth.toInt())
        val nLayout = buildLayout(noteText, nPaint, notesWidth.toInt())
        val rowHeight = maxOf(qLayout.height, nLayout.height, 16) + 10f

        state.ensureSpace(rowHeight + 4f) {
            drawCategoryHeader(state, "${item.sectionNumber}. ${item.categoryTitle.uppercase(Locale.getDefault())} (cont.)")
            drawChecklistTableHeader(state)
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
            strokeWidth = 0.6f
        }
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, bgPaint)
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, borderPaint)

        // Question text
        drawStaticLayout(canvas, qLayout, MARGIN_H + 6f, rowTop + 5f)

        // Status badge
        val (badgeBg, badgeFg) = when (item.status) {
            InspectionStatus.PASS -> COLOR_PASS_BG to COLOR_PASS_FG
            InspectionStatus.FAIL -> COLOR_FAIL_BG to COLOR_FAIL_FG
            InspectionStatus.NA -> COLOR_NA_BG to COLOR_NA_FG
            InspectionStatus.UNANSWERED -> COLOR_PENDING_BG to COLOR_PENDING_FG
        }
        drawPill(canvas, MARGIN_H + 292f, rowTop + 4f, item.status.pdfLabel, badgeBg, badgeFg)

        // Notes / Defect text
        drawStaticLayout(canvas, nLayout, MARGIN_H + 356f, rowTop + 5f)

        state.cursorY = rowBottom
    }

    private fun drawNoDefectsBox(state: RenderState) {
        state.ensureSpace(36f)
        val canvas = state.canvas
        val rect = RectF(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + 30f)
        val bgPaint = Paint().apply { color = COLOR_PASS_BG }
        val strokePaint = Paint().apply {
            color = COLOR_PASS_FG
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas?.drawRoundRect(rect, 4f, 4f, bgPaint)
        canvas?.drawRoundRect(rect, 4f, 4f, strokePaint)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_PASS_FG
            textSize = 9.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText(
            "Zero failed checks recorded. All inspected play equipment and surfacing met safety standards.",
            MARGIN_H + 10f,
            state.cursorY + 18.5f,
            textPaint,
        )
        state.cursorY += 36f
    }

    private fun drawRiskTableHeader(state: RenderState) {
        val canvas = state.canvas
        val bgPaint = Paint().apply { color = Color.rgb(127, 29, 29) }
        canvas?.drawRect(MARGIN_H, state.cursorY, PAGE_WIDTH - MARGIN_H, state.cursorY + 17f, bgPaint)

        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas?.drawText("Ref & Equipment", MARGIN_H + 6f, state.cursorY + 11.5f, headerPaint)
        canvas?.drawText("Failed Check & Defect Details", MARGIN_H + 135f, state.cursorY + 11.5f, headerPaint)
        canvas?.drawText("Risk", MARGIN_H + 382f, state.cursorY + 11.5f, headerPaint)
        canvas?.drawText("Action Required", MARGIN_H + 440f, state.cursorY + 11.5f, headerPaint)
        state.cursorY += 17f
    }

    private fun drawRiskTableRow(
        state: RenderState,
        entry: com.example.model.RiskAssessmentEntry,
        isAlternate: Boolean,
    ) {
        val equipPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 8.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val defectPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.2f
        }
        val actionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val equipLayout = buildLayout("${entry.referenceCode} — ${entry.categoryTitle}", equipPaint, 122)
        val defectLayout = buildLayout(
            "Check: ${entry.question}\nDefect: ${entry.defectDetails}",
            defectPaint,
            240,
        )
        val actionLayout = buildLayout(entry.actionRequired.label, actionPaint, 82)
        val rowHeight = maxOf(equipLayout.height, defectLayout.height, actionLayout.height, 20) + 12f

        state.ensureSpace(rowHeight + 4f) {
            drawSectionTitleBanner(state, "COMPILED RISK ASSESSMENT & ACTION PLAN (CONT.)", Color.rgb(153, 27, 27))
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
            strokeWidth = 0.7f
        }
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, bgPaint)
        canvas?.drawRect(MARGIN_H, rowTop, PAGE_WIDTH - MARGIN_H, rowBottom, borderPaint)

        drawStaticLayout(canvas, equipLayout, MARGIN_H + 6f, rowTop + 6f)
        drawStaticLayout(canvas, defectLayout, MARGIN_H + 135f, rowTop + 6f)

        val (riskBg, riskFg) = when (entry.riskLevel) {
            RiskLevel.HIGH -> COLOR_RISK_HIGH_BG to COLOR_RISK_HIGH_FG
            RiskLevel.MEDIUM -> COLOR_RISK_MED_BG to COLOR_RISK_MED_FG
            RiskLevel.LOW -> COLOR_RISK_LOW_BG to COLOR_RISK_LOW_FG
        }
        drawPill(canvas, MARGIN_H + 380f, rowTop + 5f, entry.riskLevel.label.uppercase(Locale.US), riskBg, riskFg)
        drawStaticLayout(canvas, actionLayout, MARGIN_H + 440f, rowTop + 6f)

        state.cursorY = rowBottom
    }

    private fun drawMaintenanceAndSignOff(state: RenderState, report: InspectionReport) {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TEXT_PRIMARY
            textSize = 8.5f
        }
        val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_NAVY
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        if (report.maintenanceLogs.isNotEmpty()) {
            report.maintenanceLogs.forEach { log ->
                val line = "• ${log.equipmentItem.ifBlank { "Equipment" }}: ${log.repairCarriedOut.ifBlank { "Maintenance recorded" }}" +
                    (if (log.partsUsed.isNotBlank()) " | Parts: ${log.partsUsed}" else "") +
                    (if (log.completedByDate.isNotBlank()) " | Completed: ${log.completedByDate}" else "")
                val layout = buildLayout(line, textPaint, CONTENT_WIDTH.toInt() - 16)
                state.ensureSpace(layout.height + 8f)
                drawStaticLayout(state.canvas, layout, MARGIN_H + 8f, state.cursorY + 2f)
                state.cursorY += layout.height + 6f
            }
        }

        val commentsText = report.metadata.overallComments.ifBlank { "None recorded." }
        val commentsLayout = buildLayout("Overall Comments: $commentsText", textPaint, CONTENT_WIDTH.toInt() - 16)
        val signBoxHeight = commentsLayout.height + 46f

        state.ensureSpace(signBoxHeight + 8f)
        val canvas = state.canvas
        val boxTop = state.cursorY + 4f
        val rect = RectF(MARGIN_H, boxTop, PAGE_WIDTH - MARGIN_H, boxTop + signBoxHeight)
        val bgPaint = Paint().apply { color = COLOR_ZEBRA }
        val borderPaint = Paint().apply {
            color = COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas?.drawRoundRect(rect, 4f, 4f, bgPaint)
        canvas?.drawRoundRect(rect, 4f, 4f, borderPaint)

        drawStaticLayout(canvas, commentsLayout, MARGIN_H + 8f, boxTop + 8f)

        val footerLineY = boxTop + commentsLayout.height + 22f
        canvas?.drawText(
            "Overall Action Required: ${report.metadata.overallActionRequired.label}",
            MARGIN_H + 8f,
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
            MARGIN_H + 8f,
            footerLineY + 14f,
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = COLOR_TEXT_MUTED
                textSize = 8f
            },
        )
        state.cursorY = boxTop + signBoxHeight + 8f
    }

    private fun buildLayout(text: String, paint: TextPaint, widthPx: Int): StaticLayout {
        val safeWidth = widthPx.coerceAtLeast(40)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(1.5f, 1f)
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
                textSize = 7.8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val linePaint = Paint().apply {
                color = COLOR_BORDER
                strokeWidth = 0.6f
            }
            c?.drawText(
                "${report.metadata.siteName.ifBlank { "PLAY AREA" }.uppercase(Locale.getDefault())} — SAFETY INSPECTION",
                MARGIN_H,
                22f,
                paint,
            )
            c?.drawText(
                "${report.metadata.inspectionDate}  ${report.metadata.inspectionTime}",
                PAGE_WIDTH - MARGIN_H - 92f,
                22f,
                paint,
            )
            c?.drawLine(MARGIN_H, 26f, PAGE_WIDTH - MARGIN_H, 26f, linePaint)
            cursorY = 34f
        }

        fun finishCurrentPage() {
            val c = canvas
            if (c != null) {
                val linePaint = Paint().apply {
                    color = COLOR_BORDER
                    strokeWidth = 0.6f
                }
                val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = COLOR_TEXT_MUTED
                    textSize = 7.8f
                }
                val footerY = PAGE_HEIGHT - 22f
                c.drawLine(MARGIN_H, footerY - 10f, PAGE_WIDTH - MARGIN_H, footerY - 10f, linePaint)
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
