package de.yochyo.eventcollection.observable

import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.interfaces.HasEventHandler

/**
 * Classes which inherit from this interface have an onChange Event that can be triggered when a value is changed
 * @param T type of the Object
 * @param A type of the arg that should be handed over to the event
 */
interface IObservableObject<T, A> : HasEventHandler<OnChangeObjectEvent<T, A>>