package net.xzos.upgradeall.ui.base.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import net.xzos.upgradeall.R


class ProgressButton : AppCompatButton {
    private val MAX_PROGRESS = 100 //最大进度：默认为 100
    private val MIN_PROGRESS = 0 //最小进度：默认为 0

    private var mProgress = 0
    private var mProgressDrawable: GradientDrawable? = null //加载进度时的进度颜色
    private var mProgressDrawableBg: GradientDrawable? = null //加载进度时的背景色
    private var mNormalDrawable: StateListDrawable? = null //按钮在不同状态的颜色效果
    private var cornerRadius = 0f //圆角半径

    private var isShowProgress = false //是否展示进度
    private var isFinish = false //结束状态
    private var isStop = false //停止状态
    private var isStart = false //刚开始的状态
    private var onStateListener: OnStateListener? = null //结束时的监听

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attributeSet: AttributeSet) {

        // 初始化按钮状态 Drawable
        mNormalDrawable = StateListDrawable()
        // 初始化进度条 Drawable
        mProgressDrawable = ContextCompat.getDrawable(context, R.drawable.fg_update_btn)!!.mutate() as GradientDrawable
        // 初始化进度条背景 Drawable
        mProgressDrawableBg = ContextCompat.getDrawable(context, R.drawable.bg_update_btn)!!.mutate() as GradientDrawable
        val attr: TypedArray = context.obtainStyledAttributes(attributeSet, R.styleable.ProgressButton)

        try {
            // 默认的圆角大小
            val defValue = resources.getDimension(R.dimen.btn_update_corner)
            // 获取圆角大小
            cornerRadius = attr.getDimension(R.styleable.ProgressButton_buttonCornerRadius, defValue)


            // 获取是否显示进度信息的属性
            isShowProgress = attr.getBoolean(R.styleable.ProgressButton_showProgressNum, true)

            // 给按钮的状态Drawable添加其他时候的状态
            mNormalDrawable!!.addState(intArrayOf(), getNormalDrawable(attr))

            // 获取进度条颜色属性值
            val defaultProgressColor = Color.parseColor("#4586F3")
            val progressColor = attr.getColor(R.styleable.ProgressButton_progressColor, defaultProgressColor)
            // 设置进度条Drawable的颜色
            mProgressDrawable!!.setColor(progressColor)

            // 获取进度条背景颜色属性值
            val defaultProgressBgColor = Color.parseColor("#75A6FF")
            val progressBgColor = attr.getColor(R.styleable.ProgressButton_progressBgColor, defaultProgressBgColor)
            // 设置进度条背景Drawable的颜色
            mProgressDrawableBg!!.setColor(progressBgColor)
        } finally {
            attr.recycle()
        }

        // 初始化状态
        isFinish = false
        isStop = true
        isStart = false

        // 设置圆角
        mProgressDrawable!!.cornerRadius = cornerRadius
        mProgressDrawableBg!!.cornerRadius = cornerRadius
        // 设置按钮背景为状态Drawable
        setBackgroundCompat(mNormalDrawable)
    }


    override fun onDraw(canvas: Canvas) {
        if (mProgress in (MIN_PROGRESS + 1)..MAX_PROGRESS && !isFinish) {

            // 更新进度：
            val scale = getProgress().toFloat() / MAX_PROGRESS.toFloat()
            val indicatorWidth = measuredWidth.toFloat() * scale
            mProgressDrawable!!.setBounds(0, 0, indicatorWidth.toInt(), measuredHeight)
            mProgressDrawable!!.draw(canvas)

            // 进度完成时回调方法，并更变状态
            if (mProgress == MAX_PROGRESS) {
                setBackgroundCompat(mProgressDrawable)
                isFinish = true
                onStateListener?.onFinish()
            }
        }
        super.onDraw(canvas)
    }

    // 设置进度信息
    @SuppressLint("SetTextI18n")
    fun setProgress(progress: Int) {
        if (!isFinish && !isStop) {
            mProgress = progress
            if (isShowProgress) text = "$mProgress %"
            // 设置背景
            setBackgroundCompat(mProgressDrawableBg)
            invalidate()
        }
    }


    // 获取进度
    fun getProgress(): Int {
        return mProgress
    }

    // 设置为停止状态
    fun setStop(stop: Boolean) {
        isStop = stop
        invalidate()
    }

    fun isStop(): Boolean {
        return isStop
    }

    fun isFinish(): Boolean {
        return isFinish
    }

    // 切换状态：
    fun toggle() {
        if (!isFinish && isStart) {
            if (isStop) {
                setStop(false)
                onStateListener?.onContinue()
            } else {
                setStop(true)
                onStateListener?.onStop()
            }
        } else {
            setStop(false)
            isStart = true
        }
    }

    // 设置按钮背景
    private fun setBackgroundCompat(drawable: Drawable?) {
        val pL = paddingLeft
        val pT = paddingTop
        val pR = paddingRight
        val pB = paddingBottom
        background = drawable
        setPadding(pL, pT, pR, pB)
    }

    // 初始化状态
    fun initState() {
        setBackgroundCompat(mNormalDrawable)
        isFinish = false
        isStop = true
        isStart = false
        mProgress = 0
    }


    // 获取状态Drawable的正常状态下的背景
    private fun getNormalDrawable(attr: TypedArray): Drawable {
        val drawableNormal = ContextCompat.getDrawable(context, R.drawable.bg_update_btn)!!.mutate() as GradientDrawable // 修改时就不会影响其它drawable对象的状态
        drawableNormal.cornerRadius = cornerRadius // 设置圆角半径
        val defaultNormal = Color.parseColor("#4586F3")
        val colorNormal = attr.getColor(R.styleable.ProgressButton_buttonNormalColor, defaultNormal)
        drawableNormal.setColor(colorNormal) //设置颜色
        return drawableNormal
    }

    // 获取按钮被点击时的Drawable
    private fun getPressedDrawable(attr: TypedArray): Drawable {
        val drawablePressed = ContextCompat.getDrawable(context, R.drawable.bg_update_btn)!!.mutate() as GradientDrawable // 修改时就不会影响其它drawable对象的状态
        drawablePressed.cornerRadius = cornerRadius // 设置圆角半径
        val defaultPressed = Color.parseColor("#4586F3")
        val colorPressed = attr.getColor(R.styleable.ProgressButton_buttonPressedColor, defaultPressed)
        drawablePressed.setColor(colorPressed) //设置颜色
        return drawablePressed
    }

    // 设置状态监听接口
    interface OnStateListener {
        fun onFinish()
        fun onStop()
        fun onContinue()
    }

    fun setOnStateListener(onStateListener: OnStateListener?) {
        this.onStateListener = onStateListener
    }

    fun isShowProgressNum(b: Boolean) {
        isShowProgress = b
    }

}