package de.yochyo.yBooru.utils

import android.content.Context
import android.os.Handler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

fun <R> runAsync(context: Context, async: suspend () -> R, onMainThread: (result: R) -> Unit) {
    GlobalScope.async {
        val job = async { async() }
        val result = job.await()
        main(context) { onMainThread(result) }
    }
}

fun <R> runAsync(async: suspend () -> R) {
    GlobalScope.async {
        async()
    }
}

fun main(context: Context, run: () -> Unit) {
    val handler = Handler(context.mainLooper)
    handler.post(run)
}