package de.yochyo.yummybooru.utils

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class NonNullLiveData<T>(val default: T) : MutableLiveData<T>(default) {
    override fun getValue(): T {
        return super.getValue() ?: default
    }
}

class LiveDataValue<T>(livedata: LiveData<T>, owner: LifecycleOwner?, default: T? = null) {
    private var _value: T? = default
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
