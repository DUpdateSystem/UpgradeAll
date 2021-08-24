package net.xzos.upgradeall.ui.preference.fragment

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import androidx.preference.SwitchPreferenceCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.utils.file_pref.SelectDirActivity
import net.xzos.upgradeall.utils.DOWNLOAD_CACHE_DIR

class DownloaderFragment : PrefFragment(R.xml.preferences_downloader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSeekBar()
        setDownloadPath()
        setCleanDownloadPath()
    }

    private fun setSeekBar() {
        findPreference<SeekBarPreference>(PreferencesMap.DOWNLOAD_THREAD_NUM_KEY)!!.min = 1
        findPreference<SeekBarPreference>(PreferencesMap.DOWNLOAD_MAX_TASK_NUM_KEY)!!.min = 1
        findPreference<SeekBarPreference>(PreferencesMap.DOWNLOAD_AUTO_RETRY_MAX_ATTEMPTS_KEY)!!.min = 1
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
                        findPreference<SwitchPreferenceCompat>("auto_dump_download_file")!!.isEnabled = true
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
                DOWNLOAD_CACHE_DIR.delete()
            }
            false
        }
    }
}
