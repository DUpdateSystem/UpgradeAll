package net.xzos.upgradeall.ui.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.os.FileUtils
import android.view.View
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.activity.file_pref.SelectDirActivity
import net.xzos.upgradeall.utils.FileUtil
import java.io.File


open class PrefFragment internal constructor(@XmlRes private val preferencesResId: Int) : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(preferencesResId, rootKey)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences
                .registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences
                .unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        //Your Code
    }
}

class InstallationFragment : PrefFragment(R.xml.preferences_update)
class UpdatesPrefFragment : PrefFragment(R.xml.preferences_installation)
class DownloaderFragment : PrefFragment(R.xml.preferences_downloader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDownloadPath()
        setCleanDownloadPath()
    }

    private fun setDownloadPath() {
        val junkPreference: Preference = findPreference("SELECT_DOWNLOAD_PATH")!!
        junkPreference.summary = PreferencesMap.download_path
        junkPreference.setOnPreferenceClickListener { preference ->
            GlobalScope.launch {
                context?.run {
                    val treeUri = SelectDirActivity.newInstance(this)
                    withContext(Dispatchers.Main) {
                        preference.summary = treeUri.toString()
                    }
                }
            }
            false
        }
    }

    private fun setCleanDownloadPath() {
        val junkPreference: Preference = findPreference("CLEAN_DOWNLOAD_DIR")!!
        junkPreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                FileUtil.DOWNLOAD_CACHE_DIR.delete()
            }
            false
        }
    }
}
