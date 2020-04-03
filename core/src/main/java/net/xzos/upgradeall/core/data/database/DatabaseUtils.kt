package net.xzos.upgradeall.core.data.database

import net.xzos.upgradeall.core.data.json.gson.AppDatabaseExtraData

fun AppDatabase.getExtraData(): AppDatabaseExtraData {
    return this.extraData ?: AppDatabaseExtraData()
        .apply { extraData = this }
}

fun AppDatabaseExtraData.getApplicationsAutoExclude(): MutableList<String> {
    return this.applicationsAutoExclude ?: mutableListOf<String>()
        .apply { applicationsAutoExclude = this }
}

fun AppDatabase.getApplicationsAutoExclude(): MutableList<String> {
    return this.getExtraData().getApplicationsAutoExclude()
}
