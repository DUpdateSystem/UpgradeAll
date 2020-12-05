package net.xzos.upgradeall.ui.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import com.absinthe.libraries.utils.extensions.addPaddingTop

abstract class AppBarActivity : BaseActivity() {

    abstract fun initBinding(): View
    abstract fun getAppBar(): Toolbar
    abstract fun initView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(initBinding())
        setSupportActionBar(getAppBar())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (getAppBar().parent as View).addPaddingTop(getStatusBarHeight())
        initView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }
}