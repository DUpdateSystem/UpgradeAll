package net.xzos.upgradeall.core.downloader.filedownloader.item

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.xzos.upgradeall.core.utils.coroutines.coroutinesMutableListOf
import zlc.season.rxdownload4.manager.*

class TaskWrapper internal constructor(internal val manager: TaskManager) {
    val file = manager.file()

    var snap: TaskSnap? = null

    private val subscribeList = coroutinesMutableListOf<(TaskSnap) -> Unit>()

    init {
        runBlocking(Dispatchers.Main) {
            manager.subscribe { status ->
                subscribeFun(status)
            }
        }
    }

    private fun subscribeFun(status: zlc.season.rxdownload4.manager.Status) {
        val downloadStatus = when (status) {
            is Normal -> Status.START
            is Pending -> Status.START
            is Started -> Status.START
            is Downloading -> Status.RUNNING
            is Paused -> Status.STOP
            is Completed -> Status.COMPLETE
            is Failed -> Status.FAIL
            is Deleted -> Status.CANCEL
        }
        val progress = status.progress
        manager.currentStatus()
        val oldSnap = this@TaskWrapper.snap
        val snap = TaskSnap(downloadStatus, progress.downloadSize, progress.totalSize)
        oldSnap?.also { snap.countSpeed(it) }
        this@TaskWrapper.snap = snap
        subscribeList.map { it(snap) }
    }

    internal fun subscribe(function: (TaskSnap) -> Unit) {
        subscribeList.add(function)
    }
}

fun TaskManager.wrapper() = TaskWrapper(this)