package net.xzos.upgradeall.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.xzos.upgradeall.data.database.table.AppEntity

@Dao
interface AppDao : BaseDao<AppEntity> {
    @Query("SELECT id FROM app WHERE rowid = :rowId")
    suspend fun getIdByRowId(rowId: Long): Long

    @Query("SELECT * FROM app")
    suspend fun loadAll(): List<AppEntity>

    @Query("DELETE FROM app WHERE id = :id")
    suspend fun deleteById(id: Long)
}
