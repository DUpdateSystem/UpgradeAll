package net.xzos.upgradeall.core.downloader.filedownloader.item

import net.xzos.upgradeall.core.utils.oberver.Tag
import java.time.Instant
import java.time.format.DateTimeFormatter

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
    internal val time = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toLong()
    var speed = 0L

    fun speed(speed: Long) {
        this.speed = speed
    }
}

fun TaskSnap.countSpeed(snap: TaskSnap) {
    val downloaded = downloadSize - snap.downloadSize
    val time = time - snap.time
    val speed = downloaded / time
    speed(speed)
}