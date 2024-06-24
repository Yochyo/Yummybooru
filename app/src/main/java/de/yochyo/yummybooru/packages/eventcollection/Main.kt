package de.yochyo.eventcollection

import de.yochyo.eventcollection.observable.Observable
import de.yochyo.eventcollection.observablecollection.ObservingEventCollection
import de.yochyo.eventcollection.observablecollection.ObservingSubEventCollection

fun main() {
    val array = arrayListOf<Observable<String>>(Observable("test"))
    val parent = ObservingEventCollection(array)
    val sub = ObservingSubEventCollection(arrayListOf(), parent) { it.value.startsWith("t") }
    sub.add(Observable("test"))
    //parent.find { it.value == "est" }!!.value = "ttestest"
    sub.replaceCollection(arrayListOf(Observable("taaest")))
    println()
}
