package net.xzos.upgradeall.ui.discover

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.android.material.chip.Chip
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ItemDiscoverAppBinding
import net.xzos.upgradeall.ui.base.recycleview.RecyclerViewHolder

class DiscoverListViewHolder(private val binding: ItemDiscoverAppBinding)
    : RecyclerViewHolder<DiscoverListItemView, DiscoverListItemHandler, ItemDiscoverAppBinding>(binding, binding) {

    override fun setHandler(handler: DiscoverListItemHandler) {
        binding.handler = handler
    }

    override fun doBind(itemView: DiscoverListItemView) {
        binding.item = itemView
        val context = binding.root.context
        val layoutInflater = LayoutInflater.from(context)
        val chipGroup = binding.chipGroup
        chipGroup.removeAllViewsInLayout()
        itemView.type.let {
            val typeChip = (layoutInflater.inflate(R.layout.single_chip_layout, chipGroup, false) as Chip).apply {
                setText(it)
                val iconRes = when (it) {
                    R.string.android_app -> R.drawable.ic_android_placeholder
                    R.string.magisk_module -> R.drawable.ic_home_magisk_module
                    R.string.shell -> R.drawable.ic_type_shell
                    R.string.shell_root -> R.drawable.ic_type_shell
                    else -> R.drawable.ic_url
                }
                setChipIconResource(iconRes)
            }
            chipGroup.addView(typeChip, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        itemView.hubName.let {
            val hubChip = (layoutInflater.inflate(R.layout.single_chip_layout, chipGroup, false) as Chip).apply {
                text = it
                val iconRes = when (it.lowercase()) {
                    "github" -> R.drawable.ic_hub_github
                    "google play" -> R.drawable.ic_hub_google_play
                    "酷安" -> R.drawable.ic_hub_coolapk
                    "gitlab" -> R.drawable.ic_hub_gitlab
                    "f-droid" -> R.drawable.ic_hub_fdroid
                    else -> R.drawable.ic_hub_website
                }
                setChipIconResource(iconRes)
            }
            chipGroup.addView(hubChip, -1, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
    }
}