package com.krunal.rootd.data.repo

import android.database.sqlite.SQLiteConstraintException
import com.krunal.rootd.data.db.RootdDatabase
import com.krunal.rootd.data.db.entity.toEntity
import com.krunal.rootd.data.db.entity.toModel
import com.krunal.rootd.domain.model.Session
import com.krunal.rootd.domain.repo.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class RoomSessionRepository(db: RootdDatabase) : SessionRepository {
    private val dao = db.sessionDao()

    override suspend fun insert(session: Session): Long = oneOngoing { dao.insert(session.toEntity()) }

    override suspend fun update(session: Session) = oneOngoing { dao.update(session.toEntity()) }

    override suspend fun get(id: Long): Session? = dao.get(id)?.toModel()

    override suspend fun getOngoing(): Session? = dao.getOngoing()?.toModel()

    override fun observeOngoing(): Flow<Session?> = dao.observeOngoing().map { it?.toModel() }

    override fun observeBetween(startDate: LocalDate, endDateInclusive: LocalDate): Flow<List<Session>> =
        dao.observeBetween(startDate, endDateInclusive).map { rows -> rows.map { it.toModel() } }

    override suspend fun countFinished(): Int = dao.countFinished()

    override suspend fun earliestStartDate(): LocalDate? = dao.earliestStartDate()

    /** The only constraint a session write can break is the one-ongoing trigger (data-model §8). */
    private inline fun <T> oneOngoing(block: () -> T): T = try {
        block()
    } catch (e: SQLiteConstraintException) {
        throw IllegalStateException("An ongoing session already exists", e)
    }
}
