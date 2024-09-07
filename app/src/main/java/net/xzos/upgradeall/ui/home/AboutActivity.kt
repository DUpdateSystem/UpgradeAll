package net.xzos.upgradeall.ui.home

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.R
import net.xzos.upgradeall.databinding.ActivityAboutBinding
import net.xzos.upgradeall.ui.base.AppBarActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils

class AboutActivity : AppBarActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun initBinding(): View =
        ActivityAboutBinding.inflate(layoutInflater).apply {
            binding = this
        }.root

    override fun getAppBar(): Toolbar = binding.appbar.toolbar
    override fun initView() {
        binding.layoutWebsiteCard.apply {
            ivIcon.setImageResource(R.drawable.ic_url)
            tsTitle.setText(getString(R.string.official_website))
            val url = "https://up-a.org/"
            tvSubtitle.text = url
            layoutCard.setOnClickListener {
                MiscellaneousUtils.accessByBrowser(url, layoutCard.context)
            }
        }
        binding.layoutDonateCard.apply {
            tsTitle.setText(getString(R.string.donate))
            val url = "https://afdian.net/a/inkflaw"
            tvSubtitle.text = url
            layoutCard.setOnClickListener {
                MiscellaneousUtils.accessByBrowser(url, layoutCard.context)
            }
        }
    }
}