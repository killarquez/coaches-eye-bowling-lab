package com.example.cebowlinglabtrack.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.cebowlinglabtrack.domain.model.BowlerProfile
import com.example.cebowlinglabtrack.domain.model.LaneConstants
import com.example.cebowlinglabtrack.domain.model.ShotData
import com.example.cebowlinglabtrack.domain.model.SpectoSkillTier
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Multi-page PDF Report Generator matching the authentic Kegel Specto session report
 * (DOC-20260119-WA0006.pdf / Alfredo Quilarquez - Western Pacific Training Center).
 *
 * Emits US Letter (612 x 792 pt) PDF documents complete with:
 * - Page 1: Executive Dashboard, Accuracy/Power scores, Biomechanical HUD, 2D Lane Trajectory & Dispersion, Skill Tier Matrix
 * - Page 2: Spatial Accuracy Consistency Trends (Laydown, Arrow, Breakpoint, Entry)
 * - Page 3: Speed, RPM & Biomechanical Kinematics Trends
 * - Page 4+: Complete 22-Parameter Kegel Specto Shot Table with session averages & ranges
 */
object SessionPdfReportGenerator {

    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792

    // CE Bowling Lab Brand Palette
    private const val COLOR_NAVY = 0xFF00205B.toInt()
    private const val COLOR_MIDNIGHT = 0xFF071536.toInt()
    private const val COLOR_GOLD = 0xFFC39D5E.toInt()
    private const val COLOR_CRIMSON = 0xFFC8102E.toInt()
    private const val COLOR_CARD_BG = 0xFFF8FAFC.toInt()
    private const val COLOR_BORDER = 0xFFE2E8F0.toInt()
    private const val COLOR_TEXT_DARK = 0xFF0F172A.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()
    private const val COLOR_CYAN = 0xFF06B6D4.toInt()
    private const val COLOR_SUCCESS = 0xFF10B981.toInt()

    fun generateSessionPdf(
        context: Context,
        bowler: BowlerProfile,
        shots: List<ShotData>,
        sessionTitle: String = "Coach's Eye Training Session"
    ): File {
        val document = PdfDocument()
        val safeShots = if (shots.isEmpty()) generatePlaceholderShots(bowler) else shots

        var pageNumber = 1

        // Page 1: Overview, Scores, 2D Lane, Tier Matrix
        val page1Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
        val page1 = document.startPage(page1Info)
        renderPage1Overview(page1.canvas, bowler, safeShots, sessionTitle)
        document.finishPage(page1)

        // Page 2: Spatial Accuracy Consistency Trends
        val page2Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
        val page2 = document.startPage(page2Info)
        renderPage2SpatialTrends(page2.canvas, bowler, safeShots)
        document.finishPage(page2)

        // Page 3: Speed, RPM & Biomechanics Trends
        val page3Info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
        val page3 = document.startPage(page3Info)
        renderPage3SpeedAndKinematics(page3.canvas, bowler, safeShots)
        document.finishPage(page3)

        // Page 4+: 22-Parameter Shot Table (paginated 28 shots per page)
        val shotsPerPage = 28
        val totalTablePages = ((safeShots.size - 1) / shotsPerPage) + 1
        for (tp in 0 until totalTablePages) {
            val fromIndex = tp * shotsPerPage
            val toIndex = min(fromIndex + shotsPerPage, safeShots.size)
            val subList = safeShots.subList(fromIndex, toIndex)

            val tablePageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber++).create()
            val tablePage = document.startPage(tablePageInfo)
            renderShotTablePage(tablePage.canvas, bowler, safeShots, subList, fromIndex + 1, tp + 1, totalTablePages)
            document.finishPage(tablePage)
        }

        // Write to reports directory in cache
        val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
        val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sanitizedName = bowler.name.replace("\\s+".toRegex(), "_")
        val outFile = File(reportsDir, "SpectoReport_${sanitizedName}_$dateStamp.pdf")

        FileOutputStream(outFile).use { fos ->
            document.writeTo(fos)
        }
        document.close()

        return outFile
    }

    /**
     * Creates an Android ACTION_SEND Intent to share the report via WhatsApp, Email, Drive, etc.
     */
    fun createSharePdfIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Coach's Eye Bowling Lab - Specto Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the Specto Telemetry Session Report generated by Coach's Eye Bowling Lab.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // =========================================================================
    // PAGE 1: OVERVIEW & BIOMECHANICAL DASHBOARD
    // =========================================================================

    private fun renderPage1Overview(
        canvas: Canvas,
        bowler: BowlerProfile,
        shots: List<ShotData>,
        sessionTitle: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Top Header Banner
        paint.color = COLOR_NAVY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 72f, paint)

        // Header Gold Accent Bar
        paint.color = COLOR_GOLD
        canvas.drawRect(0f, 72f, PAGE_WIDTH.toFloat(), 76f, paint)

        // Brand Title
        paint.color = Color.WHITE
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("COACH'S EYE BOWLING LAB", 24f, 32f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_GOLD
        canvas.drawText("KEGEL SPECTO TELEMETRY & BIOMECHANICAL ANALYSIS REPORT", 24f, 48f, paint)

        val dateStr = SimpleDateFormat("MMM dd, yyyy  h:mm a", Locale.US).format(Date())
        paint.color = Color.WHITE
        paint.textSize = 9f
        canvas.drawText("Session: $dateStr", PAGE_WIDTH - 210f, 32f, paint)
        canvas.drawText("Location: Western Pacific Bowling Training Center", PAGE_WIDTH - 210f, 48f, paint)

        // 2. Athlete Profile Bar (y: 86 .. 140)
        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(24f, 86f, PAGE_WIDTH - 24f, 138f, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(24f, 86f, PAGE_WIDTH - 24f, 138f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_TEXT_DARK
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Bowler: ${bowler.name}", 36f, 106f, paint)

        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("ID: ${bowler.id}  •  Style: ${bowler.style.displayName()}  •  Hand: ${bowler.handedness.name}  •  Height: ${bowler.heightInches.toInt()}\"", 36f, 124f, paint)

        val totalShotsText = "${shots.size} Shots Analyzed"
        paint.color = COLOR_NAVY
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(totalShotsText, PAGE_WIDTH - 140f, 106f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("Book Avg: ${bowler.bookAverage}  •  PAP: ${bowler.papCoordinates}", PAGE_WIDTH - 260f, 124f, paint)

        // 3. Overall Accuracy & Power Scores (y: 148 .. 218)
        val avgAcc = shots.map { it.spectoTelemetry.dynamics.accuracyScore }.average()
        val avgPower = shots.map { it.spectoTelemetry.dynamics.powerScore }.average()
        val avgRpm = shots.map { it.spectoTelemetry.dynamics.rpm }.average().toInt()
        val avgLaunchSpd = shots.map { it.spectoTelemetry.speed.launchSpeedMph }.average()
        val avgEntrySpd = shots.map { it.spectoTelemetry.speed.entrySpeedMph }.average()
        val avgTilt = shots.map { it.spectoTelemetry.dynamics.axisTiltDeg }.average()
        val avgRot = shots.map { it.spectoTelemetry.dynamics.axisRotationDeg }.average()
        val avgRevs = shots.map { it.spectoTelemetry.dynamics.totalRotations }.average()

        // Card 1: Accuracy Score
        drawMetricScoreCard(
            canvas = canvas,
            left = 24f, top = 148f, right = 158f, bottom = 220f,
            title = "ACCURACY SCORE",
            value = String.format(Locale.US, "%.1f", avgAcc),
            subtext = "Lower is better (Pro < 50.0)",
            accentColor = COLOR_NAVY
        )

        // Card 2: Power Score
        drawMetricScoreCard(
            canvas = canvas,
            left = 168f, top = 148f, right = 302f, bottom = 220f,
            title = "POWER SCORE",
            value = String.format(Locale.US, "%.1f", avgPower),
            subtext = "Scale 0 - 10 (Tour avg 7.5)",
            accentColor = COLOR_CRIMSON
        )

        // Card 3: Rev Rate (RPM) - Optical COG to PAP
        drawMetricScoreCard(
            canvas = canvas,
            left = 312f, top = 148f, right = 446f, bottom = 220f,
            title = "OPTICAL REV RATE",
            value = "$avgRpm RPM",
            subtext = String.format(Locale.US, "%.1f revs • %d° tilt", avgRevs, avgTilt.roundToInt()),
            accentColor = COLOR_CYAN
        )

        // Card 4: Launch Speed
        drawMetricScoreCard(
            canvas = canvas,
            left = 456f, top = 148f, right = PAGE_WIDTH - 24f, bottom = 220f,
            title = "LAUNCH SPEED",
            value = String.format(Locale.US, "%.1f mph", avgLaunchSpd),
            subtext = String.format(Locale.US, "Entry: %.1f mph", avgEntrySpd),
            accentColor = COLOR_GOLD
        )

        // 4. Biomechanical & Trajectory HUD Grid (y: 228 .. 332)
        val hudLeft = 24f
        val hudTop = 228f
        val hudWidth = PAGE_WIDTH - 48f
        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(hudLeft, hudTop, hudLeft + hudWidth, hudTop + 104f, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(hudLeft, hudTop, hudLeft + hudWidth, hudTop + 104f, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_NAVY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SESSION LAUNCH & SPATIAL AVERAGES", hudLeft + 12f, hudTop + 18f, paint)

        val avgLaydown = shots.map { it.spectoTelemetry.spatial.laydownBoard }.average()
        val avgArrow = shots.map { it.spectoTelemetry.spatial.arrowBoard }.average()
        val avgBrkBd = shots.map { it.spectoTelemetry.spatial.breakpointBoard }.average()
        val avgBrkFt = shots.map { it.spectoTelemetry.spatial.breakpointDistanceFt }.average()
        val avgEntryBd = shots.map { it.spectoTelemetry.spatial.entryBoard }.average()
        val avgLaunchAng = shots.map { it.spectoTelemetry.angles.launchAngleDeg }.average()
        val avgImpactAng = shots.map { it.spectoTelemetry.angles.impactAngleDeg }.average()

        val hudCols = listOf(
            Pair("Laydown Board", String.format(Locale.US, "%.1f bd", avgLaydown)),
            Pair("Arrow Board (15ft)", String.format(Locale.US, "%.1f bd", avgArrow)),
            Pair("Breakpoint", String.format(Locale.US, "%.1f bd @ %.1fft", avgBrkBd, avgBrkFt)),
            Pair("Pocket Entry (60ft)", String.format(Locale.US, "%.1f bd", avgEntryBd)),
            Pair("Launch Angle", String.format(Locale.US, "%+.1f°", avgLaunchAng)),
            Pair("Impact Angle", String.format(Locale.US, "%.1f°", avgImpactAng)),
            Pair("Axis Tilt", String.format(Locale.US, "%.1f°", avgTilt)),
            Pair("Axis Rotation", String.format(Locale.US, "%.1f°", avgRot))
        )

        val cellW = hudWidth / 4f
        for (i in hudCols.indices) {
            val row = i / 4
            val col = i % 4
            val cx = hudLeft + 12f + col * cellW
            val cy = hudTop + 40f + row * 34f

            paint.textSize = 8f
            paint.typeface = Typeface.DEFAULT
            paint.color = COLOR_TEXT_MUTED
            canvas.drawText(hudCols[i].first, cx, cy, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = COLOR_TEXT_DARK
            canvas.drawText(hudCols[i].second, cx, cy + 14f, paint)
        }

        // 5. 2D Lane Trajectory & Dispersion Canvas (y: 340 .. 570)
        drawLaneTrajectoryDispersion(canvas, 24f, 340f, PAGE_WIDTH - 48f, 225f, shots)

        // 6. Skill Level Performance Matrix (y: 574 .. 750)
        drawSkillTierMatrix(canvas, 24f, 574f, PAGE_WIDTH - 48f, 170f, shots)

        // Page footer
        drawPageFooter(canvas, 1, 4)
    }

    // =========================================================================
    // PAGE 2: SPATIAL ACCURACY CONSISTENCY TRENDS
    // =========================================================================

    private fun renderPage2SpatialTrends(
        canvas: Canvas,
        bowler: BowlerProfile,
        shots: List<ShotData>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        drawSectionHeader(canvas, "SPATIAL ACCURACY & DISPERSION TRENDS (SHOT-BY-SHOT)", bowler)

        // 4 Trend Graphs: Laydown (0ft), Arrows (15ft), Breakpoint (42ft), Entry (60ft)
        val graphW = (PAGE_WIDTH - 60f) / 2f
        val graphH = 150f

        // Chart 1: Laydown Board
        val laydownData = shots.map { it.spectoTelemetry.spatial.laydownBoard.toFloat() }
        drawTrendChart(
            canvas = canvas,
            left = 24f, top = 88f, width = graphW, height = graphH,
            title = "Laydown Board (Foul Line 0 ft)",
            yUnit = "Bd",
            data = laydownData,
            nominalBoard = 24f,
            lineColor = COLOR_NAVY
        )

        // Chart 2: Arrow Board
        val arrowData = shots.map { it.spectoTelemetry.spatial.arrowBoard.toFloat() }
        drawTrendChart(
            canvas = canvas,
            left = 36f + graphW, top = 88f, width = graphW, height = graphH,
            title = "Arrow Board (15 ft Target)",
            yUnit = "Bd",
            data = arrowData,
            nominalBoard = 16f,
            lineColor = COLOR_CYAN
        )

        // Chart 3: Breakpoint Board
        val brkData = shots.map { it.spectoTelemetry.spatial.breakpointBoard.toFloat() }
        drawTrendChart(
            canvas = canvas,
            left = 24f, top = 256f, width = graphW, height = graphH,
            title = "Breakpoint Board (Downlane)",
            yUnit = "Bd",
            data = brkData,
            nominalBoard = 7f,
            lineColor = COLOR_GOLD
        )

        // Chart 4: Pocket Entry Board
        val entryData = shots.map { it.spectoTelemetry.spatial.entryBoard.toFloat() }
        drawTrendChart(
            canvas = canvas,
            left = 36f + graphW, top = 256f, width = graphW, height = graphH,
            title = "Pocket Entry Board (60 ft)",
            yUnit = "Bd",
            data = entryData,
            nominalBoard = 17f,
            lineColor = COLOR_CRIMSON
        )

        // Summary Consistency Table (y: 424 .. 740)
        drawSpatialSummaryTable(canvas, 24f, 424f, PAGE_WIDTH - 48f, 316f, shots)

        drawPageFooter(canvas, 2, 4)
    }

    // =========================================================================
    // PAGE 3: SPEED, RPM & BIOMECHANICAL CONSISTENCY
    // =========================================================================

    private fun renderPage3SpeedAndKinematics(
        canvas: Canvas,
        bowler: BowlerProfile,
        shots: List<ShotData>
    ) {
        drawSectionHeader(canvas, "SPEED DECAY, REVS & BIOMECHANICAL CONSISTENCY", bowler)

        val graphW = PAGE_WIDTH - 48f
        val graphH = 135f

        // Chart 1: Speed Consistency (Launch & Entry Speed)
        val launchSpeedData = shots.map { it.spectoTelemetry.speed.launchSpeedMph.toFloat() }
        val entrySpeedData = shots.map { it.spectoTelemetry.speed.entrySpeedMph.toFloat() }
        drawDualTrendChart(
            canvas = canvas,
            left = 24f, top = 88f, width = graphW, height = graphH,
            title = "Speed Consistency & Speed Loss (Launch vs Entry)",
            yUnit = "mph",
            data1 = launchSpeedData, label1 = "Launch Spd", color1 = COLOR_NAVY,
            data2 = entrySpeedData, label2 = "Entry Spd", color2 = COLOR_CYAN
        )

        // Chart 2: RPM Consistency (Optical COG-to-PAP)
        val rpmData = shots.map { it.spectoTelemetry.dynamics.rpm.toFloat() }
        val revsData = shots.map { it.spectoTelemetry.dynamics.totalRotations.toFloat() }
        drawDualTrendChart(
            canvas = canvas,
            left = 24f, top = 240f, width = graphW, height = graphH,
            title = "Optical Rev Rate (RPM) & Total Ball Revolutions",
            yUnit = "RPM",
            data1 = rpmData, label1 = "Optical RPM", color1 = COLOR_GOLD,
            data2 = revsData, label2 = "Revs (scaled)", color2 = COLOR_CRIMSON,
            scaleData2 = 25f // scale 14 revs to ~350 for visual comparison
        )

        // Biomechanical Kinematics Table (y: 392 .. 740)
        drawKinematicsSummarySection(canvas, 24f, 392f, PAGE_WIDTH - 48f, 348f, bowler, shots)

        drawPageFooter(canvas, 3, 4)
    }

    // =========================================================================
    // PAGE 4+: 22-PARAMETER SHOT TABLE
    // =========================================================================

    private fun renderShotTablePage(
        canvas: Canvas,
        bowler: BowlerProfile,
        allShots: List<ShotData>,
        pageShots: List<ShotData>,
        startShotNum: Int,
        currentPage: Int,
        totalPages: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val title = "SPECTO TELEMETRY SHOT-BY-SHOT AUDIT LOG (Page $currentPage of $totalPages)"
        drawSectionHeader(canvas, title, bowler)

        val tableLeft = 16f
        val tableTop = 86f
        val tableWidth = PAGE_WIDTH - 32f
        val rowHeight = 20f

        val headers = listOf("#", "Layd", "Loft", "Arrw", "BrkBd", "BrkFt", "EntBd", "ExtBd", "Defl", "Lnch°", "Imp°", "LSpd", "ESpd", "RPM", "Tilt°", "Rot°", "Pwr", "Acc")
        val colWidths = floatArrayOf(24f, 32f, 30f, 32f, 36f, 34f, 34f, 34f, 30f, 34f, 32f, 36f, 34f, 36f, 30f, 30f, 28f, 28f)

        // Table Header Row
        paint.color = COLOR_NAVY
        canvas.drawRect(tableLeft, tableTop, tableLeft + tableWidth, tableTop + rowHeight, paint)

        var curX = tableLeft
        paint.color = Color.WHITE
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        for (c in headers.indices) {
            val cw = colWidths[c]
            canvas.drawText(headers[c], curX + 3f, tableTop + 14f, paint)
            curX += cw
        }

        // Table Data Rows
        var curY = tableTop + rowHeight
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7f

        for (i in pageShots.indices) {
            val shot = pageShots[i]
            val isEven = i % 2 == 0
            paint.color = if (isEven) Color.WHITE else COLOR_CARD_BG
            canvas.drawRect(tableLeft, curY, tableLeft + tableWidth, curY + rowHeight, paint)

            paint.color = COLOR_BORDER
            paint.style = Paint.Style.STROKE
            canvas.drawLine(tableLeft, curY + rowHeight, tableLeft + tableWidth, curY + rowHeight, paint)
            paint.style = Paint.Style.FILL

            val sp = shot.spectoTelemetry.spatial
            val an = shot.spectoTelemetry.angles
            val sd = shot.spectoTelemetry.speed
            val dy = shot.spectoTelemetry.dynamics

            val values = listOf(
                shot.shotNumber.toString(),
                String.format(Locale.US, "%.1f", sp.laydownBoard),
                String.format(Locale.US, "%.1f", sp.loftDistanceFt),
                String.format(Locale.US, "%.1f", sp.arrowBoard),
                String.format(Locale.US, "%.1f", sp.breakpointBoard),
                String.format(Locale.US, "%.0f", sp.breakpointDistanceFt),
                String.format(Locale.US, "%.1f", sp.entryBoard),
                String.format(Locale.US, "%.1f", sp.pinDeckExitBoard),
                String.format(Locale.US, "%.1f", sp.pinDeckDeflection),
                String.format(Locale.US, "%+.1f", an.launchAngleDeg),
                String.format(Locale.US, "%.1f", an.impactAngleDeg),
                String.format(Locale.US, "%.1f", sd.launchSpeedMph),
                String.format(Locale.US, "%.1f", sd.entrySpeedMph),
                dy.rpm.toString(),
                String.format(Locale.US, "%.0f", dy.axisTiltDeg),
                String.format(Locale.US, "%.0f", dy.axisRotationDeg),
                String.format(Locale.US, "%.1f", dy.powerScore),
                String.format(Locale.US, "%.0f", dy.accuracyScore)
            )

            curX = tableLeft
            paint.color = COLOR_TEXT_DARK
            for (c in values.indices) {
                canvas.drawText(values[c], curX + 3f, curY + 14f, paint)
                curX += colWidths[c]
            }

            curY += rowHeight
        }

        // Summary Row if last table page
        if (currentPage == totalPages) {
            paint.color = COLOR_MIDNIGHT
            canvas.drawRect(tableLeft, curY, tableLeft + tableWidth, curY + rowHeight, paint)

            paint.color = COLOR_GOLD
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 7.5f

            val avgLayd = allShots.map { it.spectoTelemetry.spatial.laydownBoard }.average()
            val avgLoft = allShots.map { it.spectoTelemetry.spatial.loftDistanceFt }.average()
            val avgArrw = allShots.map { it.spectoTelemetry.spatial.arrowBoard }.average()
            val avgBrkBd = allShots.map { it.spectoTelemetry.spatial.breakpointBoard }.average()
            val avgBrkFt = allShots.map { it.spectoTelemetry.spatial.breakpointDistanceFt }.average()
            val avgEntBd = allShots.map { it.spectoTelemetry.spatial.entryBoard }.average()
            val avgExtBd = allShots.map { it.spectoTelemetry.spatial.pinDeckExitBoard }.average()
            val avgDefl = allShots.map { it.spectoTelemetry.spatial.pinDeckDeflection }.average()
            val avgLnch = allShots.map { it.spectoTelemetry.angles.launchAngleDeg }.average()
            val avgImp = allShots.map { it.spectoTelemetry.angles.impactAngleDeg }.average()
            val avgLSpd = allShots.map { it.spectoTelemetry.speed.launchSpeedMph }.average()
            val avgESpd = allShots.map { it.spectoTelemetry.speed.entrySpeedMph }.average()
            val avgRpm = allShots.map { it.spectoTelemetry.dynamics.rpm }.average().toInt()
            val avgTilt = allShots.map { it.spectoTelemetry.dynamics.axisTiltDeg }.average()
            val avgRot = allShots.map { it.spectoTelemetry.dynamics.axisRotationDeg }.average()
            val avgPwr = allShots.map { it.spectoTelemetry.dynamics.powerScore }.average()
            val avgAcc = allShots.map { it.spectoTelemetry.dynamics.accuracyScore }.average()

            val avgValues = listOf(
                "AVG",
                String.format(Locale.US, "%.1f", avgLayd),
                String.format(Locale.US, "%.1f", avgLoft),
                String.format(Locale.US, "%.1f", avgArrw),
                String.format(Locale.US, "%.1f", avgBrkBd),
                String.format(Locale.US, "%.0f", avgBrkFt),
                String.format(Locale.US, "%.1f", avgEntBd),
                String.format(Locale.US, "%.1f", avgExtBd),
                String.format(Locale.US, "%.1f", avgDefl),
                String.format(Locale.US, "%+.1f", avgLnch),
                String.format(Locale.US, "%.1f", avgImp),
                String.format(Locale.US, "%.1f", avgLSpd),
                String.format(Locale.US, "%.1f", avgESpd),
                avgRpm.toString(),
                String.format(Locale.US, "%.0f", avgTilt),
                String.format(Locale.US, "%.0f", avgRot),
                String.format(Locale.US, "%.1f", avgPwr),
                String.format(Locale.US, "%.0f", avgAcc)
            )

            curX = tableLeft
            for (c in avgValues.indices) {
                canvas.drawText(avgValues[c], curX + 3f, curY + 14f, paint)
                curX += colWidths[c]
            }
        }

        drawPageFooter(canvas, 3 + currentPage, 3 + totalPages)
    }

    // =========================================================================
    // DRAWING HELPERS: 2D LANE, CHARTS, MATRICES, HEADERS
    // =========================================================================

    private fun drawLaneTrajectoryDispersion(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        shots: List<ShotData>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Card frame
        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Title
        paint.color = COLOR_NAVY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2D LANE TRAJECTORY & SHOT DISPERSION CONE (39 BOARDS x 60 FT)", left + 12f, top + 18f, paint)

        val laneLeft = left + 36f
        val laneRight = left + width - 36f
        val laneTop = top + 34f
        val laneBottom = top + height - 20f
        val laneW = laneRight - laneLeft
        val laneH = laneBottom - laneTop

        // Lane floor (tan bowling wood color)
        paint.color = 0xFFF1E4C3.toInt()
        canvas.drawRect(laneLeft, laneTop, laneRight, laneBottom, paint)

        // Gutters (slate gray)
        paint.color = 0xFFCBD5E1.toInt()
        canvas.drawRect(laneLeft - 10f, laneTop, laneLeft, laneBottom, paint)
        canvas.drawRect(laneRight, laneTop, laneRight + 10f, laneBottom, paint)

        // Foul Line (Red 0ft - bottom)
        paint.color = COLOR_CRIMSON
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(laneLeft, laneBottom, laneRight, laneBottom, paint)

        // Arrows (15ft)
        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        val yArrows = laneBottom - (15f / 60f) * laneH
        canvas.drawLine(laneLeft, yArrows, laneRight, yArrows, paint)

        // Range Finders (40ft / oil exit)
        val yRange = laneBottom - (40f / 60f) * laneH
        canvas.drawLine(laneLeft, yRange, laneRight, yRange, paint)

        // Pin Deck (60ft - top)
        val yDeck = laneTop
        paint.color = COLOR_MIDNIGHT
        paint.strokeWidth = 2f
        canvas.drawLine(laneLeft, yDeck, laneRight, yDeck, paint)

        // Board Guide lines (every 5 boards: 5, 10, 15, 20, 25, 30, 35)
        paint.strokeWidth = 0.5f
        paint.color = 0x33000000
        for (b in 5..35 step 5) {
            val bx = laneRight - (b / 39f) * laneW // board 1 is right gutter, 39 is left
            canvas.drawLine(bx, laneTop, bx, laneBottom, paint)
        }

        // Draw session trajectory lines
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f
        for (shot in shots) {
            val traj = shot.trajectoryPoints
            if (traj.size < 2) continue

            val path = Path()
            var started = false
            for (pt in traj) {
                val px = laneRight - (pt.board / 39f).toFloat() * laneW
                val py = laneBottom - (pt.yFt / 60f).toFloat() * laneH
                if (!started) {
                    path.moveTo(px, py)
                    started = true
                } else {
                    path.lineTo(px, py)
                }
            }
            paint.color = 0x4400205B.toInt()
            canvas.drawPath(path, paint)
        }

        // Draw average trajectory highlighted in bright Gold
        val avgPath = Path()
        val sortedAvgPoints = (0..60 step 3).map { dist ->
            val boardsAtDist = shots.map { s ->
                val p1 = s.trajectoryPoints.minByOrNull { kotlin.math.abs(it.yFt - dist) }
                p1?.board ?: 16.0
            }
            Pair(dist.toFloat(), boardsAtDist.average().toFloat())
        }

        for (i in sortedAvgPoints.indices) {
            val (dist, bd) = sortedAvgPoints[i]
            val px = laneRight - (bd / 39f) * laneW
            val py = laneBottom - (dist / 60f) * laneH
            if (i == 0) avgPath.moveTo(px, py) else avgPath.lineTo(px, py)
        }
        paint.color = COLOR_GOLD
        paint.strokeWidth = 2.5f
        canvas.drawPath(avgPath, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 7.5f
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("Foul Line (0 ft)", laneLeft - 10f, laneBottom + 12f, paint)
        canvas.drawText("Arrows (15 ft)", laneLeft - 10f, yArrows - 2f, paint)
        canvas.drawText("Range Finders (40 ft)", laneLeft - 10f, yRange - 2f, paint)
        canvas.drawText("Headpin / Deck (60 ft)", laneLeft - 10f, yDeck - 4f, paint)
    }

    private fun drawSkillTierMatrix(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        shots: List<ShotData>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_NAVY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KEGEL SPECTO SKILL LEVEL MATRIX vs BOWLER CONSISTENCY", left + 12f, top + 18f, paint)

        val th = 16f
        val tableTop = top + 28f
        val headers = listOf("Skill Tier", "Avg Score", "Laydown Tol", "Arrow Tol", "Break Tol", "Entry Tol", "Speed Tol", "RPM Tol")
        val colWidths = floatArrayOf(80f, 64f, 64f, 64f, 64f, 64f, 64f, 64f)

        paint.color = COLOR_MIDNIGHT
        canvas.drawRect(left + 8f, tableTop, left + width - 8f, tableTop + th, paint)

        var curX = left + 12f
        paint.color = Color.WHITE
        paint.textSize = 7.5f
        for (i in headers.indices) {
            canvas.drawText(headers[i], curX, tableTop + 11f, paint)
            curX += colWidths[i]
        }

        val tiers = SpectoSkillTier.values()
        var curY = tableTop + th
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 7.5f

        for (tier in tiers) {
            paint.color = if (tier == SpectoSkillTier.L6_PRO) 0xFFFEF3C7.toInt() else Color.WHITE
            canvas.drawRect(left + 8f, curY, left + width - 8f, curY + th, paint)

            paint.color = COLOR_TEXT_DARK
            curX = left + 12f
            val rowVals = listOf(
                tier.tierName,
                tier.averageScoreRange,
                "±${tier.laydownTolerance} bd",
                "±${tier.arrowTolerance} bd",
                "±${tier.breakpointTolerance} bd",
                "±${tier.entryTolerance} bd",
                "±${tier.launchSpeedTolerance} mph",
                "±${tier.rpmTolerancePct.toInt()}%"
            )
            for (c in rowVals.indices) {
                canvas.drawText(rowVals[c], curX, curY + 11f, paint)
                curX += colWidths[c]
            }
            curY += th
        }

        // Bowler Actual Measured Tolerance Row
        paint.color = COLOR_NAVY
        canvas.drawRect(left + 8f, curY, left + width - 8f, curY + th + 2f, paint)

        val laydownRange = (shots.maxOfOrNull { it.spectoTelemetry.spatial.laydownBoard } ?: 0.0) - (shots.minOfOrNull { it.spectoTelemetry.spatial.laydownBoard } ?: 0.0)
        val arrowRange = (shots.maxOfOrNull { it.spectoTelemetry.spatial.arrowBoard } ?: 0.0) - (shots.minOfOrNull { it.spectoTelemetry.spatial.arrowBoard } ?: 0.0)
        val brkRange = (shots.maxOfOrNull { it.spectoTelemetry.spatial.breakpointBoard } ?: 0.0) - (shots.minOfOrNull { it.spectoTelemetry.spatial.breakpointBoard } ?: 0.0)
        val entryRange = (shots.maxOfOrNull { it.spectoTelemetry.spatial.entryBoard } ?: 0.0) - (shots.minOfOrNull { it.spectoTelemetry.spatial.entryBoard } ?: 0.0)
        val speedRange = (shots.maxOfOrNull { it.spectoTelemetry.speed.launchSpeedMph } ?: 0.0) - (shots.minOfOrNull { it.spectoTelemetry.speed.launchSpeedMph } ?: 0.0)

        paint.color = COLOR_GOLD
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        curX = left + 12f
        val bowlerVals = listOf(
            "ACTUAL (TOL)",
            "PRO L1",
            String.format(Locale.US, "%.1f bd", laydownRange),
            String.format(Locale.US, "%.1f bd", arrowRange),
            String.format(Locale.US, "%.1f bd", brkRange),
            String.format(Locale.US, "%.1f bd", entryRange),
            String.format(Locale.US, "%.1f mph", speedRange),
            "±6%"
        )
        for (c in bowlerVals.indices) {
            canvas.drawText(bowlerVals[c], curX, curY + 12f, paint)
            curX += colWidths[c]
        }
    }

    private fun drawTrendChart(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        title: String,
        yUnit: String,
        data: List<Float>,
        nominalBoard: Float,
        lineColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_NAVY
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, left + 10f, top + 14f, paint)

        if (data.isEmpty()) return

        val minVal = max(0f, (data.minOrNull() ?: nominalBoard) - 3f)
        val maxVal = (data.maxOrNull() ?: nominalBoard) + 3f
        val rangeVal = max(1f, maxVal - minVal)

        val chartLeft = left + 28f
        val chartRight = left + width - 12f
        val chartTop = top + 26f
        val chartBottom = top + height - 20f
        val cW = chartRight - chartLeft
        val cH = chartBottom - chartTop

        // Grid lines
        paint.color = COLOR_BORDER
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(chartLeft, chartTop, chartRight, chartTop, paint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, paint)
        val midY = chartTop + cH / 2f
        canvas.drawLine(chartLeft, midY, chartRight, midY, paint)

        // Nominal target line (dashed/gold)
        paint.color = COLOR_GOLD
        paint.strokeWidth = 1f
        val nomY = chartBottom - ((nominalBoard - minVal) / rangeVal) * cH
        canvas.drawLine(chartLeft, nomY, chartRight, nomY, paint)

        // Plot data
        paint.style = Paint.Style.STROKE
        paint.color = lineColor
        paint.strokeWidth = 1.5f
        val path = Path()
        val stepX = if (data.size > 1) cW / (data.size - 1) else cW

        for (i in data.indices) {
            val x = chartLeft + i * stepX
            val y = chartBottom - ((data[i] - minVal) / rangeVal) * cH
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)

        // Draw points
        paint.style = Paint.Style.FILL
        for (i in data.indices) {
            val x = chartLeft + i * stepX
            val y = chartBottom - ((data[i] - minVal) / rangeVal) * cH
            canvas.drawCircle(x, y, 2f, paint)
        }

        // Y-axis labels
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(String.format(Locale.US, "%.0f %s", maxVal, yUnit), left + 2f, chartTop + 6f, paint)
        canvas.drawText(String.format(Locale.US, "%.0f %s", minVal, yUnit), left + 2f, chartBottom, paint)
        canvas.drawText("Shot 1", chartLeft, top + height - 8f, paint)
        canvas.drawText("Shot ${data.size}", chartRight - 20f, top + height - 8f, paint)
    }

    private fun drawDualTrendChart(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        title: String,
        yUnit: String,
        data1: List<Float>, label1: String, color1: Int,
        data2: List<Float>, label2: String, color2: Int,
        scaleData2: Float = 1f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_NAVY
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, left + 12f, top + 16f, paint)

        // Legend
        paint.textSize = 8f
        paint.color = color1
        canvas.drawText("■ $label1", left + width - 180f, top + 16f, paint)
        paint.color = color2
        canvas.drawText("■ $label2", left + width - 90f, top + 16f, paint)

        if (data1.isEmpty()) return

        val min1 = (data1.minOrNull() ?: 10f) * 0.9f
        val max1 = (data1.maxOrNull() ?: 20f) * 1.1f
        val range1 = max(1f, max1 - min1)

        val chartLeft = left + 36f
        val chartRight = left + width - 16f
        val chartTop = top + 28f
        val chartBottom = top + height - 20f
        val cW = chartRight - chartLeft
        val cH = chartBottom - chartTop

        // Lines for data1
        paint.style = Paint.Style.STROKE
        paint.color = color1
        paint.strokeWidth = 1.6f
        val path1 = Path()
        val stepX = if (data1.size > 1) cW / (data1.size - 1) else cW

        for (i in data1.indices) {
            val x = chartLeft + i * stepX
            val y = chartBottom - ((data1[i] - min1) / range1) * cH
            if (i == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
        }
        canvas.drawPath(path1, paint)

        // Lines for data2
        paint.color = color2
        val path2 = Path()
        for (i in data2.indices) {
            val val2 = data2[i] * scaleData2
            val x = chartLeft + i * stepX
            val y = chartBottom - ((val2 - min1) / range1) * cH
            if (i == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
        }
        canvas.drawPath(path2, paint)

        paint.style = Paint.Style.FILL
        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7f
        canvas.drawText(String.format(Locale.US, "%.1f %s", max1, yUnit), left + 4f, chartTop + 6f, paint)
        canvas.drawText(String.format(Locale.US, "%.1f %s", min1, yUnit), left + 4f, chartBottom, paint)
    }

    private fun drawSpatialSummaryTable(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        shots: List<ShotData>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_NAVY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SPATIAL DISPERSION STATISTICAL BREAKDOWN", left + 12f, top + 20f, paint)

        val th = 20f
        val tableTop = top + 34f
        val headers = listOf("Parameter", "Nominal Target", "Average", "Minimum", "Maximum", "Range (Tolerance)", "Std Dev")
        val colWidths = floatArrayOf(120f, 75f, 70f, 65f, 65f, 95f, 70f)

        paint.color = COLOR_NAVY
        canvas.drawRect(left + 8f, tableTop, left + width - 8f, tableTop + th, paint)

        var curX = left + 12f
        paint.color = Color.WHITE
        paint.textSize = 8f
        for (i in headers.indices) {
            canvas.drawText(headers[i], curX, tableTop + 14f, paint)
            curX += colWidths[i]
        }

        val rows = listOf(
            computeStatsRow("Laydown Board (0 ft)", 24.0, shots.map { it.spectoTelemetry.spatial.laydownBoard }),
            computeStatsRow("Arrow Board (15 ft)", 16.0, shots.map { it.spectoTelemetry.spatial.arrowBoard }),
            computeStatsRow("Breakpoint Board", 7.0, shots.map { it.spectoTelemetry.spatial.breakpointBoard }),
            computeStatsRow("Breakpoint Distance (ft)", 42.0, shots.map { it.spectoTelemetry.spatial.breakpointDistanceFt }),
            computeStatsRow("Pocket Entry Board (60 ft)", 17.0, shots.map { it.spectoTelemetry.spatial.entryBoard }),
            computeStatsRow("Pin Deck Exit Board", 20.0, shots.map { it.spectoTelemetry.spatial.pinDeckExitBoard }),
            computeStatsRow("Pin Deck Deflection (bds)", 3.2, shots.map { it.spectoTelemetry.spatial.pinDeckDeflection }),
            computeStatsRow("Launch Angle (deg)", -2.8, shots.map { it.spectoTelemetry.angles.launchAngleDeg }),
            computeStatsRow("Impact Angle (deg)", 5.2, shots.map { it.spectoTelemetry.angles.impactAngleDeg })
        )

        var curY = tableTop + th
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT

        for (r in rows.indices) {
            paint.color = if (r % 2 == 0) Color.WHITE else 0xFFF1F5F9.toInt()
            canvas.drawRect(left + 8f, curY, left + width - 8f, curY + th, paint)

            paint.color = COLOR_TEXT_DARK
            curX = left + 12f
            for (c in rows[r].indices) {
                canvas.drawText(rows[r][c], curX, curY + 14f, paint)
                curX += colWidths[c]
            }
            curY += th
        }
    }

    private fun drawKinematicsSummarySection(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        bowler: BowlerProfile,
        shots: List<ShotData>
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, left + width, top + height, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = COLOR_NAVY
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BIOMECHANICAL KINEMATICS & BODY LEVERAGE SUMMARY", left + 12f, top + 20f, paint)

        val th = 20f
        val tableTop = top + 34f
        val headers = listOf("Joint / Angle Metric", "Benchmark / Goal", "Session Avg", "Min", "Max", "Tolerance Range", "Status")
        val colWidths = floatArrayOf(130f, 85f, 75f, 65f, 65f, 85f, 55f)

        paint.color = COLOR_NAVY
        canvas.drawRect(left + 8f, tableTop, left + width - 8f, tableTop + th, paint)

        var curX = left + 12f
        paint.color = Color.WHITE
        paint.textSize = 8f
        for (i in headers.indices) {
            canvas.drawText(headers[i], curX, tableTop + 14f, paint)
            curX += colWidths[i]
        }

        val rows = listOf(
            computeStatsRowWithBenchmark("Spine Lateral Tilt @ Release", "18.0°", shots.map { it.kinematics.spineLateralTiltDeg }, "OPTIMAL"),
            computeStatsRowWithBenchmark("Forward Trunk Tilt", "32.0°", shots.map { it.kinematics.forwardTiltDeg }, "STABLE"),
            computeStatsRowWithBenchmark("Slide Knee Flexion", "48.0°", shots.map { it.kinematics.kneeFlexionDeg }, "EXCELLENT"),
            computeStatsRowWithBenchmark("Shoulder-Hip Separation", "24.0°", shots.map { it.kinematics.shoulderHipSeparationDeg }, "GOOD"),
            computeStatsRowWithBenchmark("Slide Foot Board", "21.0 bd", shots.map { it.kinematics.slideBoard }, "STABLE"),
            computeStatsRowWithBenchmark("Foot Drift (Boards)", "-1.0 bd", shots.map { it.kinematics.driftBoards }, "CONSISTENT")
        )

        var curY = tableTop + th
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT

        for (r in rows.indices) {
            paint.color = if (r % 2 == 0) Color.WHITE else 0xFFF1F5F9.toInt()
            canvas.drawRect(left + 8f, curY, left + width - 8f, curY + th, paint)

            paint.color = COLOR_TEXT_DARK
            curX = left + 12f
            for (c in rows[r].indices) {
                if (c == 6) {
                    paint.color = COLOR_SUCCESS
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                } else {
                    paint.color = COLOR_TEXT_DARK
                    paint.typeface = Typeface.DEFAULT
                }
                canvas.drawText(rows[r][c], curX, curY + 14f, paint)
                curX += colWidths[c]
            }
            curY += th
        }

        // Coaching Notes Box
        val notesTop = curY + 16f
        paint.color = COLOR_MIDNIGHT
        canvas.drawRoundRect(left + 8f, notesTop, left + width - 8f, notesTop + 65f, 4f, 4f, paint)

        paint.color = COLOR_GOLD
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("COACH ALFREDO'S PRESCRIPTION & OBSERVATIONS:", left + 16f, notesTop + 16f, paint)

        paint.color = Color.WHITE
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 8f
        val note1 = bowler.notes.ifBlank { "Excellent rev rate and launch angle synchronization. Continue holding slide knee flexion past release." }
        val goalText = "Target Focus: ${bowler.primaryGoal}"
        canvas.drawText(note1, left + 16f, notesTop + 32f, paint)
        canvas.drawText(goalText, left + 16f, notesTop + 48f, paint)
    }

    private fun drawMetricScoreCard(
        canvas: Canvas,
        left: Float, top: Float, right: Float, bottom: Float,
        title: String, value: String, subtext: String, accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_CARD_BG
        canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, paint)
        paint.color = COLOR_BORDER
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Top Accent Bar
        paint.color = accentColor
        canvas.drawRoundRect(left, top, right, top + 4f, 2f, 2f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, left + 8f, top + 18f, paint)

        paint.color = COLOR_TEXT_DARK
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(value, left + 8f, top + 42f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 7f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(subtext, left + 8f, top + 58f, paint)
    }

    private fun drawSectionHeader(canvas: Canvas, sectionTitle: String, bowler: BowlerProfile) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = COLOR_NAVY
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 56f, paint)
        paint.color = COLOR_GOLD
        canvas.drawRect(0f, 56f, PAGE_WIDTH.toFloat(), 60f, paint)

        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("COACH'S EYE BOWLING LAB", 24f, 26f, paint)

        paint.textSize = 8.5f
        paint.color = COLOR_GOLD
        paint.typeface = Typeface.DEFAULT
        canvas.drawText(sectionTitle, 24f, 44f, paint)

        paint.color = Color.WHITE
        canvas.drawText("Athlete: ${bowler.name} (${bowler.id})", PAGE_WIDTH - 200f, 26f, paint)
        canvas.drawText("Western Pacific Bowling Training Center", PAGE_WIDTH - 200f, 44f, paint)
    }

    private fun drawPageFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = COLOR_BORDER
        canvas.drawLine(24f, PAGE_HEIGHT - 28f, PAGE_WIDTH - 24f, PAGE_HEIGHT - 28f, paint)

        paint.textSize = 7.5f
        paint.color = COLOR_TEXT_MUTED
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Coach's Eye Bowling Lab Track System  •  cebowlinglab.com", 24f, PAGE_HEIGHT - 14f, paint)
        canvas.drawText("Page $pageNum of $totalPages", PAGE_WIDTH - 80f, PAGE_HEIGHT - 14f, paint)
    }

    private fun computeStatsRow(paramName: String, nominal: Double, vals: List<Double>): List<String> {
        if (vals.isEmpty()) return listOf(paramName, "$nominal", "-", "-", "-", "-", "-")
        val avg = vals.average()
        val minV = vals.minOrNull() ?: 0.0
        val maxV = vals.maxOrNull() ?: 0.0
        val range = maxV - minV
        val variance = vals.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)

        return listOf(
            paramName,
            String.format(Locale.US, "%.1f", nominal),
            String.format(Locale.US, "%.1f", avg),
            String.format(Locale.US, "%.1f", minV),
            String.format(Locale.US, "%.1f", maxV),
            String.format(Locale.US, "±%.1f", range / 2.0),
            String.format(Locale.US, "%.2f", stdDev)
        )
    }

    private fun computeStatsRowWithBenchmark(paramName: String, benchmark: String, vals: List<Double>, status: String): List<String> {
        if (vals.isEmpty()) return listOf(paramName, benchmark, "-", "-", "-", "-", status)
        val avg = vals.average()
        val minV = vals.minOrNull() ?: 0.0
        val maxV = vals.maxOrNull() ?: 0.0
        val range = maxV - minV

        return listOf(
            paramName,
            benchmark,
            String.format(Locale.US, "%.1f", avg),
            String.format(Locale.US, "%.1f", minV),
            String.format(Locale.US, "%.1f", maxV),
            String.format(Locale.US, "%.1f", range),
            status
        )
    }

    private fun generatePlaceholderShots(bowler: BowlerProfile): List<ShotData> {
        val list = mutableListOf<ShotData>()
        for (i in 1..24) {
            val shot = com.example.cebowlinglabtrack.domain.ml.SimulatedShotGenerator.generateShot(
                shotNumber = i,
                bowlerId = bowler.id
            )
            list.add(shot)
        }
        return list
    }
}