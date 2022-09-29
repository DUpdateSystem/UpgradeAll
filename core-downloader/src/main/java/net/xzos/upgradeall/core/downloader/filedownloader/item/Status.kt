package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.utils.oberver.Tag

enum class Status : Tag {
    NONE,
    START,
    RUNNING,
    STOP,
    COMPLETE,
    CANCEL,
    FAIL,
}

data class TaskSnap(
    val status: Status,
    val downloadSize: Long = 0,
    val totalSize: Long = 0,
) {
    internal val time = System.currentTimeMillis()
    var speed = 0L

    fun speed(snap: TaskSnap): TaskSnap {
        this.speed = countSpeed(snap)
        return this
    }

    companion object {
        private fun TaskSnap.countSpeed(snap: TaskSnap): Long {
            val downloaded = downloadSize - snap.downloadSize
            val time = time - snap.time
            return if (time == 0L) snap.speed else (downloaded / time) * 1000L
        }

    }
}

fun getNoneTaskSnap() = TaskSnap(Status.NONE)

fun TaskSnap.progress() = if (totalSize <= 0L) 0.toFloat()
else (downloadSize.toFloat() / totalSize.toFloat()) * 100
