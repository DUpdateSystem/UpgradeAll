package net.xzos.upgradeall.ui.base.selectlistdialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.ItemTouchHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.core.utils.coroutines.wait
import net.xzos.upgradeall.ui.base.listdialog.ListDialog

class SelectListDialog private constructor(
        title: Any?,
        list: List<SelectItem>,
        private val mutex: Mutex,
        val adapter: SelectListAdapter = SelectListAdapter(list.toMutableList())
) : ListDialog(title, adapter) {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            val itemTouchHelperCallback = SelectListTouchHelperCallBack(adapter)
            val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback).apply {
                adapter.itemTouchHelper = this
            }
            itemTouchHelper.attachToRecyclerView(binding.rvList)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        mutex.unlock()
    }

    fun getDataList() = adapter.dataList

    companion object {
        suspend fun showDialog(list: List<SelectItem>, supportFragmentManager: FragmentManager, title: Any?): List<SelectItem> {
            val mutex = Mutex(true)
            val dialog = SelectListDialog(title, list, mutex).apply {
                show(supportFragmentManager)
            }
            withContext(Dispatchers.Default) {
                mutex.wait()
            }
            return dialog.getDataList()
        }
    }
}