package net.xzos.upgradeall.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.xzos.upgradeall.core.database.table.HubEntity


@Dao
interface HubDao : BaseDao<HubEntity> {
    @Query("SELECT * FROM hub")
    suspend fun loadAll(): List<HubEntity>

    @Query("SELECT * FROM hub WHERE uuid = :uuid")
    suspend fun loadByUuid(uuid: String): HubEntity?

    @Query("DELETE FROM hub WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)
}