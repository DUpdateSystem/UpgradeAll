package net.xzos.upgradeall.ui.viewmodels.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.cardview_content.view.*
import net.xzos.dupdatesystem.core.data_manager.utils.SearchUtils
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.IconInfo
import net.xzos.upgradeall.utils.IconPalette


class SearchResultItemAdapter(
        context: Context, searchInfoList: List<SearchUtils.SearchInfo>,
        private val resource: Int = R.layout.cardview_content
) : ArrayAdapter<SearchUtils.SearchInfo>(context, R.layout.cardview_content, searchInfoList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return (convertView
                ?: LayoutInflater.from(context).inflate(resource, parent, false)
                ).also { view ->
            val searchInfo = getItem(position)
            val targetId = searchInfo?.matchInfo?.id
            IconPalette.loadAppIconView(view.appIconImageView, iconInfo = IconInfo(app_package = targetId))
            view.nameTextView.text = searchInfo?.matchInfo?.name
            view.versioningTextView.text = searchInfo?.targetSort
            view.descTextView.let {
                it.maxLines = Integer.MAX_VALUE
                it.text = targetId
                searchInfo?.matchInfo?.matchList?.let { matchList ->
                    @SuppressLint("SetTextI18n")
                    if (matchList.isNotEmpty()) {
                        it.text = it.text.toString() + "\n" + it.context.getString(R.string.split_line)
                        for (matchString in matchList) {
                            it.text = it.text.toString() + "\n" + matchString.matchString
                        }
                    }
                }
                it.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12F)
                with(it.parent as LinearLayout) {
                    this.viewTreeObserver.addOnGlobalLayoutListener {
                        this.layoutParams = (this.layoutParams as RelativeLayout.LayoutParams).apply {
                            this.marginEnd = 0
                        }
                        this.invalidate()
                    }
                }
            }
        }
    }
}