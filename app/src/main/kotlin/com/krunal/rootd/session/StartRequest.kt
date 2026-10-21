package com.krunal.rootd.session

import com.krunal.rootd.domain.model.SessionMode

/** What the Start screen asks for. Timer: plannedMinutes 10..120, step 5. Stopwatch: plannedMinutes null. */
data class StartRequest(
    val mode: SessionMode,
    val plannedMinutes: Int?,
    val speciesId: Long,
    val tagId: Long?,
    val allowListId: Long?,
    val goalNote: String?,
)
