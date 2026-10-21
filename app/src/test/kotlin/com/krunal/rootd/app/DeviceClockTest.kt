package com.krunal.rootd.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.util.TimeZone

class DeviceClockTest {

    private val original = TimeZone.getDefault()

    @After
    fun restore() = TimeZone.setDefault(original)

    @Test
    fun zone_followsDeviceTimeZoneChangesWhileRunning() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))
        assertEquals(ZoneId.of("Asia/Kolkata"), DeviceClock.zone)

        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))
        assertEquals(ZoneId.of("America/New_York"), DeviceClock.zone) // Review Focus 2: no stale zone
    }
}
