package net.xzos.upgradeall.utils

open class ListOperationStepBase

class ListAddOperationStep<E> internal constructor(val index: Int, val element: E)
    : ListOperationStepBase()

class ListDelOperationStep internal constructor(val index: Int)
    : ListOperationStepBase()

class ListSwapOperationStep internal constructor(val rowIndex: Int, val newIndex: Int)
    : ListOperationStepBase()

fun <E> list1ToList2(list1: List<E>, list2: List<E>): List<ListOperationStepBase> {
    if (list1 == list2) return emptyList()
    val delList = list1.filter {
        !list2.contains(it)
    }
    val addList = list2.filter {
        !list1.contains(it)
    }
    val tmpList = list1.toMutableList()
    val operationSteps = mutableListOf<ListOperationStepBase>()
    // 删除多余项
    for (item in delList) {
        val index = tmpList.indexOf(item)
        operationSteps.add(ListDelOperationStep(index))
        tmpList.removeAt(index)
    }
    // 添加缺失项
    val addItemMap = hashMapOf<E, Int>()
    for (item in addList) {
        val index = list2.indexOf(item)
        if (index < tmpList.size) {
            tmpList.add(index, item)
            addItemMap[item] = index
        } else {
            tmpList.add(item)
            addItemMap[item] = tmpList.size - 1
        }
    }
    // 还原位置
    val swapItemMap = mutableMapOf<E, Pair<Int, Int>>()
    for (i in tmpList.indices) {
        val item = tmpList[i]
        val newIndex = list2.indexOf(item)
        if (i != newIndex) {
            tmpList[i] = tmpList[newIndex].also { tmpList[newIndex] = tmpList[i] }
            if (item in addItemMap) {
                addItemMap[item] = newIndex
            } else {
                swapItemMap[item]?.let {
                    swapItemMap[item] = Pair(it.first, newIndex)
                } ?: run { swapItemMap[item] = Pair(i, newIndex) }
            }
        }
    }
    val sorted = addItemMap.toList().sortedBy { (_, value) -> value }.toMap()
    for ((item, index) in sorted.entries) {
        operationSteps.add(ListAddOperationStep(index, item))
    }
    for ((_, index) in swapItemMap) {
        operationSteps.add((ListSwapOperationStep(index.first, index.second)))
    }
    return operationSteps
}
