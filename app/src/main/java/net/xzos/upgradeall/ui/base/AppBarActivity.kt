package net.xzos.upgradeall.ui.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.absinthe.libraries.utils.utils.UiUtils

abstract class AppBarActivity : BaseActivity() {

    abstract fun initBinding(): View
    abstract fun getAppBar(): Toolbar
    abstract fun initView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(initBinding())
        setSupportActionBar(getAppBar())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        getAppBar().layoutParams = (getAppBar().layoutParams as ViewGroup.MarginLayoutParams).apply {
            setMargins(marginStart, topMargin + UiUtils.getStatusBarHeight(), marginEnd, bottomMargin)
        }
        initView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}