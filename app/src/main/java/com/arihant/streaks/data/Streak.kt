package com.arihant.streaks.data

import android.os.Parcelable
import java.time.LocalDate
import kotlinx.parcelize.Parcelize
import java.time.temporal.ChronoUnit

@Parcelize
data class Reminder(
        val time: String, // ISO_LOCAL_TIME string
        val days: List<Int> = emptyList() // 0=Mon, 6=Sun
) : Parcelable

@Parcelize
data class Streak(
        val id: String,
        val name: String,
        val emoji: String,
        val frequency: FrequencyType,
        val frequencyCount: Int,
        val createdDate: String, // Store as ISO string for Parcelable
        val lastCompletedDate: String?, // Store as ISO string or null
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val isCompletedToday: Boolean = false,
        val completions: List<String> = emptyList(), // Store as ISO strings
        val reminder: Reminder? = null,
        val color: String = "#FF9900", // Default to neon orange
        val position: Int = Int.MAX_VALUE // New field for ordering
) : Parcelable {
        fun getCreatedDate(): LocalDate = LocalDate.parse(createdDate)
        fun getLastCompletedDate(): LocalDate? = lastCompletedDate?.let { LocalDate.parse(it) }
        fun asLocalDateCompletions(): List<LocalDate> = completions.map { LocalDate.parse(it) }

        // Calculates consistency percentage over a rolling window
        fun calculateConsistency(frequency: FrequencyType = FrequencyType.MONTHLY): Float {
                val windowDays = when (frequency) {
                        FrequencyType.DAILY -> 1
                        FrequencyType.WEEKLY -> 7
                        FrequencyType.MONTHLY -> 30
                        FrequencyType.YEARLY -> 365
                }

                val today = LocalDate.now()
                val created = getCreatedDate()

                val completionDates = asLocalDateCompletions().toSet()

                val earliestCompletion = completionDates.minOrNull()
                val effectiveStart = if (earliestCompletion != null && created.isAfter(earliestCompletion)) {
                        earliestCompletion
                } else {
                        created
                }

                // If the streak was created less than 30 days ago,
                // adjust the window to only cover days since creation.
                val daysSinceCreation = ChronoUnit.DAYS.between(effectiveStart, today).toInt() + 1
                val actualWindow = minOf(windowDays, maxOf(1, daysSinceCreation))

                var completedCount = 0
                var validDaysCount = 0

                for (i in 0 until actualWindow) {
                        val targetDate = today.minusDays(i.toLong())
                        // Only count days on or after the streak's creation date
                        if (!targetDate.isBefore(effectiveStart)) {
                                validDaysCount++
                                if (targetDate in completionDates) {
                                        completedCount++
                                }
                        }
                }

                if (validDaysCount == 0) return 0f
                return (completedCount.toFloat() / validDaysCount.toFloat()) * 100f
        }

        // Returns a formatted string representation of consistency, e.g., "90%"
        fun getFormattedConsistency(frequency: FrequencyType = FrequencyType.MONTHLY): String {
                val percentage = calculateConsistency(frequency)
                // Round to nearest integer or 1 decimal place
                return "${percentage.toInt()}%"
        }
}

@Parcelize
enum class FrequencyType : Parcelable {
        DAILY,
        WEEKLY,
        MONTHLY,
        YEARLY
}

data class StreakExportDto(
        val id: String,
        val name: String,
        val emoji: String,
        val frequency: FrequencyType,
        val frequencyCount: Int,
        val createdDate: String, // formatted as ISO string
        val lastCompletedDate: String?, // formatted as ISO string or null
        val currentStreak: Int = 0,
        val bestStreak: Int = 0,
        val completions: List<String> = emptyList(),
        val reminder: Reminder? = null,
        val color: String = "#FF9900", // Default to neon orange
        val position: Int = Int.MAX_VALUE // New field for ordering
)
