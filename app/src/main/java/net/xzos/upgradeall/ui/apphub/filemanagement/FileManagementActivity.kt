package net.xzos.upgradeall.ui.apphub.filemanagement

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import net.xzos.upgradeall.ui.apphub.HubListActivity

class FileManagementActivity : HubListActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewpager.apply {
            adapter = object : FragmentStateAdapter(this@FileManagementActivity) {
                override fun getItemCount(): Int {
                    return 1
                }

                override fun createFragment(position: Int): Fragment {
                    return FileHubListFragment()
                }
            }
        }
    }
}