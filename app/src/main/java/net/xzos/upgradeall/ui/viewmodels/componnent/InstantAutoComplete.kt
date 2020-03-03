package net.xzos.upgradeall.ui.viewmodels.componnent

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.widget.AppCompatAutoCompleteTextView


class InstantAutoComplete : AppCompatAutoCompleteTextView {
    constructor(context: Context?) : super(context)
    constructor(arg0: Context?, arg1: AttributeSet?) : super(arg0, arg1)
    constructor(arg0: Context?, arg1: AttributeSet?, arg2: Int) : super(arg0, arg1, arg2)

    override fun enoughToFilter(): Boolean {
        return true
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (windowVisibility == View.GONE) {
            Log.d("InstantAutoComplete", "Window not visible, will not show drop down")
            return
        }
        if (focused && adapter != null) {
            performFiltering(text, 0)
            showDropDown()
        }
    }
}
