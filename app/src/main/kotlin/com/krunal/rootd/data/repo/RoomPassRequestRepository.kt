package com.krunal.rootd.data.repo

import com.krunal.rootd.data.db.RootdDatabase
import com.krunal.rootd.data.db.entity.toEntity
import com.krunal.rootd.data.db.entity.toModel
import com.krunal.rootd.domain.model.PassRequest
import com.krunal.rootd.domain.repo.PassRequestRepository
import java.time.LocalDate

class RoomPassRequestRepository(db: RootdDatabase) : PassRequestRepository {
    private val dao = db.passRequestDao()

    override suspend fun insert(request: PassRequest): Long = dao.insert(request.toEntity())

    override suspend fun update(request: PassRequest) = dao.update(request.toEntity())

    override suspend fun get(id: Long): PassRequest? = dao.get(id)?.toModel()

    override suspend fun forSession(sessionId: Long): List<PassRequest> = dao.forSession(sessionId).map { it.toModel() }

    override suspend fun activePass(sessionId: Long): PassRequest? = dao.activePass(sessionId)?.toModel()

    override suspend fun countNonAppealForAppOn(appPackage: String, date: LocalDate): Int =
        dao.countNonAppealForAppOn(appPackage, date)

    override suspend fun between(startDate: LocalDate, endDateInclusive: LocalDate): List<PassRequest> =
        dao.between(startDate, endDateInclusive).map { it.toModel() }
}
