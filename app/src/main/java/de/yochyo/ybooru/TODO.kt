package de.yochyo.ybooru

//Features
//TODO gifs und videos
//TODO add pixiv support
//TODO add nhentai support?
//Optimizations
//TODO strings vol pref_general in string.xml
//TODO md5 des bildes benutzen
//TODO namen der xml elemente optimieren
//TODO don´t allow to create tags if no internet connection -> Fix them when you have internet
//TODO Subs nicht nur mit :id>X filtern, sondern auch per hand
//TODO folder in taglist einfügen
//TODO Manager.resetAll optimieren, anwendungszeit verbessern
//TODO setContentView dauert ewig
//TODO getallTags + Subs haben einen runBlcoking block
//TODO subs und Tags sollen beim geladen werden geupdated werden
//Bugs
//TODO funktioniert das speichern auf jeder Android Version, testen
//TODO was mach ich wenn der SPeicherPfad gelöscht wird -- vllt jedes mal nachschauen und sonst savePath auf default setzen
//TODO bitmap = BitmapFactory.decodeStream(stream)!! in downloader schlägt selten fehl
//TODO bei startup/closeup buggt es manchmal
//TODO settings backpress geht nicht in emulierten handys
//Nettes zeug für irgendwann
//TODO Auto mode bei server hinzufügen