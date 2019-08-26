package net.xzos.UpgradeAll.ui.viewmodels.componnent;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;

import androidx.core.content.res.TypedArrayUtils;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceManager;

import net.xzos.UpgradeAll.R;
import net.xzos.UpgradeAll.application.MyApplication;

public class EditIntPreference extends EditTextPreference {

    private static final String TAG = "EditIntPreference";

    private String mKey;

    @Override
    public void setKey(String key) {
        super.setKey(key);
        mKey = key;
    }

    @SuppressLint("PrivateResource")
    public EditIntPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        final TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.Preference, defStyleAttr, defStyleRes);
        mKey = TypedArrayUtils.getString(a, R.styleable.Preference_key,
                R.styleable.Preference_android_key);
    }

    public EditIntPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public EditIntPreference(Context context, AttributeSet attrs) {
        this(context, attrs, TypedArrayUtils.getAttr(context, R.attr.editTextPreferenceStyle,
                android.R.attr.editTextPreferenceStyle));
    }

    public EditIntPreference(Context context) {
        this(context, null);
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int defaultReturnIntValue;
        if (defaultReturnValue == null) {
            defaultReturnIntValue = getInt(mKey, 0);  // 已设置值, （修复属性）并返回设置值
        } else
            defaultReturnIntValue = Integer.valueOf(defaultReturnValue);  // 未设置值，获取默认值
        return String.valueOf(getPersistedInt(defaultReturnIntValue));
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.valueOf(value));
    }

    public static int getInt(String key, int defaultReturnValue) {
        int returnValue;
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MyApplication.getContext());
        try {
            returnValue = sharedPref.getInt(key, defaultReturnValue);
        } catch (ClassCastException e) {
            returnValue = Integer.parseInt(sharedPref.getString("sync_time", String.valueOf(defaultReturnValue)));
            sharedPref.edit().remove(key).apply();
            sharedPref.edit().putInt(key, returnValue).apply();
            Log.e(TAG, "getInt: 已自动更改设置值属性");
            // TODO: 三个大版本后移除。当前版本：0.1.0，移除版本：0.1.3
        }
        return returnValue;
    }
}
