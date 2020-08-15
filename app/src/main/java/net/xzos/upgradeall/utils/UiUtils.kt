package net.xzos.upgradeall.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.util.TypedValue
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
}