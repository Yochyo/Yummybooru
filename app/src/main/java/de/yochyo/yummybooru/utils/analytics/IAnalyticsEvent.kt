package de.yochyo.yummybooru.utils.analytics

import android.os.Bundle

interface IAnalyticsEvent {
    val name: String
    fun createBundle(): Bundle
}