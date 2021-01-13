package net.xzos.upgradeall.ui.rss

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityRssBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

class RssActivity : AppBarActivity() {

    private lateinit var binding: ActivityRssBinding

    override fun initBinding(): View {
        binding = ActivityRssBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {

    }
}