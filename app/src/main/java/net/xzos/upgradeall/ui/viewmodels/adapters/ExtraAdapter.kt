package net.xzos.upgradeall.ui.viewmodels.adapters

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.gson.ExtraItem

class ExtraAdapter :BaseQuickAdapter<ExtraItem, BaseViewHolder>(R.layout.item_extra) {

    init {
        addChildClickViewIds(R.id.ib_delete)
    }

    override fun convert(holder: BaseViewHolder, item: ExtraItem) {
        holder.getView<EditText>(R.id.et_key).apply {
            if (item.key.isNotEmpty()) {
                setText(item.key)
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable) {
                    item.key = text.toString()
                }
                override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
            })
        }

        holder.getView<EditText>(R.id.et_value).apply {
            if (item.value.isNotEmpty()) {
                setText(item.value)
            }
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(text: Editable) {
                    item.value = text.toString()
                }
                override fun beforeTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence, p1: Int, p2: Int, p3: Int) {}
            })
        }
    }
}