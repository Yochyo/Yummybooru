package de.yochyo.yummybooru.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class LiveDataValue<T>(livedata: LiveData<T>, owner: LifecycleOwner?) {
    private var _value: T? = null
    val value: T
        get() = runBlocking {
            var v: T? = _value
            while (v == null) {
                v = _value
                if (v == null) delay(5)
            }
            v
        }

    init {
        if (owner == null) livedata.observeForever { _value = it }
        else livedata.observe(owner, { _value = it })
    }

}

fun <T> LiveData<T>.withValue(owner: LifecycleOwner, observer: (value: T) -> Unit) {
    observe(owner, object : Observer<T> {
        override fun onChanged(t: T) {
            if (t != null) {
                observer(t)
                removeObserver(this)
            }
        }
    })
}

//TODO a reason not to do this?
fun <T> LiveData<T>.withValue2(owner: LifecycleOwner, observer: (value: T) -> Unit) {
    observeForever(object : Observer<T> {
        override fun onChanged(t: T) {
            if (t != null) {
                observer(t)
                removeObserver(this)
            }
        }
    })
}

fun <T> LiveData<T>.observeUntil(owner: LifecycleOwner, observer: (value: T) -> Unit, success: (value: T) -> Boolean) {
    observe(owner, object : Observer<T> {
        override fun onChanged(t: T) {
            if (t != null) {
                observer(t)
                if (success(t)) removeObserver(this)
            }
        }
    })
}