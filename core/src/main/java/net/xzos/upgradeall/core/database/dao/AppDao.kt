package net.xzos.upgradeall.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.xzos.upgradeall.core.database.table.AppEntity
import net.xzos.upgradeall.core.database.table.HubEntity

@Dao
interface AppDao : BaseDao<AppEntity> {
    @Query("SELECT id FROM app WHERE rowid = :rowId")
    suspend fun loadIdByRowId(rowId: Long): Long

    @Query("SELECT * FROM app")
    suspend fun loadAll(): List<AppEntity>

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun deleteById(id: Long)
}