package net.xzos.upgradeall.ui.fragment.setting.preference

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.SeekBarPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.activity.file_pref.SelectDirActivity
import net.xzos.upgradeall.utils.file.FileUtil

class DownloaderFragment : PrefFragment(R.xml.preferences_downloader) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setSeekBar()
        setDownloadPath()
        setCleanDownloadPath()
    }

    private fun setSeekBar() {
        val downloadThreadNumKeyPreference: SeekBarPreference = findPreference(PreferencesMap.DOWNLOAD_THREAD_NUM_KEY)!!
        downloadThreadNumKeyPreference.min = 1
        val downloadMaxTaskNumKeyPreference: SeekBarPreference = findPreference(PreferencesMap.DOWNLOAD_MAX_TASK_NUM_KEY)!!
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
