package com.studylock.app.feature.schedule

import android.graphics.Rect
import com.studylock.app.data.entity.Course
import com.studylock.app.ui.util.CourseColors

data class OcrCourseCandidate(
    var name: String = "",
    var teacher: String = "",
    var location: String = "",
    var weekday: Int = 1,
    var startSection: Int = 1,
    var endSection: Int = 2,
    var startWeek: Int = 1,
    var endWeek: Int = 16,
    var weekType: Int = 0,
    var colorIndex: Int = 0
) {
    fun toCourse(semesterId: Long): Course = Course(
        name = name.take(20).trim(),
        teacher = teacher.ifBlank { null },
        location = location.ifBlank { null },
        weekday = weekday.coerceIn(1, 7),
        startSection = startSection.coerceIn(1, 14),
        endSection = endSection.coerceIn(startSection, 14),
        startWeek = startWeek.coerceIn(1, 25),
        endWeek = endWeek.coerceIn(startWeek, 25),
        weekType = weekType.coerceIn(0, 2),
        colorTag = CourseColors.getHexFromIndex(colorIndex),
        semesterId = semesterId
    )
}

data class OcrTextLine(val text: String, val boundingBox: Rect?)

object CourseOcrParser {

    // Full weekday keywords: long form first for better matching
    private val WEEKDAY_KW = listOf(
        "星期一" to 1, "星期二" to 2, "星期三" to 3, "星期四" to 4,
        "星期五" to 5, "星期六" to 6, "星期日" to 7, "周天" to 7,
        "周一" to 1, "周二" to 2, "周三" to 3, "周四" to 4,
        "周五" to 5, "周六" to 6, "周日" to 7
    )
    private val WDAY_CHAR = mapOf(
        '一' to 1, '二' to 2, '三' to 3, '四' to 4, '五' to 5, '六' to 6, '日' to 7, '天' to 7
    )

    // Flexible regexes: optional prefixes/suffixes
    private val RE_SECTION = Regex("""第?\s*(\d{1,2})\s*[-~至到]\s*(\d{1,2})\s*节?""")
    private val RE_WEEK = Regex("""第?\s*(\d{1,2})\s*[-~至到]\s*(\d{1,2})\s*周?""")
    private val RE_WDAY = Regex("""周([一二三四五六日天])""")
    private val RE_BARE_RANGE = Regex("""^(\d{1,2})\s*[-~至到]\s*(\d{1,2})$""")

    private val LOC_KW = listOf("楼", "教", "室", "馆", "场", "堂", "厅", "院", "栋", "层", "区", "实", "训", "综")
    private val TEACHER_KW = listOf("老师", "教授", "讲师", "教师", "导员")

    // ---- PUBLIC API ----

    fun parse(text: String): List<OcrCourseCandidate> {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        return postProcess(tryFlexibleParse(lines))
    }

    fun parseFromStructuredLines(ocrLines: List<OcrTextLine>): List<OcrCourseCandidate> {
        val withBox = ocrLines.filter { it.boundingBox != null }
        val result = if (withBox.size >= 6) tryPixelTableParse(withBox)
            .ifEmpty { tryFlexibleParse(ocrLines.map { it.text }) }
        else tryFlexibleParse(ocrLines.map { it.text })
        return postProcess(result)
    }

    // ---- PIXEL TABLE PARSE ----

    private data class Lbl(val text: String, val cx: Int, val cy: Int, val h: Int)
    private data class Col(val wday: Int, val xMin: Int, val xMax: Int)

    private fun tryPixelTableParse(lines: List<OcrTextLine>): List<OcrCourseCandidate> {
        val lbls = lines.mapNotNull { l ->
            l.boundingBox?.let { Lbl(l.text.trim(), it.centerX(), it.centerY(), it.height()) }
        }.filter { it.text.isNotBlank() }
        if (lbls.size < 6) return emptyList()

        val cols = detectColumns(lbls) ?: return emptyList()
        if (cols.size < 3) return emptyList()

        val body = lbls.filter { !isHeaderRow(it.text) }
        if (body.isEmpty()) return emptyList()

        val rows = clusterRows(body)
        val cells = mutableMapOf<Pair<Int, Int>, StringBuilder>()

        for ((ri, row) in rows.withIndex()) {
            for (line in row) {
                val col = cols.firstOrNull { line.cx in it.xMin..it.xMax } ?: continue
                cells.getOrPut(ri to col.wday) { StringBuilder() }.append(line.text).append(" ")
            }
        }

        return cells.mapNotNull { (key, sb) ->
            val txt = sb.toString().trim().replace(Regex("""\s+"""), " ")
            if (txt.length < 2) return@mapNotNull null
            val c = parseCell(txt, key.second)
            if (c.name.isBlank()) null else c
        }.distinctBy { Triple(it.name, it.weekday, it.startSection) }
    }

    private fun detectColumns(lbls: List<Lbl>): List<Col>? {
        val headerRows = lbls.filter { isHeaderRow(it.text) }
        if (headerRows.isEmpty()) return null

        val hits = mutableListOf<Pair<Int, Int>>()
        for (hr in headerRows) {
            for ((kw, day) in WEEKDAY_KW) {
                var idx = hr.text.indexOf(kw)
                while (idx >= 0) {
                    // Estimate X pixel from char position within line's bounding box
                    val estX = hr.cx + (idx - hr.text.length / 2) * 2
                    hits.add(estX to day)
                    idx = hr.text.indexOf(kw, idx + 1)
                }
            }
        }
        val unique = hits.distinctBy { it.second }.sortedBy { it.first }
        if (unique.size < 3) return null

        val cols = mutableListOf<Col>()
        for (i in unique.indices) {
            val (cx, day) = unique[i]
            val left = if (i > 0) (unique[i-1].first + cx) / 2 else Int.MIN_VALUE
            val right = if (i < unique.size - 1) (cx + unique[i+1].first) / 2 else Int.MAX_VALUE
            cols.add(Col(day, left, right))
        }
        return cols
    }

    private fun isHeaderRow(text: String): Boolean =
        WEEKDAY_KW.count { (kw, _) -> text.contains(kw) } >= 3

    private fun clusterRows(lines: List<Lbl>): List<List<Lbl>> {
        if (lines.size <= 1) return listOf(lines)
        val sorted = lines.sortedBy { it.cy }
        val medianH = sorted.map { it.h }.sorted().let { it[it.size / 2] }.coerceAtLeast(20)
        val thresh = (medianH * 1.5f).toInt()

        val rows = mutableListOf<MutableList<Lbl>>()
        var cur = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            if (kotlin.math.abs(sorted[i].cy - cur.last().cy) < thresh) cur.add(sorted[i])
            else { rows.add(cur); cur = mutableListOf(sorted[i]) }
        }
        rows.add(cur)
        return rows
    }

    // ---- FLEXIBLE TEXT PARSE ----

    private fun tryFlexibleParse(lines: List<String>): List<OcrCourseCandidate> {
        // Try list format first: each course on one or few lines
        val listResult = parseListFormat(lines)
        if (listResult.size >= 2) return listResult

        // Fall back to streaming parse
        return parseStreaming(lines)
    }

    private fun parseListFormat(lines: List<String>): List<OcrCourseCandidate> {
        val result = mutableListOf<OcrCourseCandidate>()
        var cur = OcrCourseCandidate()
        var hasAny = false

        for (line in lines) {
            if (isHeaderRow(line)) continue
            val wday = findWday(line)
            val sec = findSection(line)
            val wk = findWeek(line)

            if (wday != null || sec != null) {
                if (hasAny && cur.name.isNotBlank()) result.add(cur)
                cur = OcrCourseCandidate(
                    weekday = wday ?: cur.weekday,
                    startSection = sec?.first ?: cur.startSection,
                    endSection = sec?.second ?: cur.endSection,
                    startWeek = wk?.first ?: cur.startWeek,
                    endWeek = wk?.second ?: cur.endWeek
                )
                hasAny = true
            }

            if (hasAny) {
                if (cur.name.isBlank() && isCourseName(line)) cur.name = line.take(20)
                if (cur.location.isBlank() && isLocation(line)) cur.location = line
                if (cur.teacher.isBlank() && isTeacher(line)) cur.teacher = line
                wk?.let { cur.startWeek = it.first; cur.endWeek = it.second }
                sec?.let { cur.startSection = it.first; cur.endSection = it.second }
            } else if (isCourseName(line)) {
                cur.name = line.take(20); hasAny = true
            }
        }
        if (hasAny && cur.name.isNotBlank()) result.add(cur)
        return result.filter { it.name.isNotBlank() }
    }

    private fun parseStreaming(lines: List<String>): List<OcrCourseCandidate> {
        val result = mutableListOf<OcrCourseCandidate>()
        var cur = OcrCourseCandidate()
        var active = false
        for (line in lines) {
            if (isHeaderRow(line)) continue
            val wday = findWday(line)
            val sec = findSection(line)
            val wk = findWeek(line)

            if (wday != null || sec != null) {
                if (active && cur.name.isNotBlank()) result.add(cur)
                cur = OcrCourseCandidate(weekday = wday ?: 1)
                active = true
            }
            if (active && isCourseName(line) && cur.name.isBlank()) cur.name = line.take(20)
            if (active && isLocation(line) && cur.location.isBlank()) cur.location = line
            if (active && isTeacher(line) && cur.teacher.isBlank()) cur.teacher = line
            wk?.let { cur.startWeek = it.first; cur.endWeek = it.second }
            sec?.let { cur.startSection = it.first; cur.endSection = it.second }
            if (sec == null && wday == null && wk == null) {
                // maybe continuation of previous course info
                if (active && cur.name.isNotBlank()) {
                    if (cur.location.isBlank() && isLocation(line)) cur.location = line
                    if (cur.teacher.isBlank() && isTeacher(line)) cur.teacher = line
                }
            }
        }
        if (active && cur.name.isNotBlank()) result.add(cur)
        return result.filter { it.name.isNotBlank() }
    }

    // ---- CELL PARSING ----

    private fun parseCell(text: String, weekday: Int): OcrCourseCandidate {
        val c = OcrCourseCandidate(weekday = weekday)
        val parts = text.split(Regex("""\s+""")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return c

        c.name = parts.firstOrNull { isCourseName(it) } ?: parts.first().take(20)
        for (p in parts) {
            if (c.location.isBlank() && isLocation(p)) c.location = p
            if (c.teacher.isBlank() && isTeacher(p)) c.teacher = p
            RE_SECTION.find(p)?.destructured?.let { (s, e) ->
                c.startSection = s.toIntOrNull()?.coerceIn(1, 14) ?: c.startSection
                c.endSection = e.toIntOrNull()?.coerceIn(1, 14) ?: c.endSection
            }
            RE_WEEK.find(p)?.destructured?.let { (s, e) ->
                c.startWeek = s.toIntOrNull()?.coerceIn(1, 25) ?: c.startWeek
                c.endWeek = e.toIntOrNull()?.coerceIn(1, 25) ?: c.endWeek
            }
            RE_BARE_RANGE.find(p)?.destructured?.let { (s, e) ->
                if (c.startSection == 1 && c.endSection == 2) {
                    val si = s.toIntOrNull() ?: 0; val ei = e.toIntOrNull() ?: 0
                    if (si in 1..14 && ei in si..14) { c.startSection = si; c.endSection = ei }
                }
            }
        }
        return c
    }

    // ---- HELPERS ----

    private fun findWday(text: String): Int? {
        RE_WDAY.find(text)?.destructured?.let { (c) -> return WDAY_CHAR[c[0]] }
        for ((kw, d) in WEEKDAY_KW) if (text.contains(kw)) return d
        return null
    }
    private fun findSection(text: String): Pair<Int, Int>? {
        RE_SECTION.find(text)?.destructured?.let { (s, e) ->
            val si = s.toIntOrNull() ?: return@let; val ei = e.toIntOrNull() ?: return@let
            if (si in 1..14 && ei in si..14) return si to ei
        }
        return null
    }
    private fun findWeek(text: String): Pair<Int, Int>? {
        RE_WEEK.find(text)?.destructured?.let { (s, e) ->
            val si = s.toIntOrNull() ?: return@let; val ei = e.toIntOrNull() ?: return@let
            if (si in 1..25 && ei in si..25) return si to ei
        }
        return null
    }

    private fun isCourseName(text: String): Boolean {
        if (text.length !in 2..25) return false
        if (text.any { it.isDigit() } && text.length <= 3) return false
        if (isLocation(text) || isTeacher(text)) return false
        if (RE_SECTION.matches(text) || RE_WEEK.matches(text) || RE_WDAY.matches(text)) return false
        return text.any { it in '一'..'鿿' }
    }
    private fun isLocation(text: String): Boolean =
        text.length in 3..20 && LOC_KW.any { text.contains(it) }

    private fun isTeacher(text: String): Boolean {
        if (text.length !in 2..8) return false
        if (TEACHER_KW.any { text.contains(it) }) return true
        val cn = text.count { it in '一'..'鿿' }
        return cn in 2..4 && cn == text.length
    }

    // ---- POST-PROCESS ----

    private fun postProcess(courses: List<OcrCourseCandidate>): List<OcrCourseCandidate> {
        if (courses.isEmpty()) return emptyList()
        val colored = courses.mapIndexed { i, c -> c.copy(colorIndex = i % CourseColors.PALETTE.size) }
        val sorted = colored.sortedWith(compareBy({ it.weekday }, { it.startSection }))
        if (sorted.size <= 1) return sorted
        val merged = mutableListOf<OcrCourseCandidate>()
        var prev = sorted.first()
        for (i in 1 until sorted.size) {
            val cur = sorted[i]
            if (cur.name == prev.name && cur.weekday == prev.weekday && cur.startSection == prev.endSection + 1) {
                prev = prev.copy(endSection = cur.endSection)
            } else { merged.add(prev); prev = cur }
        }
        merged.add(prev)
        return merged
    }
}
