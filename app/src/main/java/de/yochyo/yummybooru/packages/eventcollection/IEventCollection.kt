package de.yochyo.eventcollection

import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnClearEvent
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventcollection.events.OnReplaceCollectionEvent
import de.yochyo.eventcollection.events.OnReplaceElementEvent
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.EventHandler
import de.yochyo.eventmanager.Listener

interface IEventCollection<T> : MutableCollection<T> {
    @Deprecated("do not access without exactly knowing what you are doing")
    var collection: MutableCollection<T>

    val onUpdate: EventHandler<OnUpdateEvent<T>>
    val onClear: EventHandler<OnClearEvent<T>>
    val onAddElements: EventHandler<OnAddElementsEvent<T>>
    val onRemoveElements: EventHandler<OnRemoveElementsEvent<T>>
    val onReplaceCollection: EventHandler<OnReplaceCollectionEvent<T>>
    val onReplaceElement: EventHandler<OnReplaceElementEvent<T>>


    fun replaceCollection(c: MutableCollection<T>)
    fun replaceElement(old: T, new: T): Boolean

    fun notifyChange() {
        onUpdate.trigger(OnUpdateEvent(collection))
    }

    fun registerOnAddElementsListener(l: Listener<OnAddElementsEvent<T>>) = onAddElements.registerListener(l)
    fun registerOnRemoveElementsListener(l: Listener<OnRemoveElementsEvent<T>>) = onRemoveElements.registerListener(l)
    fun registerOnClearListener(l: Listener<OnClearEvent<T>>) = onClear.registerListener(l)
    fun registerOnUpdateListener(l: Listener<OnUpdateEvent<T>>) = onUpdate.registerListener(l)
    fun registerOnReplaceCollectionListener(l: Listener<OnReplaceCollectionEvent<T>>) = onReplaceCollection.registerListener(l)
    fun registerOnReplaceElementListener(l: Listener<OnReplaceElementEvent<T>>) = onReplaceElement.registerListener(l)

    fun removeOnAddElementsListener(l: Listener<OnAddElementsEvent<T>>) = onAddElements.removeListener(l)
    fun removeOnRemoveElementsListener(l: Listener<OnRemoveElementsEvent<T>>) = onRemoveElements.removeListener(l)
    fun removeOnClearListener(l: Listener<OnClearEvent<T>>) = onClear.removeListener(l)
    fun removeOnUpdateListener(l: Listener<OnUpdateEvent<T>>) = onUpdate.removeListener(l)
    fun removeOnReplaceCollectionListener(l: Listener<OnReplaceCollectionEvent<T>>) = onReplaceCollection.removeListener(l)
    fun removeOnReplaceElementListener(l: Listener<OnReplaceElementEvent<T>>) = onReplaceElement.removeListener(l)

    fun triggerOnAddElementsEvent(e: OnAddElementsEvent<T>) = onAddElements.trigger(e)
    fun triggerOnRemoveElementsEvent(e: OnRemoveElementsEvent<T>) = onRemoveElements.trigger(e)
    fun triggerOnClearEvent(e: OnClearEvent<T>) = onClear.trigger(e)
    fun triggerOnUpdateEvent(e: OnUpdateEvent<T>) = onUpdate.trigger(e)
    fun triggerOnReplaceCollectionEvent(e: OnReplaceCollectionEvent<T>) = onReplaceCollection.trigger(e)
    fun triggerOnReplaceElementEvent(e: OnReplaceElementEvent<T>) = onReplaceElement.trigger(e)
}