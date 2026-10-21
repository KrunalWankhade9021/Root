package com.krunal.rootd.data.db

import androidx.room.TypeConverter
import com.krunal.rootd.domain.model.AlwaysAllowedReason
import com.krunal.rootd.domain.model.CoinKind
import com.krunal.rootd.domain.model.FailReason
import com.krunal.rootd.domain.model.PassEndReason
import com.krunal.rootd.domain.model.ReasonCategory
import com.krunal.rootd.domain.model.SessionMode
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.domain.model.Verdict
import com.krunal.rootd.domain.model.VerdictSource
import java.time.Instant
import java.time.LocalDate

object Converters {

    @TypeConverter
    fun toMillis(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromMillis(millis: Long?): Instant? = millis?.let { Instant.ofEpochMilli(it) }

    @TypeConverter
    fun toLocalDateString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun fromLocalDateString(str: String?): LocalDate? = str?.let { LocalDate.parse(it) }

    @TypeConverter
    fun sessionModeToDb(mode: SessionMode): String = mode.db

    @TypeConverter
    fun sessionModeFromDb(db: String): SessionMode = SessionMode.fromDb(db)

    @TypeConverter
    fun sessionStatusToDb(status: SessionStatus): String = status.db

    @TypeConverter
    fun sessionStatusFromDb(db: String): SessionStatus = SessionStatus.fromDb(db)

    @TypeConverter
    fun failReasonToDb(reason: FailReason): String = reason.db

    @TypeConverter
    fun failReasonFromDb(db: String): FailReason = FailReason.fromDb(db)

    @TypeConverter
    fun verdictToDb(verdict: Verdict): String = verdict.db

    @TypeConverter
    fun verdictFromDb(db: String): Verdict = Verdict.fromDb(db)

    @TypeConverter
    fun verdictSourceToDb(source: VerdictSource): String = source.db

    @TypeConverter
    fun verdictSourceFromDb(db: String): VerdictSource = VerdictSource.fromDb(db)

    @TypeConverter
    fun reasonCategoryToDb(category: ReasonCategory): String = category.db

    @TypeConverter
    fun reasonCategoryFromDb(db: String): ReasonCategory = ReasonCategory.fromDb(db)

    @TypeConverter
    fun passEndReasonToDb(reason: PassEndReason): String = reason.db

    @TypeConverter
    fun passEndReasonFromDb(db: String): PassEndReason = PassEndReason.fromDb(db)

    @TypeConverter
    fun coinKindToDb(kind: CoinKind): String = kind.db

    @TypeConverter
    fun coinKindFromDb(db: String): CoinKind = CoinKind.fromDb(db)

    @TypeConverter
    fun alwaysAllowedReasonToDb(reason: AlwaysAllowedReason): String = reason.db

    @TypeConverter
    fun alwaysAllowedReasonFromDb(db: String): AlwaysAllowedReason = AlwaysAllowedReason.fromDb(db)
}