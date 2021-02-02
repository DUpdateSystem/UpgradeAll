package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.runWithLock

class CoroutinesCount(count: Int) {
    var count: Int = count
        private set
    private val mutex = Mutex()

    fun plusAssign(a: Int) {
        mutex.runWithLock {
            count += a
        }
    }

    fun minusAssign(a: Int) {
        mutex.runWithLock {
            count -= a
        }
    }

    fun up() {
        plusAssign(1)
    }

    fun down() {
        minusAssign(1)
    }
}