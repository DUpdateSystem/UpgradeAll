package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.runWithLock
import net.xzos.upgradeall.core.utils.wait

class CoroutinesCount(count: Int) {
    var count: Int = count
        private set(value) {
            field = value
            funList.forEach { it() }
        }
    private val mutex = Mutex()

    private val funList = coroutinesMutableListOf<() -> Unit>(true)

    fun plusAssign(a: Int): Int {
        return mutex.runWithLock {
            count += a
            count
        }
    }

    fun minusAssign(a: Int): Int {
        return mutex.runWithLock {
            count -= a
            count
        }
    }

    fun up(): Int {
        return plusAssign(1)
    }

    fun down(): Int {
        return minusAssign(1)
    }

    suspend fun waitNum(num: Int) {
        val mutex = Mutex(true)
        val f = {
            if (count == num)
                mutex.unlock()
        }
        funList.add(f)
        mutex.wait()
        funList.remove(f)
    }
}