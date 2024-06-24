package de.yochyo.eventcollection.events

import de.yochyo.eventmanager.Event

/**
 * Event triggered when an EventCollection is Updated.
 * This event is triggered should be triggered whenever an element is added/removed,
 * the collection is cleared or created.
 * @param collection the updated collection
 * @param T type of elements contained in the collection
 */
class OnUpdateEvent<T>(val collection: Collection<T>) : Event()

/**
 * Event triggered when an EventCollection is cleared.
 * @param collection the updated collection
 * @param T type of elements contained in the collection
 */
class OnClearEvent<T>(val collection: Collection<T>) : Event()

/**
 * Event triggered when an element is added to the EventCollection.
 * @param collection the updated collection
 * @param elements elements added to the collection
 * @param T type of elements contained in the collection
 */
class OnAddElementsEvent<T>(val collection: Collection<T>, val elements: Collection<T>) : Event()

/**
 * Event triggered when an element is removed from the EventCollection.
 * @param collection the updated collection
 * @param elements elements removed from the collection
 * @param T type of elements contained in the collection
 */
class OnRemoveElementsEvent<T>(val collection: Collection<T>, val elements: Collection<T>) : Event()

/**
 * Event triggered when an IObservableObject is changed
 * @param new the new value of the Object
 * @param arg can be whatever you want, for example the old value.
 * @param T type of the object
 * @param A type of arg
 */
class OnChangeObjectEvent<T, A>(val new: T, val arg: A) : Event()

/**
 * Event triggered when the backing collection of an EventCollection is replaced
 * @param old the old collection
 * @param new the new collection
 * @param T type of collection elements
 */
class OnReplaceCollectionEvent<T>(val old: Collection<T>, val new: Collection<T>) : Event()

/**
 * Event thrown when an element was replaced in a collection. The index of the element will change. Use a sorted collection to retain it
 * @param old old element
 * @param element new element
 * @param new new collection
 */
class OnReplaceElementEvent<T>(val old: T, val element: T, val new: Collection<T>) : Event()