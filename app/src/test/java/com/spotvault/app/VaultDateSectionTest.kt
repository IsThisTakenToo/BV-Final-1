package com.spotvault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * vaultDateSection buckets by calendar day/week/month/year, not by rolling millisecond windows —
 * these lock in the boundary behavior at each cutover, including two edge cases already found
 * during manual audit: today falling exactly on the week-start day, and "now" falling early
 * enough in the month that the month boundary sits inside the current calendar week.
 */
class VaultDateSectionTest {

    /** This week's Monday at noon, relative to whatever "today" the test clock is on — computed
     * the same way vaultDateSection computes its own week start, so this can't drift out of sync
     * with the production logic it's checking. */
    private fun thisMonday(): Calendar = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.plusDays(days: Int): Calendar =
        (clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, days) }

    @Test
    fun today_isLabeledToday() {
        val now = Calendar.getInstance().timeInMillis
        assertEquals("Today", vaultDateSection(now, now))
    }

    @Test
    fun today_earlyMorning_stillTodayNotYesterday() {
        // A spot saved at 12:01am today is under 24h old but must still bucket as "Today" —
        // bucketing is by calendar day, not a rolling 24-hour window.
        val now = Calendar.getInstance()
        val earlyToday = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
        }
        assertEquals("Today", vaultDateSection(earlyToday.timeInMillis, now.timeInMillis))
    }

    @Test
    fun lateLastNight_isYesterdayNotToday() {
        // A spot from 11:59pm yesterday is well under 24h old but must not bucket as "Today."
        val now = Calendar.getInstance()
        val lastNight = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
        }
        assertEquals("Yesterday", vaultDateSection(lastNight.timeInMillis, now.timeInMillis))
    }

    @Test
    fun mondayThroughToday_bucketAsThisWeek() {
        val monday = thisMonday()
        val now = monday.plusDays(3) // Thursday
        assertEquals("This Week", vaultDateSection(monday.timeInMillis, now.timeInMillis))
        assertEquals("This Week", vaultDateSection(monday.plusDays(1).timeInMillis, now.timeInMillis))
    }

    @Test
    fun today_isMonday_previousWeeksSaturday_isNotThisWeek() {
        // Regression: Calendar.set(DAY_OF_WEEK, MONDAY) with firstDayOfWeek = MONDAY must resolve
        // to *this* Monday when today already is Monday, not roll back an extra week — otherwise
        // a spot from the *previous* week's Saturday would wrongly test >= weekStartDay and get
        // swept into "This Week."
        val monday = thisMonday()
        val saturdayLastWeek = monday.plusDays(-2)
        assertNotEquals("This Week", vaultDateSection(saturdayLastWeek.timeInMillis, monday.timeInMillis))
    }

    @Test
    fun monthBoundaryInsideCurrentWeek_previousMonthDayIsNotThisMonth() {
        // Force "now" onto a day early enough in the month that the 1st-of-month boundary falls
        // inside the current calendar week — a spot from the last day of the *previous* month
        // must fall through past both "This Week" and "This Month" to the month-name bucket.
        val now = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 3)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val lastMonthEnd = (now.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val expected = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(lastMonthEnd.timeInMillis))
        assertEquals(expected, vaultDateSection(lastMonthEnd.timeInMillis, now.timeInMillis))
    }

    @Test
    fun sameCalendarYear_olderThanAMonth_bucketsByMonthNameOnly() {
        val now = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 20) }
        val threeMonthsAgo = (now.clone() as Calendar).apply { add(Calendar.MONTH, -3) }
        // Guard against the 3-months-back subtraction crossing into the previous year.
        org.junit.Assume.assumeTrue(threeMonthsAgo.get(Calendar.YEAR) == now.get(Calendar.YEAR))
        val expected = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(threeMonthsAgo.timeInMillis))
        assertEquals(expected, vaultDateSection(threeMonthsAgo.timeInMillis, now.timeInMillis))
    }

    @Test
    fun pastCalendarYear_bucketsAsYearOnly_notMonthName() {
        val now = Calendar.getInstance()
        val lastYear = (now.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        assertEquals(
            lastYear.get(Calendar.YEAR).toString(),
            vaultDateSection(lastYear.timeInMillis, now.timeInMillis)
        )
    }
}
