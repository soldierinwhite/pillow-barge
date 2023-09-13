package io.soldierinwhite.pillowbarge.extensions

import android.os.Build
import android.os.Bundle

inline fun <reified T> Bundle.parcelable(key: String) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        getParcelable(key)
    }
