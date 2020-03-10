package net.xzos.upgradeall.ui.viewmodels.adapters

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.list_item_search.view.*
import net.xzos.dupdatesystem.core.data.config.AppType
import net.xzos.dupdatesystem.core.data_manager.utils.SearchUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.IconInfo
import net.xzos.upgradeall.utils.IconPalette

class SearchResultItemAdapter(context: Context, searchInfoList: List<SearchUtils.SearchInfo>)
    : ArrayAdapter<SearchUtils.SearchInfo>(context, R.layout.list_item_search, searchInfoList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (convertView
                ?: LayoutInflater.from(context).inflate(R.layout.list_item_search, parent, false)
                ).also { view ->
            getItem(position)?.let { searchInfo ->
                val targetId = searchInfo.matchInfo.id
                IconPalette.loadAppIconView(view.appIconImageView, iconInfo = IconInfo(app_package = targetId))
                view.nameTextView.text = searchInfo.matchInfo.name
                view.searchTypeTextView.setText(
                        when (searchInfo.targetSort) {
                            AppType.androidApp -> R.string.app
                            AppType.androidMagiskModule -> R.string.magisk_module
                            else -> R.string.app
                        }
                )
                view.searchTextView.let {
                    it.maxLines = Integer.MAX_VALUE
                    it.text = targetId
                    searchInfo.matchInfo.matchList.let { matchList ->
                        if (matchList.isNotEmpty()) {
                            var text = it.text.toString() + "\n" + it.context.getString(R.string.split_line)
                            for (matchString in matchList) {
                                text = text + "\n" + matchString.matchString
                            }
                            it.text = text
                        }
                    }
                    it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
                }
            }
        }
    }
}
