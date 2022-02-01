package net.xzos.upgradeall.ui.preference

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceFragmentCompat
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.databinding.SettingsActivityBinding
import net.xzos.upgradeall.ui.base.AppBarActivity


class SettingsActivity : AppBarActivity() {

    private lateinit var binding: SettingsActivityBinding

    override fun initBinding(): View {
        binding = SettingsActivityBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun getAppBar(): Toolbar = binding.appbar.toolbar

    override fun initView() {
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences_main, rootKey)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onPause() {
        super.onPause()
        PreferencesMap.async()
    }
}