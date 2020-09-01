package net.xzos.upgradeall.data.database.dao

import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Update

interface BaseDao<E> {
    @Insert
    suspend fun insert(item: E): Long

    @Update
    suspend fun update(item: E)

    @Delete
    suspend fun delete(item: E)
}