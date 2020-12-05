package net.xzos.upgradeall.ui.apps

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityAppsBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

class AppsActivity : AppBarActivity() {

    private lateinit var binding: ActivityAppsBinding

    override fun initBinding(): View {
        binding = ActivityAppsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {

    }
}