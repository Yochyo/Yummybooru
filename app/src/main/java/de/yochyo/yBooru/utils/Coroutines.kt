package de.yochyo.yBooru.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

fun addChild(root: Job, isAsync: Boolean = false, job: suspend CoroutineScope.() -> Unit): Job {//TODO hier auch einen async anbieten
    return with(CoroutineScope(root)) { if (isAsync) async { job() } else launch { job() } }
}
