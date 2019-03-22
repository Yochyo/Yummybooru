package de.yochyo.ybooru

/*
SubPreview entfernt
Gemacht: Sub erbt von Tag -- testen
 */

////
//TODO gifs und videos
//TODO namen der xml elemente optimieren
//TODO md5 des bildes benutzen
//TODO subscription statt startID macht man last
//TODO subscriptions ignorieren den r18 mode

//TODO onSingleFling geht nur wenn das bild geladen wurde
//TODO tags und subs irgendwie verbinden
//TODO ladebalken beim laden von large image
//large and original image
//TODO logo
//todo speicherpfad
//TODO mehrere server
//TODO bei downloads, einen kürzeren toast

//TODO Downloader pausieren wenn kein bild da ist, und resume wenn die queue gefüllt wird
/**
 * Server:
 * Database.loadserver(server: String)
 * Api: holt sich links aus der Datenbank
 * Für jeden Server eigene Subs/Tags Datenbank
 *  Bilder laden manchmal nicht ganz und richtig
 */

/*
Add Button in SubscriptionActivity
 */
//TODO don´t allow to create tags if no internet connection
//TODO cache beim schließen, nicht beim starten löschen
//TODO addTag combobox nicht mehr öffnen, wenn etwas geöffnet wurde

//TODO das ist ugly code
//entstehen crashes z.B. beim bildschirm drehen durch eine null referenz bei context?
//--------------TODO ein sorted liste/set benutzen,
//------------TODO subs sollen auf newestTag besetzt werden, nicht auf 0???????????
//------------TODO sub seite aktuallisieren

//TODO downloads in der sub-seite abbrechen wenn die aktivity verlassen wird
//-------------------TODO reset subs to last

//TODO
/** Bei adaptern, um leistung zu sparen
 * private val mInflater: LayoutInflater
 * init {
mInflater = LayoutInflater.from(context)
}
 */
//TODO database -> _variablen durch var mit getter und setter ersetzen
//TODO keine Context-referenzen speichern, weil
/*
Warning: Never pass context into ViewModel instances. Do not store Activity, Fragment, or View instances or their Context in the ViewModel.
For example, an Activity can be destroyed and created many times during the lifecycle of a ViewModel as the device is rotated. If you store a reference to the Activity in the ViewModel, you end Up with references that point to the destroyed Activity. This is a memory leak.
If you need the application context, use AndroidViewModel, as shown in this codelab.
 */
