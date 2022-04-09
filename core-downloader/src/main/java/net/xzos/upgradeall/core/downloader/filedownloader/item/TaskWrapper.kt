package net.xzos.upgradeall.core.downloader.filedownloader.item

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zlc.season.rxdownload4.manager.*

class TaskWrapper private constructor(
    internal val manager: TaskManager,
    private var subscribeList: Array<out (TaskSnap) -> Unit>
) {
    val file = manager.file()

    var snap: TaskSnap = getNoneTaskSnap()

    private suspend fun init() {
        withContext(Dispatchers.Main) {
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
        val snap = TaskSnap(downloadStatus, progress.downloadSize, progress.totalSize)
        snap.countSpeed(this.snap)
        this.snap = snap
        subscribeList.map { it(snap) }
    }

    companion object {
        internal suspend fun new(
            manager: TaskManager,
            vararg subscribe: (TaskSnap) -> Unit
        ) = TaskWrapper(manager, subscribe).apply { init() }
    }
}

suspend fun TaskManager.wrapper(
    vararg subscribe: (TaskSnap) -> Unit
) = TaskWrapper.new(this, *subscribe)