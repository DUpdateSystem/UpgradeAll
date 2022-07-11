package net.xzos.upgradeall.core.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery


interface BaseDao<E> {
    @Insert
    suspend fun insert(item: E): Long

    @Update
    suspend fun update(item: E)

    @Delete
    suspend fun delete(item: E)

    @RawQuery
    suspend fun checkpoint(supportSQLiteQuery: SupportSQLiteQuery): Int
}