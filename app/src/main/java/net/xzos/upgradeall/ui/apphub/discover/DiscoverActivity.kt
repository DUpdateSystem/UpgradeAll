package net.xzos.upgradeall.ui.apphub.discover

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityDiscoverBinding
import net.xzos.upgradeall.ui.base.AppBarActivity


class DiscoverActivity : AppBarActivity() {

    private lateinit var binding: ActivityDiscoverBinding

    override fun initBinding(): View {
        binding = ActivityDiscoverBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {

    }
}