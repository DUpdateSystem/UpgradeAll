package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.utils.oberver.Tag

enum class Status : Tag {
    NONE,
    START,
    RUNNING,
    STOP,
    COMPLETE,
    CANCEL,
    FAIL
}

class TaskSnap(
    val status: Status,
    var downloadSize: Long = 0,
    var totalSize: Long = 0,
) {
    internal val time = System.currentTimeMillis() / 1000L
    var speed = 0L

    fun speed(speed: Long) {
        this.speed = speed
    }
}

fun TaskSnap.progress() = if (totalSize == 0L) 0
else (downloadSize / totalSize) * 100

fun TaskSnap.countSpeed(snap: TaskSnap) {
    val downloaded = downloadSize - snap.downloadSize
    val time = time - snap.time
    val speed = if (time == 0L) snap.speed else downloaded / time
    speed(speed)
}