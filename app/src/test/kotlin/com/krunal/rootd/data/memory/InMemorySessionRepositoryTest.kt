package com.krunal.rootd.data.memory

import com.krunal.rootd.data.contract.SessionRepositoryContract

class InMemorySessionRepositoryTest : SessionRepositoryContract() {
    override fun createRepo() = InMemorySessionRepository()
}
