package net.xzos.upgradeall.ui.base

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar

abstract class AppBarActivity : BaseActivity() {

    abstract fun initBinding(): View
    abstract fun getAppBar(): Toolbar
    abstract fun initView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(initBinding())
        setSupportActionBar(getAppBar())
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        initView()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}