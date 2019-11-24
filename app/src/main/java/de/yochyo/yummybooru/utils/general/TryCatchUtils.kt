package de.yochyo.yummybooru.utils.general


fun <E> tryCatch(method: () -> E): TryCatchResult<E> {
    //TODO tryCatch Methode benutzen
    val result: TryCatchResult<E>
    result = try {
        val r = method()
        TryCatchResult(r, null)
    } catch (e: Exception) {
        TryCatchResult(null, e)
    }
    return result
}

suspend fun <E> tryCatchSuspended(method: suspend () -> E): TryCatchResult<E> {
    val result: TryCatchResult<E>
    result = try {
        val r = method()
        TryCatchResult(r, null)
    } catch (e: Exception) {
        TryCatchResult(null, e)
    }
    return result
}

class TryCatchResult<E>(val value: E?, internal val exception: Exception?) {
    fun catch(method: (e: Exception) -> E): TryCatchResult<E> {
        if (exception != null) method(exception)
        return this
    }

    fun finally(method: () -> E): TryCatchResult<E> {
        method()
        return this
    }

    fun log(message: String = ""): TryCatchResult<E> {
        if (exception != null) Logger.log(exception, message)
        return this
    }

    fun stackTrace(): TryCatchResult<E> {
        exception?.printStackTrace()
        return this
    }

    fun stackTraceAndLogs(message: String = "") = stackTrace().log(message)
}