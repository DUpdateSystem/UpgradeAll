package net.xzos.upgradeall.utils

import androidx.lifecycle.MutableLiveData

fun <E> MutableLiveData<MutableList<E>>.setList(list: List<E>) {
    value!!.clear()
    value!!.addAll(list)
    notifyObserver()
}