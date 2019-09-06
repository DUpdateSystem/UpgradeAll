package net.xzos.upgradeAll.ui.viewmodels.componnent

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log

import androidx.core.content.res.TypedArrayUtils
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceManager

import net.xzos.upgradeAll.R
import net.xzos.upgradeAll.application.MyApplication

class EditIntPreference @SuppressLint("PrivateResource")
constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : EditTextPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var mKey: String? = null

    override fun setKey(key: String) {
        super.setKey(key)
        mKey = key
    }

    init {
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.Preference, defStyleAttr, defStyleRes)
        mKey = TypedArrayUtils.getString(a, R.styleable.Preference_key,
                R.styleable.Preference_android_key)
    }

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle,
            android.R.attr.editTextPreferenceStyle)) : this(context, attrs, defStyleAttr, 0)

    override fun getPersistedString(defaultReturnValue: String?): String {
        val defaultReturnIntValue: Int
        if (defaultReturnValue == null) {
            defaultReturnIntValue = getInt(mKey, 0)  // 已设置值, （修复属性）并返回设置值
        } else
            defaultReturnIntValue = Integer.valueOf(defaultReturnValue)  // 未设置值，获取默认值
        return getPersistedInt(defaultReturnIntValue).toString()
    }

    override fun persistString(value: String): Boolean {
        try {
            return persistInt(Integer.valueOf(value))
        } catch (ignored: NumberFormatException) {
            return true
        }

    }

    companion object {

        private val TAG = "EditIntPreference"

        fun getInt(key: String?, defaultReturnValue: Int): Int {
            var returnValue: Int
            val sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.context)
            try {
                returnValue = sharedPref.getInt(key, defaultReturnValue)
            } catch (e: ClassCastException) {
                returnValue = Integer.parseInt(sharedPref.getString("sync_time", defaultReturnValue.toString())!!)
                sharedPref.edit().remove(key).apply()
                sharedPref.edit().putInt(key, returnValue).apply()
                Log.e(TAG, "getInt: 已自动更改设置值属性")
                // 数据类型兼容功能
            }

            return returnValue
        }
    }
}
