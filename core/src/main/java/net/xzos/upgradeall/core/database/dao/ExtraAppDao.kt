package net.xzos.upgradeall.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import net.xzos.upgradeall.core.database.table.extra_app.ExtraAppEntity

@Dao
interface ExtraAppDao : BaseDao<ExtraAppEntity> {
    @Query("SELECT * FROM extra_app")
    suspend fun loadAll(): List<ExtraAppEntity>
}