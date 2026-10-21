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

/** Storage formats from data-model §1: epoch ms, ISO `yyyy-MM-dd`, enum `db` strings. */
class Converters {
    @TypeConverter fun instantToDb(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun instantFromDb(v: Long?): Instant? = v?.let(Instant::ofEpochMilli)

    @TypeConverter fun localDateToDb(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun localDateFromDb(v: String?): LocalDate? = v?.let(LocalDate::parse)

    @TypeConverter fun sessionModeToDb(v: SessionMode?): String? = v?.db
    @TypeConverter fun sessionModeFromDb(v: String?): SessionMode? = v?.let(SessionMode::fromDb)

    @TypeConverter fun sessionStatusToDb(v: SessionStatus?): String? = v?.db
    @TypeConverter fun sessionStatusFromDb(v: String?): SessionStatus? = v?.let(SessionStatus::fromDb)

    @TypeConverter fun failReasonToDb(v: FailReason?): String? = v?.db
    @TypeConverter fun failReasonFromDb(v: String?): FailReason? = v?.let(FailReason::fromDb)

    @TypeConverter fun verdictToDb(v: Verdict?): String? = v?.db
    @TypeConverter fun verdictFromDb(v: String?): Verdict? = v?.let(Verdict::fromDb)

    @TypeConverter fun verdictSourceToDb(v: VerdictSource?): String? = v?.db
    @TypeConverter fun verdictSourceFromDb(v: String?): VerdictSource? = v?.let(VerdictSource::fromDb)

    @TypeConverter fun reasonCategoryToDb(v: ReasonCategory?): String? = v?.db
    @TypeConverter fun reasonCategoryFromDb(v: String?): ReasonCategory? = v?.let(ReasonCategory::fromDb)

    @TypeConverter fun passEndReasonToDb(v: PassEndReason?): String? = v?.db
    @TypeConverter fun passEndReasonFromDb(v: String?): PassEndReason? = v?.let(PassEndReason::fromDb)

    @TypeConverter fun coinKindToDb(v: CoinKind?): String? = v?.db
    @TypeConverter fun coinKindFromDb(v: String?): CoinKind? = v?.let(CoinKind::fromDb)

    @TypeConverter fun alwaysAllowedReasonToDb(v: AlwaysAllowedReason?): String? = v?.db
    @TypeConverter fun alwaysAllowedReasonFromDb(v: String?): AlwaysAllowedReason? = v?.let(AlwaysAllowedReason::fromDb)
}
