package com.krunal.rootd.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.krunal.rootd.domain.model.AlwaysAllowedReason

@Entity(tableName = "always_allowed_apps")
data class AlwaysAllowedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "app_package") val appPackage: String,
    @ColumnInfo(name = "reason") val reason: AlwaysAllowedReason,
    @ColumnInfo(name = "is_system_default") val isSystemDefault: Boolean = false,
) {
    fun toModel(): com.krunal.rootd.domain.model.AlwaysAllowedApp {
        return com.krunal.rootd.domain.model.AlwaysAllowedApp(
            appPackage = appPackage,
            reason = reason,
            isSystemDefault = isSystemDefault
        )
    }

    companion object {
        fun fromModel(model: com.krunal.rootd.domain.model.AlwaysAllowedApp): AlwaysAllowedAppEntity {
            return AlwaysAllowedAppEntity(
                appPackage = model.appPackage,
                reason = model.reason,
                isSystemDefault = model.isSystemDefault
            )
        }
    }
}