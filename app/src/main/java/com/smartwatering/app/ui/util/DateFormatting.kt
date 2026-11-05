package com.smartwatering.app.ui.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val NairobiZone: ZoneId = ZoneId.of("Africa/Nairobi")

fun relativeTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    val days = ((now - timestamp).coerceAtLeast(0) / (24L * 60 * 60 * 1000)).toInt()
    return when (days) { 0 -> "today"; 1 -> "yesterday"; else -> "$days days ago" }
}

fun localDate(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(NairobiZone).toLocalDate()
