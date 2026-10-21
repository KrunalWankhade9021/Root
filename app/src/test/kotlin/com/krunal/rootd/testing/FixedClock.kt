package com.krunal.rootd.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/** A mutable clock for tests: set or advance the time, change the zone. */
class FixedClock(
    var now: Instant = Instant.parse("2026-10-21T09:00:00Z"),
    private var zoneId: ZoneId = ZoneId.of("Asia/Kolkata"),
) : Clock() {
    override fun instant(): Instant = now
    override fun getZone(): ZoneId = zoneId
    override fun withZone(zone: ZoneId): Clock = FixedClock(now, zone)

    fun advance(by: Duration) {
        now = now.plus(by)
    }

    fun setZone(zone: ZoneId) {
        zoneId = zone
    }
}
