package net.xzos.upgradeall.ui.fragment.setting.preference

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.backup.BackupManager
import net.xzos.upgradeall.data.backup.RestoreManager
import net.xzos.upgradeall.ui.activity.file_pref.SaveFileActivity
import net.xzos.upgradeall.ui.activity.file_pref.SelectFileActivity

class BackupFragment : PrefFragment(R.xml.preferences_backup) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLocalBackup()
    }

    private fun setLocalBackup() {
        val backupPreference: Preference = findPreference("BACKUP")!!
        backupPreference.setOnPreferenceClickListener {
            val backupFileBytes = BackupManager().mkZipFileBytes()
            val context = this.context
            if (backupFileBytes != null && context != null) {
                GlobalScope.launch {
                    SaveFileActivity.newInstance("UpgradeAll_Backup.zip", "application/zip", backupFileBytes, context)
                }
            }
            false
        }

        val restorePreference: Preference = findPreference("RESTORE")!!
        restorePreference.setOnPreferenceClickListener {
            this.context?.let { context ->
                GlobalScope.launch {
                    SelectFileActivity.newInstance(context, "application/zip")?.let { uri ->
                        @Suppress("BlockingMethodInNonBlockingContext")
                        context.contentResolver.openInputStream(uri)?.let { iStream ->
                            val bytes = iStream.readBytes()
                            RestoreManager().parseZip(bytes)
                        }
                    }
                }
            }
            false
        }
    }
}
