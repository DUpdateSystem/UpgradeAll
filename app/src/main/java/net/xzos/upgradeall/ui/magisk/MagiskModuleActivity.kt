package net.xzos.upgradeall.ui.magisk

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityMagiskModuleBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

class MagiskModuleActivity : AppBarActivity() {

    private lateinit var binding: ActivityMagiskModuleBinding

    override fun initBinding(): View {
        binding = ActivityMagiskModuleBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {

    }
}