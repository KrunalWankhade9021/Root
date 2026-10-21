package com.krunal.rootd.domain.repo

import com.krunal.rootd.domain.model.AllowList
import com.krunal.rootd.domain.model.AlwaysAllowedApp
import kotlinx.coroutines.flow.Flow

interface AllowListRepository {
    fun observeAllowLists(): Flow<List<AllowList>>
    suspend fun create(name: String): Long
    suspend fun rename(id: Long, name: String)

    /** Returns false (and deletes nothing) if the list is used by the ongoing session or is the last list. */
    suspend fun delete(id: Long): Boolean
    suspend fun setDefault(id: Long)
    suspend fun getDefault(): AllowList
    suspend fun packagesFor(allowListId: Long): Set<String>
    suspend fun setPackages(allowListId: Long, packages: Set<String>)

    fun observeAlwaysAllowed(): Flow<List<AlwaysAllowedApp>>
    suspend fun alwaysAllowedPackages(): Set<String>

    /** Ignored if the package is already present. */
    suspend fun addAlwaysAllowed(app: AlwaysAllowedApp)

    /** Returns false (and removes nothing) if the app is a system default. */
    suspend fun removeAlwaysAllowed(appPackage: String): Boolean
}
