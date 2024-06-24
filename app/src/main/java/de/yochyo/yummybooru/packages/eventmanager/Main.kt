package de.yochyo.eventmanager

fun main() {
    var test = true
    val handler = EventHandler<TestEvent>()
    handler.registerListener {
        if (test) {
            test = false
            handler.registerListener { println("wert") }
        }
    }
    handler.trigger(TestEvent())
    println(234)
}

class TestEvent : Event()