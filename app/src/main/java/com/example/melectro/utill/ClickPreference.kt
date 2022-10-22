package com.example.melectro.utill

import android.content.Context
import androidx.preference.ListPreference
import android.util.AttributeSet


class ClickPreference : ListPreference {
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    ) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {}
    constructor(context: Context?) : super(context!!) {}

    override protected fun onClick() {}
}