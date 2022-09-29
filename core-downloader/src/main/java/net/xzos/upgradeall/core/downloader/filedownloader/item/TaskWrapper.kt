package net.xzos.upgradeall.core.downloader.filedownloader.item

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.downloader.filedownloader.item.data.file

class TaskWrapper private constructor(
    internal val manager: DownloadWorker,
    private val subscribeList: MutableList<(TaskSnap) -> Unit>
) {
    val file = manager.taskData.file()

    var snap: TaskSnap = getNoneTaskSnap()

    private suspend fun init() {
        withContext(Dispatchers.Main) {
            manager.observe { status ->
                subscribeFun(status)
            }
        }
    }

    private fun subscribeFun(snap: TaskSnap) {
        snap.speed(this.snap)
        this.snap = snap
        subscribeList.map { it(snap) }
    }

    fun observe(func: (TaskSnap) -> Unit) {
        subscribeList.add(func)
    }

    companion object {
        internal suspend fun new(
            manager: DownloadWorker,
            vararg subscribe: (TaskSnap) -> Unit
        ) = TaskWrapper(manager, subscribe.toMutableList()).apply { init() }
    }
}

internal suspend fun DownloadWorker.wrapper(
    vararg subscribe: (TaskSnap) -> Unit
) = TaskWrapper.new(this, *subscribe)