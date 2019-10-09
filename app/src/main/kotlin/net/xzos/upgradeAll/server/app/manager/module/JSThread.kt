package net.xzos.upgradeAll.server.app.manager.module

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

internal object JSThread {

    private val javascriptThreadList: MutableList<ExecutorCoroutineDispatcher> = mutableListOf()

    // TODO: 用户自定义线程池大小
    private const val threadMaxNumber = 64

    private var javascriptThreadIndex: Int = 0
        get() {
            field++
            field %= threadMaxNumber
            return field
        }

    internal fun getJavascriptThread(): ExecutorCoroutineDispatcher {
        val index = javascriptThreadIndex
        return if (index < javascriptThreadList.size)
            javascriptThreadList[index]
        else {
            val emptyNum = javascriptThreadIndex - index
            for (i in 0..emptyNum) {
                javascriptThreadList.add(Executors.newSingleThreadExecutor().asCoroutineDispatcher())
            }
            javascriptThreadList[index]
        }
    }
}
