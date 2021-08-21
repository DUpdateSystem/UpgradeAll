package net.xzos.upgradeall.ui.home.adapter

import android.content.Intent
import android.widget.ImageButton
import com.chad.library.adapter.base.BaseMultiItemQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import net.xzos.upgradeall.R
import net.xzos.upgradeall.ui.log.LogActivity
import net.xzos.upgradeall.ui.preference.SettingsActivity
import net.xzos.upgradeall.core.androidutils.ToastUtil

class HomeModuleAdapter : BaseMultiItemQuickAdapter<HomeModuleBean, BaseViewHolder>() {

    init {
        addItemType(STYLE_CARD, R.layout.item_home_module_card)
        addItemType(STYLE_NON_CARD, R.layout.item_home_module_non_card)
        addItemType(STYLE_SIMPLE_CARD, R.layout.layout_home_simple_menu)
    }

    override fun convert(holder: BaseViewHolder, item: HomeModuleBean) {
        if (item.itemType == STYLE_CARD || item.itemType == STYLE_NON_CARD) {
            holder.setImageResource(R.id.iv_icon, item.iconRes)
            holder.setText(R.id.tv_title, item.titleRes)
            holder.itemView.setOnClickListener(item.clickListener)
        } else if (item.itemType == STYLE_SIMPLE_CARD) {
            holder.getView<ImageButton>(R.id.btn_log).setOnClickListener {
                context.startActivity(Intent(context, LogActivity::class.java))
            }
            holder.getView<ImageButton>(R.id.btn_settings).setOnClickListener {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
            holder.getView<ImageButton>(R.id.btn_about).setOnClickListener {
                ToastUtil.makeText(R.string.home_about)
            }
        }
    }
}