package net.xzos.upgradeall.ui.viewmodels.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.list_content.*
import net.xzos.upgradeall.R

class CloudBackupListDialog private constructor(
        context: Context,
        private val fileNameList: List<String>,
        private val clickFun: (position: Int) -> Unit
) : BottomSheetDialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        setContentView(R.layout.list_content)
        super.onCreate(savedInstanceState)
        placeholderLayout.visibility = View.VISIBLE
        // 下载文件
        list.setOnItemClickListener { _, _, position, _ ->
            clickFun(position)
            dismiss()
        }
        if (fileNameList.isNotEmpty())
            list.adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, fileNameList)
        else
            emptyPlaceHolderTextView.visibility = View.VISIBLE
        placeholderLayout.visibility = View.GONE
    }

    companion object {
        fun show(context: Context, fileNameList: List<String>,
                 clickFun: (position: Int) -> Unit
        ) {
            CloudBackupListDialog(context, fileNameList, clickFun).show()
        }
    }
}
