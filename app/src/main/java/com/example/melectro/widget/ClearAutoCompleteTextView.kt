package com.example.melectro.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.View.OnTouchListener
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.example.melectro.R



class ClearAutoCompleteTextView : AppCompatAutoCompleteTextView, OnTouchListener,
    OnFocusChangeListener, TextWatcher {
    private var mClearTextIcon: Drawable? = null
    private var mOnFocusChangeListener: OnFocusChangeListener? = null
    private var mOnTouchListener: OnTouchListener? = null

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context) {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_clear)
        val wrappedDrawable =
            DrawableCompat.wrap(drawable!!) //Wrap the drawable so that it can be tinted pre Lollipop
        DrawableCompat.setTint(wrappedDrawable, currentHintTextColor)
        mClearTextIcon = wrappedDrawable
        mClearTextIcon!!.setBounds(
            0,
            0,
            mClearTextIcon!!.intrinsicHeight,
            mClearTextIcon!!.intrinsicHeight
        )
        setClearIconVisible(false)
        super.setOnTouchListener(this)
        super.setOnFocusChangeListener(this)
        addTextChangedListener(this)
    }

    override fun setOnFocusChangeListener(l: OnFocusChangeListener) {
        mOnFocusChangeListener = l
    }

    override fun setOnTouchListener(l: OnTouchListener) {
        mOnTouchListener = l
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) {
            setClearIconVisible(text.length > 0)
        } else {
            setClearIconVisible(false)
        }
        if (mOnFocusChangeListener != null) {
            mOnFocusChangeListener!!.onFocusChange(v, hasFocus)
        }
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val x = motionEvent.x.toInt()
        if (mClearTextIcon!!.isVisible && x > width - paddingRight - mClearTextIcon!!.intrinsicWidth) {
            if (motionEvent.action == MotionEvent.ACTION_UP) {
                error = null
                setText("")
            }
            return true
        }
        return mOnTouchListener != null && mOnTouchListener!!.onTouch(view, motionEvent)
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        if (isFocused) {
            setClearIconVisible(text.length > 0)
        }
    }

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
    override fun afterTextChanged(s: Editable) {}
    private fun setClearIconVisible(visible: Boolean) {
        mClearTextIcon!!.setVisible(visible, false)
        val compoundDrawables = compoundDrawables
        setCompoundDrawables(
            compoundDrawables[0],
            compoundDrawables[1],
            if (visible) mClearTextIcon else null,
            compoundDrawables[3]
        )
    }
}