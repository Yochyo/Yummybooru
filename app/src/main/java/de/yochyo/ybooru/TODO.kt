package de.yochyo.ybooru

//TODO gifs und videos
//TODO md5 des bildes benutzen
//TODO namen der xml elemente optimieren
//Satt R18 mode, server mit r18 und ohne machen

//TODO onSingleFling geht nur wenn das bild geladen wurde
//TODO ladebalken beim laden von large image
//TODO large and original image
//TODO icon
//todo speicherpfad
//TODO bei downloads, einen kürzeren toast
//TODO don´t allow to create tags if no internet connection
//TODO reset subs to last
//TODO reloaden in Subscriptionacitivty eine funktion geben, wie reloaden falls es kein internet gab?
/**
 * eine server.selectserver(s: Server) funktion, die id, currentServer, die geladenen tags/subs und die api ändert
 */
/**
 * Server:
 * Database.loadserver(server: String)
 * Api: holt sich links aus der Datenbank
 * Für jeden Server eigene Subs/Tags Datenbank
 *  Bilder laden manchmal nicht ganz und richtig
 */
//TODO Subs nicht nur mit :id>X filtern, sondern auch per hand
//TODO URL in richtige URL umwandeln?
/*
Server:
Alles ausprobieren, um die Server-Url herauszufinden (danbooru hat posts.json, yande.re hat post.json)
 */

//TODO
/** Bei adaptern, um leistung zu sparen
 * private val mInflater: LayoutInflater
 * init {
mInflater = LayoutInflater.from(context)
}
//TODO verschiedene server -> ordnername
 */
//TODO entstehen crashes z.B. beim bildschirm drehen durch eine null referenz bei context?
//TODO keine Context-referenzen speichern, weil
/*
Warning: Never pass context into ViewModel instances. Do not store Activity, Fragment, or View instances or their Context in the ViewModel.
For example, an Activity can be destroyed and created many times during the lifecycle of a ViewModel as the device is rotated. If you store a reference to the Activity in the ViewModel, you end Up with references that point to the destroyed Activity. This is a memory leak.
If you need the application context, use AndroidViewModel, as shown in this codelab.
 */
