package net.xzos.upgradeall.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.xzos.upgradeall.core.database.table.extra_hub.ExtraHubEntity


@Dao
interface ExtraHubDao : BaseDao<ExtraHubEntity> {
    @Query("SELECT * FROM extra_hub WHERE id= :id")
    suspend fun loadByUuid(id: String): ExtraHubEntity?

    @Query("DELETE FROM extra_hub WHERE id= :id")
    suspend fun deleteByUuid(id: String)
}