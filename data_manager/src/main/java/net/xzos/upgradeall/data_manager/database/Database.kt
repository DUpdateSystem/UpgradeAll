package net.xzos.upgradeall.data_manager.database

interface Database {
    fun save() :Boolean
    fun delete() :Boolean
}
