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
        override val sAdapter: DialogListAdapter<*, *, *>
) : DialogFragment(), ListDialogPart {

    protected lateinit var binding: RecyclerlistContentBinding

    fun show(supportFragmentManager: FragmentManager) {
        super.show(supportFragmentManager, TAG)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = RecyclerlistContentBinding.inflate(layoutInflater)
        binding.srlContainer.apply {
            isRefreshing = false
            isEnabled = false
        }
        renewListView(binding)
        return initDialog(binding.root)
    }

    fun initDialog(view: View): Dialog {
        return AlertDialog.Builder(requireContext()).apply {
            setView(view)
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