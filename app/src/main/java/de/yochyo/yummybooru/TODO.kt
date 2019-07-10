package de.yochyo.yummybooru

//Features
//TODO gifs und videos
//TODO folder in taglist einfügen
//TODO Auto mode bei server hinzufügen
//Tags und Subs als eine Klasse?
//Optimizations
//TODO auto-updater
//TODO Statt LiveData events benutzen
//TODO beim Profiler ist die CPU belastung heavy
//TODO downloader braucht ne lock, synchronisieren
//TODO Code Kommentieren
//TODO strings vol pref_general in string.xml
//TODO md5 des bildes benutzen
//TODO namen der xml elemente optimieren
//TODO Previewactivity select pictures to download
//TODO Wenn man den ausgewählten server ändert, wo wird der wieder ausgewählt
//Bugs
//TODO when z.B. ein download crasht, muss ein event (oder awaitPicture...) abgebrochen werden
//TODO bitmap = BitmapFactory.decodeStream(stream)!! in downloader schlägt selten fehl //wahrscheinlich weil mein png/jpg
//Nettes zeug für irgendwann
//TODO Subs nicht nur mit :id>X filtern, sondern auch per hand