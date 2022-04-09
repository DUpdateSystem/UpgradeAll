package net.xzos.upgradeall.core.utils.coroutines

import kotlinx.coroutines.sync.Mutex
import net.xzos.upgradeall.core.utils.oberver.Observer

class CoroutinesMutableList<E>(hash: Boolean = false, collection: Collection<E>? = null) : MutableList<E> {
    private val mutex = Mutex()
    private val list = if (hash) hashSetOf<E>() else mutableListOf()

    init {
        collection?.let { addAll(it) }
    }

    override val size: Int get() = list.size

    override fun contains(element: E): Boolean {
        return mutex.runWithLock {
            list.contains(element)
        }
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        return mutex.runWithLock {
            list.containsAll(elements)
        }
    }

    override fun get(index: Int): E {
        return mutex.runWithLock {
            if (list is MutableList) {
                list[index]
            } else {
                list.first()  // HashSet 元素顺序随机
            }
        }
    }

    override fun indexOf(element: E): Int {
        return mutex.runWithLock {
            if (list is MutableList) {
                list.indexOf(element)
            } else {
                -1  // HashSet 元素顺序随机
            }
        }
    }

    override fun isEmpty(): Boolean {
        return mutex.runWithLock {
            list.isEmpty()
        }
    }

    override fun iterator(): MutableIterator<E> {
        return mutex.runWithLock {
            list.toMutableList().iterator()
        }
    }

    override fun lastIndexOf(element: E): Int {
        return mutex.runWithLock {
            if (list is MutableList) {
                list.lastIndexOf(element)
            } else {
                -1  // HashSet 元素顺序随机
            }
        }
    }

    override fun add(element: E): Boolean {
        return mutex.runWithLock {
            list.add(element)
        }
    }

    override fun add(index: Int, element: E) {
        return mutex.runWithLock {
            if (list is MutableList) {
                list.add(index, element)
            } else {
                list.add(element)  // HashSet 元素顺序随机
            }
        }
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        return mutex.runWithLock {
            if (list is MutableList) {
                list.addAll(index, elements)
            } else {
                list.addAll(elements)  // HashSet 元素顺序随机
            }
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        return mutex.runWithLock {
            list.addAll(elements)
        }
    }

    override fun clear() {
        mutex.runWithLock {
            list.clear()
        }
    }

    fun resetList(elements: Collection<E>) {
        mutex.runWithLock {
            list.clear()
            list.addAll(elements)
        }
    }

    override fun listIterator(): MutableListIterator<E> = list.toMutableList().listIterator()

    override fun listIterator(index: Int): MutableListIterator<E> = list.toMutableList().listIterator(index)

    override fun remove(element: E): Boolean {
        return mutex.runWithLock {
            list.remove(element)
        }
    }

    override fun removeAll(elements: Collection<E>): Boolean {
        return mutex.runWithLock {
            list.removeAll(elements)
        }
    }

    override fun removeAt(index: Int): E {
        return mutex.runWithLock {
            if (list is MutableList)
                list.removeAt(index)
            else
                list.first().also {
                    list.remove(it)
                }  // HashSet 元素顺序随机
        }
    }

    override fun retainAll(elements: Collection<E>): Boolean {
        return mutex.runWithLock {
            list.retainAll(elements)
        }
    }

    override fun set(index: Int, element: E): E {
        return mutex.runWithLock {
            if (list is MutableList)
                list.set(index, element)
            else
                element.also {
                    list.add(it)
                }  // HashSet 元素顺序随机
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return mutex.runWithLock {
            if (list is MutableList)
                list.subList(fromIndex, toIndex)
            else
                mutableListOf()
        }
    }
}

fun <E> coroutinesMutableListOf(hash: Boolean = false) = CoroutinesMutableList<E>(hash)
fun <E> Collection<E>.toCoroutinesMutableList(hash: Boolean = false) = CoroutinesMutableList(hash, this)
