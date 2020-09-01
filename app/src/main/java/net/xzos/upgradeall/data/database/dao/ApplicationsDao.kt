package net.xzos.upgradeall.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.xzos.upgradeall.data.database.table.ApplicationsEntity


@Dao
interface ApplicationsDao : BaseDao<ApplicationsEntity> {
    @Query("SELECT id FROM applications WHERE rowid = :rowId")
    suspend fun getIdByRowId(rowId: Long): Long

    @Query("SELECT * FROM applications")
    suspend fun loadAll(): List<ApplicationsEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM applications WHERE id = :id LIMIT 1)")
    suspend fun existsById(id: Long): Boolean

    @Query("DELETE FROM applications WHERE id = :id")
    suspend fun deleteById(id: Long)
}
