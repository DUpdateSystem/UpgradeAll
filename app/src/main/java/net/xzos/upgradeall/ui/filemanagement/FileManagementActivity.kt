package net.xzos.upgradeall.ui.filemanagement

import android.view.View
import androidx.appcompat.widget.Toolbar
import net.xzos.upgradeall.databinding.ActivityFileManagementBinding
import net.xzos.upgradeall.ui.base.AppBarActivity

class FileManagementActivity : AppBarActivity() {

    private lateinit var binding: ActivityFileManagementBinding

    override fun initBinding(): View {
        binding = ActivityFileManagementBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {

    }
}