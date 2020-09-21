package de.yochyo.yummybooru.utils.general

import com.google.firebase.crashlytics.FirebaseCrashlytics

class Logger(private val e: Throwable) {
    private val firebase = FirebaseCrashlytics.getInstance()

    fun log(message: String): Logger {
        firebase.log(message)
        return this
    }

    fun send() = FirebaseCrashlytics.getInstance().recordException(e)
}


fun Throwable.logFirebase(message: String) = Logger(this).log(message)
fun Throwable.sendFirebase() = Logger(this).send()