package net.xzos.upgradeall.ui.fragment.preference

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.annotation.XmlRes
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.data.PreferencesMap.DOWNLOAD_MAX_TASK_NUM_KEY
import net.xzos.upgradeall.data.PreferencesMap.DOWNLOAD_THREAD_NUM_KEY
import net.xzos.upgradeall.ui.activity.file_pref.SelectDirActivity
import net.xzos.upgradeall.utils.FileUtil


open class PrefFragment internal constructor(@XmlRes private val preferencesResId: Int)
    : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
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
    }
}

class UpdatesPrefFragment : PrefFragment(R.xml.preferences_update)
class InstallationFragment : PrefFragment(R.xml.preferences_installation)
class DownloaderFragment : PrefFragment(R.xml.preferences_downloader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSeekBar()
        setDownloadPath()
        setCleanDownloadPath()
    }

    private fun setSeekBar() {
        val downloadThreadNumKeyPreference: SeekBarPreference = findPreference(DOWNLOAD_THREAD_NUM_KEY)!!
        downloadThreadNumKeyPreference.min = 1
        val downloadMaxTaskNumKeyPreference: SeekBarPreference = findPreference(DOWNLOAD_MAX_TASK_NUM_KEY)!!
        downloadMaxTaskNumKeyPreference.min = 1
    }

    private fun setDownloadPath() {
        val downloadPathPreference: Preference = findPreference("SELECT_DOWNLOAD_PATH")!!
        downloadPathPreference.summary = PreferencesMap.user_download_path
        downloadPathPreference.setOnPreferenceClickListener { preference ->
            GlobalScope.launch {
                context?.run {
                    val treeUri = SelectDirActivity.newInstance(this) ?: return@launch
                    PreferencesMap.user_download_path = treeUri.toString()
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
