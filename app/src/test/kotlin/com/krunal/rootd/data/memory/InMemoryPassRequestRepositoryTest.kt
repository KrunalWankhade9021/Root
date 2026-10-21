package com.krunal.rootd.data.memory

import com.krunal.rootd.data.contract.PassRequestRepositoryContract

class InMemoryPassRequestRepositoryTest : PassRequestRepositoryContract() {
    private var nextSession = 1L
    override fun createRepo() = InMemoryPassRequestRepository()
    override suspend fun newSessionId() = nextSession++
}
