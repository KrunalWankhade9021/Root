package com.krunal.rootd.app

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * System time in the device's *current* zone. Unlike `Clock.systemDefaultZone()`, the zone is re-read on every
 * call, so local dates stay correct after the user changes time zone while the app is running.
 */
object DeviceClock : Clock() {
    override fun instant(): Instant = Instant.now()
    override fun getZone(): ZoneId = ZoneId.systemDefault()
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
}
