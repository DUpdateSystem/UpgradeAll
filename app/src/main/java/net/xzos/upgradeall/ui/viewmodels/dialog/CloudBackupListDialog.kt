package net.xzos.upgradeall.ui.viewmodels.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import net.xzos.upgradeall.databinding.ListContentBinding

class CloudBackupListDialog private constructor(
        context: Context,
        private val fileNameList: List<String>,
        private val clickFun: (position: Int) -> Unit
) : BottomSheetDialog(context) {
    private lateinit var binding: ListContentBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ListContentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        super.onCreate(savedInstanceState)
        val list = binding.list
        list.setOnItemClickListener { _, _, position, _ ->
            clickFun(position)
            dismiss()
        }
        if (fileNameList.isNotEmpty())
            binding.list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, fileNameList)
        else
            binding.emptyPlaceHolderTextView.visibility = View.VISIBLE
    }

    companion object {
        fun show(context: Context, fileNameList: List<String>,
                 clickFun: (position: Int) -> Unit
        ) {
            CloudBackupListDialog(context, fileNameList, clickFun).show()
        }
    }
}
