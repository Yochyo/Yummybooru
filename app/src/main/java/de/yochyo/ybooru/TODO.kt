package de.yochyo.ybooru

//Features
//TODO gifs und videos
//TODO folder in taglist einfügen
//TODO Auto mode bei server hinzufügen
//TODO add pixiv support
//Optimizations
//TODO beim Profiler ist die CPU belastung heavy
//TODO downloader braucht ne lock, synchronisieren
//TODO Code Kommentieren
//TODO strings vol pref_general in string.xml
//TODO md5 des bildes benutzen
//TODO namen der xml elemente optimieren
//TODO Manager.resetAll optimieren, anwendungszeit verbessern
//TODO Download All
//TODO Previewactivity select pictures to download
//TODO only search but don´t add tag
//TODO tags wie bei mBooru suchen
//TODO Wenn man den ausgewählten server ändert, wo wird der wieder ausgewählt
//Bugs
//TODO when z.B. ein download crasht, muss ein event (oder awaitPicture...) abgebrochen werden
//TODO wenn man einen Tag/Sub löscht und schnell server wechselt, wird der tag nicht gelöscht / geadded -------
//TODO was mach ich wenn der SPeicherPfad gelöscht wird -- vllt jedes mal nachschauen und sonst savePath auf default setzen
//TODO bitmap = BitmapFactory.decodeStream(stream)!! in downloader schlägt selten fehl
//TODO settings backpress geht nicht in emulierten handys
//Nettes zeug für irgendwann
//TODO Subs nicht nur mit :id>X filtern, sondern auch per hand