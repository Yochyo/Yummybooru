package de.yochyo.yummybooru.utils.general

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

val TagDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()