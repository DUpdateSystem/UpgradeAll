package net.xzos.upgradeall.ui.apphub

import android.os.Bundle
import android.view.View

open class HubListActivity : AppHubActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.tabLayoutContainer.visibility = View.GONE
    }
}
