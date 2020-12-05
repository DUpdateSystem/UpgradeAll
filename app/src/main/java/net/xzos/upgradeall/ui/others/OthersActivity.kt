package net.xzos.upgradeall.ui.others

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityOthersBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

class OthersActivity : AppBarActivity() {

    private lateinit var binding: ActivityOthersBinding

    override fun initBinding(): View {
        binding = ActivityOthersBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {

    }
}