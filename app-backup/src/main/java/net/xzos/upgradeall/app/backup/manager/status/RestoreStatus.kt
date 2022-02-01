package net.xzos.upgradeall.app.backup.manager.status

data class RestoreStatus(
    val stage: RestoreStage,
    val progress: Progress, // percent
    val stepNote: String
)

enum class RestoreStage(val msg: String) {
    //DOWNLOAD_RESTORE_FILE("getting restore file"),
    RESTORE_DATABASE("Restore Database"),
    RESTORE_PREFS("Restore Pref"),
    RESTORE_INVALID("Invalid Restore File"),
    FINISH("Restore Finish"),
}

data class Progress(
    val total: Int,
    val currentIndex: Int,
)