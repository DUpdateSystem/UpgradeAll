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
    val operationSteps = mutableListOf<ListOperationStepBase>()
    // 删除多余项
    val delList = list1.filter {
        !list2.contains(it)
    }
    for (item in delList) {
        operationSteps.add(ListDelOperationStep(item))
        list1.remove(item)
    }
    if (list1 == list2) return operationSteps

    // 生成索引（支持 list 包含多个相同值）
    val listItemIndex = mutableMapOf<E, MutableList<Int>>()
    for (i in list2.indices) {
        val item = list2[i]
        val list = listItemIndex[item] ?: mutableListOf<Int>().also {
            listItemIndex[item] = it
        }
        list.add(i)
    }

    // 添加缺失项
    val addItemMap = mutableMapOf<E, MutableList<Int>>()
    val listItemIndexC = listItemIndex.toMutableMap()
    for (item in list2) {
        if (!list1.contains(item)) {
            for (i in listItemIndexC[item]!!) {
                var index = i
                if (index >= list1.size) {
                    index = list1.size
                }
                list1.add(index, item)
                val list = addItemMap[item] ?: mutableListOf<Int>().also {
                    addItemMap[item] = it
                }
                list.add(index)
            }
        }
    }
    // 还原位置
    val swapItemMap = mutableMapOf<E, Pair<Int, Int>>()
    if (list1 != list2) {
        for (i in list1.indices) {
            val item = list1[i]
            if (list1[i] != list2[i]) {
                val newIndex = listItemIndex[item]!!.removeAt(0)
                list1[i] = list1[newIndex].also { list1[newIndex] = list1[i] }
                addItemMap[item]?.let { list ->
                    list.remove(i)
                    list.add(newIndex)
                } ?: run {
                    swapItemMap[item]?.let {
                        swapItemMap[item] = Pair(it.first, newIndex)
                    } ?: run { swapItemMap[item] = Pair(i, newIndex) }
                }
            }
        }
    }

    val addOperationSteps = mutableListOf<ListAddOperationStep<E>>()
    for ((item, list) in addItemMap.entries) {
        for (index in list) {
            addOperationSteps.add(ListAddOperationStep(index, item))
        }
    }
    val sortedAddOperationSteps = addOperationSteps.sortedBy { it.index }
    operationSteps.addAll(sortedAddOperationSteps)
    for ((_, index) in swapItemMap) {
        operationSteps.add((ListSwapOperationStep(index.first, index.second)))
    }
    return operationSteps
}
