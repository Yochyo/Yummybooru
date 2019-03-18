package de.yochyo.ybooru.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun addChild(root: Job, job: suspend CoroutineScope.() -> Unit): Job {//TODO hier auch einen async anbieten
    return with(CoroutineScope(root)) { launch { job() } }
}
