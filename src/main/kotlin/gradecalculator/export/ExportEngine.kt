package gradecalculator.export

import gradecalculator.model.GradeCalculator
import gradecalculator.model.CumulativeGPACalculator
import gradecalculator.model.Student
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * ExportEngine
 * Handles exporting grade data to multiple formats.
 * Demonstrates: Single Responsibility, reusable across all calculator types.
 */
object ExportEngine {

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

    // ─── EXCEL ────────────────────────────────────────────────────────

    fun toExcel(calculators: List<GradeCalculator>, outputPath: String, student: Student? = null): File {
        val workbook = XSSFWorkbook()

        // ── Student Info sheet (if student provided) ──
        if (student != null) {
            val infoSheet = workbook.createSheet("Student Info")
            val infoStyle = workbook.createCellStyle().apply {
                val font = workbook.createFont()
                font.bold = true
                setFont(font)
            }

            var rowIdx = 0
            val nameRow = infoSheet.createRow(rowIdx++)
            nameRow.createCell(0).setCellValue("Student Name:")
            nameRow.createCell(1).setCellValue(student.name)
            nameRow.getCell(0).cellStyle = infoStyle

            val idRow = infoSheet.createRow(rowIdx++)
            idRow.createCell(0).setCellValue("Matricule:")
            idRow.createCell(1).setCellValue(student.matricule)
            idRow.getCell(0).cellStyle = infoStyle

            val majorRow = infoSheet.createRow(rowIdx++)
            majorRow.createCell(0).setCellValue("Major:")
            majorRow.createCell(1).setCellValue(student.major.displayName)
            majorRow.getCell(0).cellStyle = infoStyle

            val generatedRow = infoSheet.createRow(rowIdx++)
            generatedRow.createCell(0).setCellValue("Generated:")
            generatedRow.createCell(1).setCellValue(timestamp())
            generatedRow.getCell(0).cellStyle = infoStyle

            (0..1).forEach { infoSheet.autoSizeColumn(it) }
        }

        // ── Summary sheet ──
        val summarySheet = workbook.createSheet("Summary")
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_BLUE.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.color = IndexedColors.WHITE.index
            font.bold = true
            setFont(font)
        }

        val headers = listOf("Course", "Type", "Credits", "Grade %", "Letter", "GPA Pts")
        val headerRow = summarySheet.createRow(0)
        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        calculators.forEachIndexed { idx, calc ->
            val row = summarySheet.createRow(idx + 1)
            calc.toExcelRow().forEachIndexed { i, v ->
                row.createCell(i).setCellValue(v)
            }
        }

        // Auto-size columns
        headers.indices.forEach { summarySheet.autoSizeColumn(it) }

        // ── Per-course sheets ──
        for (calc in calculators) {
            val sheet = workbook.createSheet(calc.name.take(30))
            val titleRow = sheet.createRow(0)
            titleRow.createCell(0).setCellValue("${calc.name} — ${calc.getCalculatorType()}")

            val catHeaderRow = sheet.createRow(2)
            listOf("Category", "Weight %", "Drop Lowest", "Average").forEachIndexed { i, h ->
                catHeaderRow.createCell(i).setCellValue(h)
            }

            var rowIdx = 3
            for (cat in calc.getCategories()) {
                val catRow = sheet.createRow(rowIdx++)
                catRow.createCell(0).setCellValue(cat.name)
                catRow.createCell(1).setCellValue(cat.weight)
                catRow.createCell(2).setCellValue(if (cat.dropLowest) "Yes" else "No")
                catRow.createCell(3).setCellValue(cat.getEffectiveAverage()?.let { "%.1f%%".format(it) } ?: "N/A")

                for (grade in cat.grades) {
                    val gradeRow = sheet.createRow(rowIdx++)
                    gradeRow.createCell(1).setCellValue("  └ ${grade.name}")
                    gradeRow.createCell(2).setCellValue(grade.score?.toString() ?: "—")
                    gradeRow.createCell(3).setCellValue("/ ${grade.total}")
                }
            }

            val resultRow = sheet.createRow(rowIdx + 1)
            resultRow.createCell(0).setCellValue("FINAL GRADE")
            resultRow.createCell(1).setCellValue("%.2f%%".format(calc.calculateGrade()))
            resultRow.createCell(2).setCellValue(calc.getLetterGrade())
            resultRow.createCell(3).setCellValue("GPA: ${calc.getGPA()}")

            (0..3).forEach { sheet.autoSizeColumn(it) }
        }

        val file = File(outputPath)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    // ─── BATCH EXCEL (Teacher Export) ──────────────────────────────────

    fun toExcelBatch(results: List<gradecalculator.model.StudentResult>, outputPath: String): File {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Student Grades")

        // Get all unique courses
        val allCourses = results.flatMap { it.courses }.distinctBy { it.courseName }
        
        // Header style
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.DARK_GREEN.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            val font = workbook.createFont()
            font.color = IndexedColors.WHITE.index
            font.bold = true
            setFont(font)
        }

        // Create headers
        val headerRow = sheet.createRow(0)
        val headers = mutableListOf("Student Name", "Matricule", "Major")
        headers.addAll(allCourses.map { it.courseName })
        headers.addAll(listOf("GPA", "Distinction"))

        headers.forEachIndexed { i, h ->
            headerRow.createCell(i).apply {
                setCellValue(h)
                cellStyle = headerStyle
            }
        }

        // Fill data rows
        var rowIdx = 1
        for (result in results) {
            val row = sheet.createRow(rowIdx++)
            var colIdx = 0

            // Student info
            row.createCell(colIdx++).setCellValue(result.student.name)
            row.createCell(colIdx++).setCellValue(result.student.matricule)
            row.createCell(colIdx++).setCellValue(result.student.major.displayName)

            // Grades for each course
            for (course in allCourses) {
                val gradeIdx = result.courses.indexOfFirst { it.courseName == course.courseName }
                val gradeValue = if (gradeIdx >= 0 && gradeIdx < result.grades.size) {
                    result.grades[gradeIdx]?.let { "%.1f%%".format(it) } ?: "—"
                } else {
                    "—"
                }
                row.createCell(colIdx++).setCellValue(gradeValue)
            }

            // GPA and distinction
            row.createCell(colIdx++).setCellValue("%.2f".format(result.cumulativeGPA))
            row.createCell(colIdx).setCellValue(result.distinction)
        }

        // Auto-size columns
        headers.indices.forEach { sheet.autoSizeColumn(it) }

        val file = File(outputPath)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()
        return file
    }

    // ─── PDF (using iText) ────────────────────────────────────────────

    fun toPdf(calculators: List<GradeCalculator>, outputPath: String, student: Student? = null): File {
        val file = File(outputPath)

        // iText 7 PDF generation
        val writer = com.itextpdf.kernel.pdf.PdfWriter(outputPath)
        val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
        val document = com.itextpdf.layout.Document(pdf)

        val titleFont = com.itextpdf.kernel.font.PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)
        val bodyFont = com.itextpdf.kernel.font.PdfFontFactory.createFont(
            com.itextpdf.io.font.constants.StandardFonts.HELVETICA)

        // Title
        document.add(
            com.itextpdf.layout.element.Paragraph("Grade Calculator Report")
                .setFont(titleFont).setFontSize(22f)
                .setMarginBottom(4f)
        )

        // Student Info (if provided)
        if (student != null) {
            document.add(
                com.itextpdf.layout.element.Paragraph("Student: ${student.name}")
                    .setFont(titleFont).setFontSize(16f)
                    .setMarginBottom(2f)
            )
            document.add(
                com.itextpdf.layout.element.Paragraph("ID: ${student.matricule} | Major: ${student.major.displayName}")
                    .setFont(bodyFont).setFontSize(12f)
                    .setMarginBottom(8f)
            )
        }

        document.add(
            com.itextpdf.layout.element.Paragraph("Generated: ${timestamp()}")
                .setFont(bodyFont).setFontSize(10f).setFontColor(
                    com.itextpdf.kernel.colors.ColorConstants.GRAY)
                .setMarginBottom(20f)
        )

        // Summary table
        val table = com.itextpdf.layout.element.Table(6).useAllAvailableWidth()
        listOf("Course", "Type", "Credits", "Grade %", "Letter", "GPA").forEach { h ->
            table.addHeaderCell(
                com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph(h).setFont(titleFont).setFontSize(10f)
                ).setBackgroundColor(com.itextpdf.kernel.colors.DeviceRgb(30, 58, 138))
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
            )
        }

        for (calc in calculators) {
            val pct = calc.calculateGrade()
            val gradeColor = when {
                pct >= 90 -> com.itextpdf.kernel.colors.DeviceRgb(34, 197, 94)
                pct >= 80 -> com.itextpdf.kernel.colors.DeviceRgb(59, 130, 246)
                pct >= 70 -> com.itextpdf.kernel.colors.DeviceRgb(245, 158, 11)
                else -> com.itextpdf.kernel.colors.DeviceRgb(239, 68, 68)
            }
            listOf(calc.name, calc.getCalculatorType(), calc.credits.toString(),
                "%.2f%%".format(pct), calc.getLetterGrade(), calc.getGPA().toString()
            ).forEachIndexed { i, v ->
                val cell = com.itextpdf.layout.element.Cell().add(
                    com.itextpdf.layout.element.Paragraph(v).setFont(bodyFont).setFontSize(10f)
                )
                if (i == 4) cell.setFontColor(gradeColor)
                table.addCell(cell)
            }
        }
        document.add(table)

        // Per-course details
        for (calc in calculators) {
            document.add(com.itextpdf.layout.element.AreaBreak())
            document.add(
                com.itextpdf.layout.element.Paragraph(calc.name)
                    .setFont(titleFont).setFontSize(16f).setMarginBottom(4f)
            )
            document.add(
                com.itextpdf.layout.element.Paragraph(calc.getDescription())
                    .setFont(bodyFont).setFontSize(10f)
                    .setFontColor(com.itextpdf.kernel.colors.ColorConstants.GRAY)
                    .setMarginBottom(12f)
            )

            for (cat in calc.getCategories()) {
                document.add(
                    com.itextpdf.layout.element.Paragraph("${cat.name} (${cat.weight}%)")
                        .setFont(titleFont).setFontSize(12f).setMarginBottom(4f)
                )
                val catTable = com.itextpdf.layout.element.Table(3).useAllAvailableWidth()
                listOf("Assignment", "Score", "/ Total").forEach { h ->
                    catTable.addHeaderCell(com.itextpdf.layout.element.Cell().add(
                        com.itextpdf.layout.element.Paragraph(h).setFont(titleFont).setFontSize(9f)))
                }
                for (g in cat.grades) {
                    catTable.addCell(g.name)
                    catTable.addCell(g.score?.toString() ?: "—")
                    catTable.addCell(g.total.toString())
                }
                document.add(catTable)
                val avg = cat.getEffectiveAverage()
                document.add(
                    com.itextpdf.layout.element.Paragraph(
                        "Average: ${if (avg != null) "%.1f%%".format(avg) else "N/A"}" +
                        if (cat.dropLowest) " (lowest dropped)" else ""
                    ).setFont(bodyFont).setFontSize(10f).setMarginBottom(10f)
                )
            }

            document.add(
                com.itextpdf.layout.element.Paragraph(
                    "Final Grade: ${"%.2f%%".format(calc.calculateGrade())} — ${calc.getLetterGrade()} (${calc.getGPA()} GPA)"
                ).setFont(titleFont).setFontSize(12f)
            )
        }

        document.close()
        return file
    }

    // ─── HTML ─────────────────────────────────────────────────────────

    fun toHtml(calculators: List<GradeCalculator>, outputPath: String, student: Student? = null): File {
        val html = buildString {
            appendLine("""<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>Grade Report</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Segoe UI', sans-serif; background: #f8f9fa; color: #111827; padding: 40px; }
  h1 { font-size: 32px; color: #2563eb; margin-bottom: 8px; }
  .subtitle { color: #6b7280; font-size: 14px; margin-bottom: 32px; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 32px; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
  th { background: #2563eb; color: white; padding: 12px 16px; text-align: left; font-size: 13px; }
  td { padding: 10px 16px; border-bottom: 1px solid #e5e7eb; font-size: 14px; }
  tr:hover td { background: #f3f4f6; }
  .A { color: #16a34a; font-weight: 700; }
  .B { color: #2563eb; font-weight: 700; }
  .C { color: #f59e0b; font-weight: 700; }
  .D { color: #f97316; font-weight: 700; }
  .F { color: #dc2626; font-weight: 700; }
  .section { background: white; border-radius: 12px; padding: 24px; margin-bottom: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e5e7eb; }
  .section h2 { color: #2563eb; margin-bottom: 16px; font-size: 18px; }
  .cat-header { background: #f3f4f6; border-radius: 8px; padding: 10px 14px; margin: 12px 0 6px; border: 1px solid #e5e7eb; }
  .cat-name { font-weight: 600; color: #111827; }
  .cat-weight { color: #6b7280; font-size: 12px; margin-left: 8px; }
  footer { color: #6b7280; font-size: 12px; margin-top: 40px; text-align: center; }
  .student-info { background: white; border-radius: 12px; padding: 20px; margin-bottom: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); border: 1px solid #e5e7eb; }
  .student-info h2 { color: #2563eb; margin-bottom: 12px; font-size: 20px; }
  .info-row { display: flex; margin-bottom: 8px; }
  .info-label { font-weight: 600; width: 120px; color: #6b7280; }
  .info-value { color: #111827; }
</style>
</head>
<body>
<h1>📊 Grade Calculator Report</h1>""")

            // Student Info Section
            if (student != null) {
                appendLine("""<div class="student-info">
  <h2>👤 Student Information</h2>
  <div class="info-row">
    <span class="info-label">Name:</span>
    <span class="info-value">${student.name}</span>
  </div>
  <div class="info-row">
    <span class="info-label">Matricule:</span>
    <span class="info-value">${student.matricule}</span>
  </div>
  <div class="info-row">
    <span class="info-label">Major:</span>
    <span class="info-value">${student.major.displayName}</span>
  </div>
</div>""")
            }

            appendLine("""<p class="subtitle">Generated: ${timestamp()} &nbsp;|&nbsp; ${calculators.size} course(s)</p>

<table>
<thead><tr><th>Course</th><th>Type</th><th>Credits</th><th>Grade %</th><th>Letter</th><th>GPA Pts</th></tr></thead>
<tbody>""")
            for (calc in calculators) {
                val letter = calc.getLetterGrade()
                val letterClass = letter.firstOrNull()?.toString() ?: "F"
                appendLine("<tr><td>${calc.name}</td><td>${calc.getCalculatorType()}</td>" +
                    "<td>${calc.credits}</td><td>${"%.2f".format(calc.calculateGrade())}%</td>" +
                    "<td class=\"$letterClass\">$letter</td><td>${calc.getGPA()}</td></tr>")
            }
            appendLine("</tbody></table>")

            for (calc in calculators) {
                appendLine("""<div class="section">
  <h2>${calc.name}</h2>
  <p style="color:#64748b;font-size:13px;margin-bottom:16px">${calc.getDescription()}</p>""")
                for (cat in calc.getCategories()) {
                    val avg = cat.getEffectiveAverage()
                    appendLine("""  <div class="cat-header">
    <span class="cat-name">${cat.name}</span>
    <span class="cat-weight">Weight: ${cat.weight}%${if (cat.dropLowest) " · Drop Lowest" else ""}</span>
    <span style="float:right;color:#a5b4fc">${if (avg != null) "%.1f%%".format(avg) else "N/A"}</span>
  </div>
  <table>
  <thead><tr><th>Assignment</th><th>Score</th><th>Total</th><th>%</th></tr></thead>
  <tbody>""")
                    for (g in cat.grades) {
                        val pct = g.score?.div(g.total)?.times(100)
                        appendLine("  <tr><td>${g.name}</td><td>${g.score ?: "—"}</td>" +
                            "<td>${g.total}</td><td>${if (pct != null) "%.1f%%".format(pct) else "—"}</td></tr>")
                    }
                    appendLine("  </tbody></table>")
                }
                val letter = calc.getLetterGrade()
                val lc = letter.firstOrNull()?.toString() ?: "F"
                appendLine("""  <p style="margin-top:16px;font-size:16px;font-weight:600">
    Final: <span class="$lc">${"%.2f".format(calc.calculateGrade())}% — $letter</span>
    &nbsp;|&nbsp; GPA: ${calc.getGPA()}
  </p>
</div>""")
            }
            appendLine("<footer>GradeOS Kotlin Desktop App · ${timestamp()}</footer>")
            append("</body></html>")
        }

        val file = File(outputPath)
        file.writeText(html)
        return file
    }

    // ─── XML ──────────────────────────────────────────────────────────

    fun toXml(calculators: List<GradeCalculator>, outputPath: String): File {
        val xml = buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<gradeReport generated="${timestamp()}" courses="${calculators.size}">""")
            for (calc in calculators) {
                appendLine(calc.toXmlElement())
            }
            append("</gradeReport>")
        }
        val file = File(outputPath)
        file.writeText(xml)
        return file
    }

    // ─── CSV ──────────────────────────────────────────────────────────

    fun toCsv(calculators: List<GradeCalculator>, outputPath: String): File {
        val csv = buildString {
            appendLine("Course,Type,Credits,Grade %,Letter,GPA Pts")
            for (calc in calculators) {
                appendLine(calc.toCsvRow())
            }
        }
        val file = File(outputPath)
        file.writeText(csv)
        return file
    }
}
