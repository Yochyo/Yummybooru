package de.yochyo.yummybooru.utils.analytics

import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsUtils {
    private val analytics = Firebase.analytics

    fun sendAnalytics(event: IAnalyticsEvent) {
        analytics.logEvent(event.name, event.createBundle())
    }
}