package de.yochyo.ybooru

//TODO gifs und videos
//TODO md5 des bildes benutzen
//TODO namen der xml elemente optimieren
//Satt R18 mode, server mit r18 und ohne machen
//TODO Bug cache id muss die server id enthalten
//TODO onSingleFling geht nur wenn das bild geladen wurde
//TODO ladebalken beim laden von sample image
//TODO sample and original image
//TODO icon
//todo speicherpfad
//TODO bei downloads, einen kürzeren toast
//TODO don´t allow to create tags if no internet connection
//TODO reset subs to last
//TODO reloaden in Subscriptionacitivty eine funktion geben, wie reloaden falls es kein internet gab?

//TODO Subs nicht nur mit :id>X filtern, sondern auch per hand
/*
Server:
Alles ausprobieren, um die Server-Url herauszufinden (danbooru hat posts.json, yande.re hat post.json)
 */
//TODO farben von mbooru klauen
//TODO server name bei der datei verzürzen
/** Bei adaptern, um leistung zu sparen
 * private val mInflater: LayoutInflater
 * init {
mInflater = LayoutInflater.from(context)
}
//TODO verschiedene server -> ordnername
//TODO add pixiv support
//TODO add nhentai support?
 */
//TODO entstehen crashes z.B. beim bildschirm drehen durch eine null referenz bei context?
//TODO keine Context-referenzen speichern, weil
/*
Warning: Never pass context into ViewModel instances. Do not store Activity, Fragment, or View instances or their Context in the ViewModel.
For example, an Activity can be destroyed and created many times during the lifecycle of a ViewModel as the device is rotated. If you store a reference to the Activity in the ViewModel, you end Up with references that point to the destroyed Activity. This is a memory leak.
If you need the application context, use AndroidViewModel, as shown in this codelab.
 */

//TODO bei startup/closeup buggt es manchmal
