package de.yochyo.ybooru

/*
SubPreview entfernt
Gemacht: Sub erbt von Tag -- testen
 */

////
//TODO gifs und videos
//TODO namen der xml elemente optimieren
//TODO md5 des bildes benutzen
//TODO subscription statt startID macht man lastID
//TODO subscriptions ignorieren den r18 mode

//TODO onSingleFling geht nur wenn das bild geladen wurde
//TODO tags und subs irgendwie verbinden
//TODO ladebalken beim laden von large image
//large and original image
//TODO logo
//todo speicherpfad
//TODO mehrere server
//TODO bei downloads, einen kürzeren toast


/**
 * Server:
 * Database.loadserver(server: String)
 * Api: holt sich links aus der Datenbank
 * Für jeden Server eigene Subs/Tags Datenbank
 *  Bilder laden manchmal nicht ganz und richtig
 */

//--------------TODO ein sorted liste/set benutzen,
//------------TODO subs sollen auf newestTag besetzt werden, nicht auf 0???????????
//------------TODO sub seite aktuallisieren

//TODO downloads in der sub-seite abbrechen wenn die aktivity verlassen wird
//-------------------TODO reset subs to last
