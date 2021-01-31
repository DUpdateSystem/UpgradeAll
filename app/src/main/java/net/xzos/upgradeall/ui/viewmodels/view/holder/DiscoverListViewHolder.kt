package net.xzos.upgradeall.ui.viewmodels.view.holder

import android.content.res.ColorStateList
import android.view.ViewGroup
import com.absinthe.libraries.utils.extensions.layoutInflater
import com.google.android.material.chip.Chip
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ItemDiscoverAppBinding
import net.xzos.upgradeall.ui.viewmodels.view.CloudConfigListItemView
import net.xzos.upgradeall.ui.viewmodels.view.ListItemView
import net.xzos.upgradeall.utils.UxUtils
import java.util.*

class DiscoverListViewHolder(private val binding: ItemDiscoverAppBinding)
    : RecyclerViewHolder<CloudConfigListItemView>(binding) {

    override fun doBind(itemView: CloudConfigListItemView) {
        binding.item = itemView
        val context = binding.root.context
        val chipGroup = binding.chipGroup
        chipGroup.removeAllViewsInLayout()
        binding.ivIcon.apply {
            val firstChar = itemView.name.toCharArray().find { !ListItemView.pattern.matcher(it.toString()).find() }
            text = firstChar.toString().toUpperCase(Locale.ROOT)
            backgroundTintList = ColorStateList.valueOf(UxUtils.getRandomColor())
        }
        itemView.type.let {
            val typeChip = (context.layoutInflater.inflate(R.layout.single_chip_layout, chipGroup, false) as Chip).apply {
                setText(it)
                val iconRes = when (it) {
                    R.string.android_app -> R.drawable.ic_android_placeholder
                    R.string.magisk_module -> R.drawable.ic_home_magisk_module
                    R.string.shell -> R.drawable.ic_type_shell
                    R.string.shell_root -> R.drawable.ic_type_shell
                    else -> 0
                }
                setChipIconResource(iconRes)
            }
            chipGroup.addView(typeChip, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        itemView.hubName.let {
            val hubChip = (context.layoutInflater.inflate(R.layout.single_chip_layout, chipGroup, false) as Chip).apply {
                text = it
                val iconRes = when (it) {
                    "GitHub" -> R.drawable.ic_hub_github
                    "Google Play" -> R.drawable.ic_hub_google_play
                    "酷安" -> R.drawable.ic_hub_coolapk
                    else -> R.drawable.ic_hub_website
                }
                setChipIconResource(iconRes)
            }
            chipGroup.addView(hubChip, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}