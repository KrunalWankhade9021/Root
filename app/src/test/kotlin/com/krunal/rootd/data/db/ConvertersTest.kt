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
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {

    private val c = Converters()

    @Test
    fun instant_storedAsEpochMillis() {
        val t = Instant.parse("2026-10-21T09:00:00.123Z")
        assertEquals(t.toEpochMilli(), c.instantToDb(t))
        assertEquals(t, c.instantFromDb(t.toEpochMilli()))
        assertNull(c.instantToDb(null))
        assertNull(c.instantFromDb(null))
    }

    @Test
    fun localDate_storedAsIsoText() {
        val d = LocalDate.parse("2026-10-21")
        assertEquals("2026-10-21", c.localDateToDb(d))
        assertEquals(d, c.localDateFromDb("2026-10-21"))
        assertNull(c.localDateToDb(null))
        assertNull(c.localDateFromDb(null))
    }

    @Test
    fun enums_storedAsTheirDbStrings() {
        SessionMode.entries.forEach { assertEquals(it.db, c.sessionModeToDb(it)); assertEquals(it, c.sessionModeFromDb(it.db)) }
        SessionStatus.entries.forEach { assertEquals(it.db, c.sessionStatusToDb(it)); assertEquals(it, c.sessionStatusFromDb(it.db)) }
        FailReason.entries.forEach { assertEquals(it.db, c.failReasonToDb(it)); assertEquals(it, c.failReasonFromDb(it.db)) }
        Verdict.entries.forEach { assertEquals(it.db, c.verdictToDb(it)); assertEquals(it, c.verdictFromDb(it.db)) }
        VerdictSource.entries.forEach { assertEquals(it.db, c.verdictSourceToDb(it)); assertEquals(it, c.verdictSourceFromDb(it.db)) }
        ReasonCategory.entries.forEach { assertEquals(it.db, c.reasonCategoryToDb(it)); assertEquals(it, c.reasonCategoryFromDb(it.db)) }
        PassEndReason.entries.forEach { assertEquals(it.db, c.passEndReasonToDb(it)); assertEquals(it, c.passEndReasonFromDb(it.db)) }
        CoinKind.entries.forEach { assertEquals(it.db, c.coinKindToDb(it)); assertEquals(it, c.coinKindFromDb(it.db)) }
        AlwaysAllowedReason.entries.forEach {
            assertEquals(it.db, c.alwaysAllowedReasonToDb(it)); assertEquals(it, c.alwaysAllowedReasonFromDb(it.db))
        }
    }

    @Test
    fun nullableEnums_mapNull() {
        assertNull(c.failReasonToDb(null))
        assertNull(c.failReasonFromDb(null))
        assertNull(c.passEndReasonToDb(null))
        assertNull(c.passEndReasonFromDb(null))
    }
}
