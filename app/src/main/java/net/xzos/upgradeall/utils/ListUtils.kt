package net.xzos.upgradeall.utils

open class ListOperationStepBase

class ListAddOperationStep<E> internal constructor(val index: Int, val element: E)
    : ListOperationStepBase()

class ListDelOperationStep<E> internal constructor(val element: E)
    : ListOperationStepBase()

class ListSwapOperationStep internal constructor(val rowIndex: Int, val newIndex: Int)
    : ListOperationStepBase()

fun <E> list1ToList2(list1t: List<E>, list2t: List<E>): List<ListOperationStepBase> {
    val list1 = list1t.toMutableList()
    val list2 = list2t.toList()
    if (list1 == list2) return emptyList()
    val delList = list1.filter {
        !list2.contains(it)
    }
    val addList = list2.filter {
        !list1.contains(it)
    }
    val operationSteps = mutableListOf<ListOperationStepBase>()
    // 删除多余项
    for (item in delList) {
        operationSteps.add(ListDelOperationStep(item))
        list1.remove(item)
    }
    // 添加缺失项
    val addItemMap = hashMapOf<E, Int>()
    for (item in addList) {
        var index = list2.indexOf(item)
        if (index >= list1.size) {
            index = list1.size
        }
        list1.add(index, item)
        addItemMap[item] = index
    }
    // 还原位置
    val swapItemMap = mutableMapOf<E, Pair<Int, Int>>()
    for (i in list1.indices) {
        val item = list1[i]
        val newIndex = list2.indexOf(item)
        if (i != newIndex) {
            list1[i] = list1[newIndex].also { list1[newIndex] = list1[i] }
            if (item in addItemMap) {
                addItemMap[item] = newIndex
            } else {
                swapItemMap[item]?.let {
                    swapItemMap[item] = Pair(it.first, newIndex)
                } ?: run { swapItemMap[item] = Pair(i, newIndex) }
            }
        }
    }
    val sorted = addItemMap.toList().sortedBy { (_, value) -> value }
    for ((item, index) in sorted) {
        operationSteps.add(ListAddOperationStep(index, item))
    }
    for ((_, index) in swapItemMap) {
        operationSteps.add((ListSwapOperationStep(index.first, index.second)))
    }
    return operationSteps
}
