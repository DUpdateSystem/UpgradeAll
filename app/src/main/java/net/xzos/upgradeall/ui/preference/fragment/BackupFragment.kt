package net.xzos.upgradeall.ui.preference.fragment

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.xzos.upgradeall.R
import net.xzos.upgradeall.app.backup.manager.BackupManager
import net.xzos.upgradeall.app.backup.manager.CloudBackupManager
import net.xzos.upgradeall.app.backup.manager.RestoreManager
import net.xzos.upgradeall.app.backup.manager.data.WebDavConfig
import net.xzos.upgradeall.core.androidutils.ToastUtil
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.restore.RestoreActivity
import net.xzos.upgradeall.ui.utils.dialog.CloudBackupListDialog
import net.xzos.upgradeall.ui.utils.file_pref.SaveFileActivity
import net.xzos.upgradeall.ui.utils.file_pref.SelectFileActivity


class BackupFragment : PrefFragment(R.xml.preferences_backup) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hidePassword()
        setLocalBackup()
        setCloudBackup()
    }

    private fun hidePassword() {
        findPreference<EditTextPreference>("webdav_password")?.let { editTextPreference ->
            editTextPreference.summaryProvider =
                Preference.SummaryProvider<EditTextPreference> { preference ->
                    "*".repeat(preference.text?.length ?: return@SummaryProvider "")
                }

            editTextPreference.setOnBindEditTextListener { editText ->
                editText.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
        }
    }

    private fun setLocalBackup() {
        val backupPreference: Preference = findPreference("BACKUP")!!
        backupPreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                ToastUtil.showText(requireContext(), R.string.backup_running)
                val backupFileBytes = BackupManager.mkZipFileBytes()
                if (backupFileBytes != null) {
                    SaveFileActivity.newInstance(
                        BackupManager.newFileName(),
                        "application/zip",
                        backupFileBytes,
                        requireContext()
                    )
                }
                ToastUtil.showText(requireContext(), R.string.backup_stop)
            }
            false
        }

        val restorePreference: Preference = findPreference("RESTORE")!!
        restorePreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                SelectFileActivity.newInstance(requireContext(), "application/zip")?.let { bytes ->
                    ToastUtil.showText(requireContext(), R.string.restore_running)
                    launch { RestoreManager.parseZip(bytes) }
                    startActivity(Intent(it.context, RestoreActivity::class.java))
                    ToastUtil.showText(requireContext(), R.string.restore_stop, Toast.LENGTH_LONG)
                }
            }
            false
        }
    }

    private fun getCloudBackupManager() = CloudBackupManager(
        WebDavConfig(
            PreferencesMap.webdav_url,
            PreferencesMap.webdav_path,
            PreferencesMap.webdav_username,
            PreferencesMap.webdav_password
        )
    )

    private fun setCloudBackup() {
        val backupPreference: Preference = findPreference("WEBDAV_BACKUP")!!
        val restorePreference: Preference = findPreference("WEBDAV_RESTORE")!!
        backupPreference.setOnPreferenceClickListener {
            getCloudBackupManager().backup(
                { ToastUtil.showText(requireContext(), R.string.backup_running) },
                { ToastUtil.showText(requireContext(), R.string.backup_stop) },
                { ToastUtil.showText(requireContext(), it.message.toString(), Toast.LENGTH_LONG) },
            )
            false
        }
        restorePreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                val cloudBackupManager = getCloudBackupManager()
                val fileNameList = cloudBackupManager.getBackupFileList() ?: return@launch
                withContext(Dispatchers.Main) {
                    CloudBackupListDialog.show(requireContext(), fileNameList, fun(position) {
                        GlobalScope.launch {
                            cloudBackupManager.restoreBackup(
                                fileNameList[position],
                                { ToastUtil.showText(requireContext(), R.string.restore_running) },
                                { ToastUtil.showText(requireContext(), R.string.restore_stop) },
                                {
                                    ToastUtil.showText(
                                        requireContext(),
                                        it.message.toString(), Toast.LENGTH_LONG
                                    )
                                },
                            )
                        }
                    })
                }
            }
            false
        }
    }
}
