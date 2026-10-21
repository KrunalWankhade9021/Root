package com.krunal.rootd.domain.model

import java.time.Instant

data class Tag(
    val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val archived: Boolean = false,
    val createdAt: Instant,
)

data class Species(
    val id: Long,
    val key: String,
    val name: String,
    val priceCoins: Int,
    val unlockedAt: Instant?,
    val sortOrder: Int,
)

data class AllowList(
    val id: Long = 0,
    val name: String,
    val isDefault: Boolean,
    val createdAt: Instant,
)

data class AlwaysAllowedApp(
    val appPackage: String,
    val reason: AlwaysAllowedReason,
    val isSystemDefault: Boolean,
)

data class AppRef(val packageName: String, val label: String)
