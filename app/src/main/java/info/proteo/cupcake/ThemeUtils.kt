package info.proteo.cupcake

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

fun Context.getThemeColor(@AttrRes attributeId: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attributeId, typedValue, true)
    return typedValue.data
}

