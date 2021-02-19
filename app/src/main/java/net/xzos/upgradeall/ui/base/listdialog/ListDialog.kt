package net.xzos.upgradeall.ui.base.listdialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import net.xzos.upgradeall.databinding.RecyclerlistContentBinding

open class ListDialog(
        private val title: Any?,
        private val adapter: DialogListAdapter<*, *, *>
) : DialogFragment() {

    fun show(supportFragmentManager: FragmentManager) {
        super.show(supportFragmentManager, TAG)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val contentBinding = RecyclerlistContentBinding.inflate(layoutInflater)

        contentBinding.apply {
            if (adapter.itemCount > 0) {
                rvList.adapter = adapter
            } else {
                vfContainer.displayedChild = 1
                tvEmpty.visibility = View.VISIBLE
            }
        }

        return AlertDialog.Builder(requireContext()).apply {
            setView(contentBinding.root)
            title?.let {
                if (title is CharSequence)
                    setTitle(title)
                else if (title is Int)
                    setTitle(title)
            }
        }.create()
    }

    companion object {
        private const val TAG = "ListDialog"
    }
}