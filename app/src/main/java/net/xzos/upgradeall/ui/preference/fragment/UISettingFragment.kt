package net.xzos.upgradeall.ui.preference.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.xzos.upgradeall.R
import net.xzos.upgradeall.data.PreferencesMap
import net.xzos.upgradeall.ui.base.selectlistdialog.SelectItem
import net.xzos.upgradeall.ui.base.selectlistdialog.SelectListDialog
import net.xzos.upgradeall.ui.home.MainActivity
import net.xzos.upgradeall.utils.MiscellaneousUtils


class UISettingFragment : PrefFragment(R.xml.preferences_ui), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHomeBottomList()
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
        MiscellaneousUtils.showToast(R.string.plz_restart)
    }

    private fun setHomeBottomList() {
        val customHomeListPreference: Preference = findPreference("CUSTOM_HOME_BOTTOM_LIST")!!
        customHomeListPreference.setOnPreferenceClickListener {
            GlobalScope.launch {
                val homeBottomMap = PreferencesMap.home_bottom_map
                val dataList = SelectListDialog.showDialog(
                        homeBottomMap.map { SelectItem(getString(MainActivity.getBeanName(it.key)!!), it.key, it.value) },
                        activity?.supportFragmentManager!!, R.string.home_bottom_queue_setting
                )
                PreferencesMap.home_bottom_map = dataList.map { it.id to it.enableObservable.enable }.toMap()
            }
            false
        }
    }
}
