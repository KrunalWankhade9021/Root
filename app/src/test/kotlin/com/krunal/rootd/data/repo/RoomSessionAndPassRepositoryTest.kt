package com.krunal.rootd.data.repo

import com.krunal.rootd.data.contract.PassRequestRepositoryContract
import com.krunal.rootd.data.contract.SessionRepositoryContract
import com.krunal.rootd.data.db.newTestDb
import com.krunal.rootd.data.db.sessionRow
import com.krunal.rootd.domain.model.SessionStatus
import com.krunal.rootd.testing.T0
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomSessionRepositoryTest : SessionRepositoryContract() {
    private val db = newTestDb()
    override fun createRepo() = RoomSessionRepository(db)

    @After
    fun close() = db.close()
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomPassRequestRepositoryTest : PassRequestRepositoryContract() {
    private val db = newTestDb()
    private var n = 0L
    override fun createRepo() = RoomPassRequestRepository(db)
    override suspend fun newSessionId(): Long =
        db.sessionDao().insert(sessionRow(status = SessionStatus.GROWN, startedAt = T0.plusSeconds(n++)))

    @After
    fun close() = db.close()
}
