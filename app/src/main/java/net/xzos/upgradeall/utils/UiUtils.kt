package net.xzos.upgradeall.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.provider.Settings
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import androidx.annotation.*
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.TintTypedArray
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import net.xzos.upgradeall.R
import net.xzos.upgradeall.utils.DimensionKtx.dp


object UiUtils {
    fun getStatusBarHeight(): Int {
        val resources: Resources = Resources.getSystem()
        val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
        return resources.getDimensionPixelSize(resourceId)
    }

    fun getNavBarHeight(contentResolver: ContentResolver): Int {
        //Full screen adaption
        if (Settings.Global.getInt(contentResolver, "force_fsg_nav_bar", 0) != 0) {
            return 20.dp
        }

        val res = Resources.getSystem()
        val resourceId = res.getIdentifier("navigation_bar_height", "dimen", "android")

        return if (resourceId != 0) {
            res.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    fun createItemBackgroundMd2(context: Context): Drawable =
            createItemShapeDrawableMd2(
                    context.getColorStateListCompat(R.color.mtrl_navigation_item_background_color),
                    context
            )

    fun createItemForegroundMd2(context: Context): Drawable {
        val mask = createItemShapeDrawableMd2(ColorStateList.valueOf(Color.WHITE), context)
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(R.attr.colorControlHighlight, typedValue, true)
        @ColorInt val color = typedValue.data
        return RippleDrawable(ColorStateList.valueOf(color), null, mask)
    }

    private fun createItemShapeDrawableMd2(
            fillColor: ColorStateList,
            context: Context
    ): Drawable {
        val materialShapeDrawable = MaterialShapeDrawable(
                ShapeAppearanceModel.builder(context, R.style.ShapeAppearance_Google_Navigation, 0)
                        .build()
        ).apply { this.fillColor = fillColor }
        val insetRight = 8.dp
        return InsetDrawable(materialShapeDrawable, 0, 0, insetRight, 0)
    }

    @SuppressLint("RestrictedApi")
    fun Context.obtainStyledAttributesCompat(
            set: AttributeSet? = null,
            @StyleableRes attrs: IntArray,
            @AttrRes defStyleAttr: Int = 0,
            @StyleRes defStyleRes: Int = 0
    ): TintTypedArray =
            TintTypedArray.obtainStyledAttributes(this, set, attrs, defStyleAttr, defStyleRes)

    fun Context.getColorCompat(@ColorRes id: Int): Int = getColorStateListCompat(id).defaultColor

    fun Context.getColorStateListCompat(@ColorRes id: Int): ColorStateList =
            AppCompatResources.getColorStateList(this, id)!!

    fun isDarkMode(context: Context): Boolean {
        return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> true
            Configuration.UI_MODE_NIGHT_NO -> false
            else -> false
        }
    }

    fun setSystemBarStyle(activity: Activity) {
        if (isDarkMode(activity)) {
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
        } else {
            activity.window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activity.window.decorView.systemUiVisibility = (
                        activity.window.decorView.systemUiVisibility
                                or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            }
            if (getNavBarHeight(activity.contentResolver) > 20.dp) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.window.decorView.systemUiVisibility = (
                            activity.window.decorView.systemUiVisibility
                                    or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
                }
            }
        }
        setSystemBarTransparent(activity)
    }

    fun setSystemBarTransparent(activity: Activity) {
        val window = activity.window
        val view = window.decorView
        val flag = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        view.systemUiVisibility = view.systemUiVisibility or flag

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = Color.TRANSPARENT
            navigationBarColor = Color.TRANSPARENT
        }
    }
}