package com.krunal.rootd.data.db

import com.krunal.rootd.domain.model.AlwaysAllowedReason
import com.krunal.rootd.domain.model.CoinKind
import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.PassEndReason
import com.krunal.rootd.domain.model.ReasonCategory
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.model.VerdictSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    @Test
    fun instant_roundTrip() {
        val instant = Instant.parse("2026-10-21T09:30:45.123Z")
        val millis = Converters.toMillis(instant)
        val back = Converters.fromMillis(millis)
        assertEquals(instant, back)
    }

    @Test
    fun instant_nullRoundTrip() {
        val millis = Converters.toMillis(null)
        assertEquals(null, millis)
        val back = Converters.fromMillis(millis)
        assertEquals(null, back)
    }

    @Test
    fun localDate_roundTrip() {
        val date = LocalDate.parse("2026-10-21")
        val str = Converters.toLocalDateString(date)
        val back = Converters.fromLocalDateString(str)
        assertEquals(date, back)
    }

    @Test
    fun localDate_nullRoundTrip() {
        val str = Converters.toLocalDateString(null)
        assertEquals(null, str)
        val back = Converters.fromLocalDateString(str)
        assertEquals(null, back)
    }

    @Test
    fun sessionMode_roundTrip() {
        SessionMode.entries.forEach { mode ->
            val db = Converters.sessionModeToDb(mode)
            val back = Converters.sessionModeFromDb(db)
            assertEquals(mode, back)
        }
    }

    @Test
    fun sessionStatus_roundTrip() {
        SessionStatus.entries.forEach { status ->
            val db = Converters.sessionStatusToDb(status)
            val back = Converters.sessionStatusFromDb(db)
            assertEquals(status, back)
        }
    }

    @Test
    fun failReason_roundTrip() {
        FailReason.entries.forEach { reason ->
            val db = Converters.failReasonToDb(reason)
            val back = Converters.failReasonFromDb(db)
            assertEquals(reason, back)
        }
    }

    @Test
    fun verdict_roundTrip() {
        Verdict.entries.forEach { verdict ->
            val db = Converters.verdictToDb(verdict)
            val back = Converters.verdictFromDb(db)
            assertEquals(verdict, back)
        }
    }

    @Test
    fun verdictSource_roundTrip() {
        VerdictSource.entries.forEach { source ->
            val db = Converters.verdictSourceToDb(source)
            val back = Converters.verdictSourceFromDb(db)
            assertEquals(source, back)
        }
    }

    @Test
    fun reasonCategory_roundTrip() {
        ReasonCategory.entries.forEach { category ->
            val db = Converters.reasonCategoryToDb(category)
            val back = Converters.reasonCategoryFromDb(db)
            assertEquals(category, back)
        }
    }

    @Test
    fun passEndReason_roundTrip() {
        PassEndReason.entries.forEach { reason ->
            val db = Converters.passEndReasonToDb(reason)
            val back = Converters.passEndReasonFromDb(db)
            assertEquals(reason, back)
        }
    }

    @Test
    fun coinKind_roundTrip() {
        CoinKind.entries.forEach { kind ->
            val db = Converters.coinKindToDb(kind)
            val back = Converters.coinKindFromDb(db)
            assertEquals(kind, back)
        }
    }

    @Test
    fun alwaysAllowedReason_roundTrip() {
        AlwaysAllowedReason.entries.forEach { reason ->
            val db = Converters.alwaysAllowedReasonToDb(reason)
            val back = Converters.alwaysAllowedReasonFromDb(db)
            assertEquals(reason, back)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun sessionMode_unknownThrows() {
        Converters.sessionModeFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun sessionStatus_unknownThrows() {
        Converters.sessionStatusFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun failReason_unknownThrows() {
        Converters.failReasonFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun verdict_unknownThrows() {
        Converters.verdictFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun verdictSource_unknownThrows() {
        Converters.verdictSourceFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun reasonCategory_unknownThrows() {
        Converters.reasonCategoryFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun passEndReason_unknownThrows() {
        Converters.passEndReasonFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun coinKind_unknownThrows() {
        Converters.coinKindFromDb("unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun alwaysAllowedReason_unknownThrows() {
        Converters.alwaysAllowedReasonFromDb("unknown")
    }
}