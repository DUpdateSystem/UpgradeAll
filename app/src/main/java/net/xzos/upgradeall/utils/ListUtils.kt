package net.xzos.upgradeall.utils

const val add = "ADD"
const val del = "DEL"
const val swap = "SWAP"

open class ListOperationStepBase internal constructor(val operation: String)

class ListAddOperationStep<E> internal constructor(val index: Int, val element: E)
    : ListOperationStepBase(add)

class ListDelOperationStep internal constructor(val index: Int)
    : ListOperationStepBase(del)

class ListSwapOperationStep internal constructor(val rowIndex: Int, val newIndex: Int)
    : ListOperationStepBase(swap)

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
    for (item in delList) {
        val index = tmpList.indexOf(item)
        operationSteps.add(ListDelOperationStep(index))
        tmpList.removeAt(index)
    }
    for (item in addList) {
        val index = list2.indexOf(item)
        if (index < tmpList.size) {
            tmpList.add(index, item)
            operationSteps.add(ListAddOperationStep(index, item))
        } else {
            tmpList.add(item)
            operationSteps.add(ListAddOperationStep(tmpList.size - 1, item))
        }
    }
    for (i in tmpList.indices) {
        val item = tmpList[i]
        val newIndex = list2.indexOf(item)
        if (i != newIndex && newIndex < tmpList.size) {
            tmpList[i] = tmpList[newIndex].also { tmpList[newIndex] = tmpList[i] }
            operationSteps.add((ListSwapOperationStep(i, newIndex)))
        }
    }
    return operationSteps
}