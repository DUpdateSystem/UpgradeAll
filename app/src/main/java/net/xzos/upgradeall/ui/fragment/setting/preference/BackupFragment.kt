package net.xzos.upgradeall.ui.fragment.setting.preference

import android.os.Bundle
import android.text.InputType
import android.view.View
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.core.data.backup.BackupManager
import net.xzos.upgradeall.core.data.backup.CloudBackupManager
import net.xzos.upgradeall.core.data.backup.RestoreManager
import net.xzos.upgradeall.ui.activity.file_pref.SaveFileActivity
import net.xzos.upgradeall.ui.activity.file_pref.SelectFileActivity
import net.xzos.upgradeall.ui.viewmodels.dialog.CloudBackupListDialog
import net.xzos.upgradeall.utils.MiscellaneousUtils


class BackupFragment : PrefFragment(R.xml.preferences_backup) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hidePassword()
        setLocalBackup()
        setCloudBackup()
    }

    private fun hidePassword() {
        findPreference<EditTextPreference>("webdav_password")?.let { editTextPreference ->
            editTextPreference.summaryProvider = Preference.SummaryProvider<EditTextPreference> { preference ->
                "*".repeat(preference.text?.length ?: return@SummaryProvider "")
            }

            editTextPreference.setOnBindEditTextListener { editText ->
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }
    }

    private fun setLocalBackup() {
        val backupPreference: Preference = findPreference("BACKUP")!!
        backupPreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                MiscellaneousUtils.showToast(R.string.backup_running)
                val backupFileBytes = BackupManager.mkZipFileBytes()
                val context = this@BackupFragment.context
                if (backupFileBytes != null && context != null) {
                    SaveFileActivity.newInstance(BackupManager.newFileName(), "application/zip", backupFileBytes, context)
                }
                MiscellaneousUtils.showToast(R.string.backup_stop)
            }
            false
        }

        val restorePreference: Preference = findPreference("RESTORE")!!
        restorePreference.setOnPreferenceClickListener {
            this.context?.let { context ->
                GlobalScope.launch {
                    SelectFileActivity.newInstance(context, "application/zip")?.let { uri ->
                        MiscellaneousUtils.showToast(R.string.restore_running)
                        @Suppress("BlockingMethodInNonBlockingContext")
                        context.contentResolver.openInputStream(uri)?.let { iStream ->
                            val bytes = iStream.readBytes()
                            RestoreManager.parseZip(bytes)
                        }
                        MiscellaneousUtils.showToast(R.string.restore_stop)
                    }
                }
            }
            false
        }
    }

    private fun setCloudBackup() {
        val backupPreference: Preference = findPreference("WEBDAV_BACKUP")!!
        val restorePreference: Preference = findPreference("WEBDAV_RESTORE")!!
        backupPreference.setOnPreferenceClickListener {
            CloudBackupManager().backup()
            false
        }
        restorePreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                val cloudBackupManager = CloudBackupManager()
                val fileNameList = cloudBackupManager.getBackupFileList() ?: return@launch
                context?.let {
                    withContext(Dispatchers.Main) {
                        CloudBackupListDialog.show(it, fileNameList, fun(position) {
                            GlobalScope.launch {
                                cloudBackupManager.restoreBackup(fileNameList[position])
                            }
                        })
                    }
                }
            }
            false
        }
    }
}
