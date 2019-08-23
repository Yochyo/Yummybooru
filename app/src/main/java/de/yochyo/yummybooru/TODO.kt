package de.yochyo.yummybooru

//Features
//TODO animated pictures
//TODO changelog
//TODO gifs und videos
//TODO folder in taglist einfügen
//TODO Auto mode bei server hinzufügen
//TODO Previewactivity select pictures to download
//TODO auto-updater
//TODO buttons für width, order ... features hinzufügen
//TODO einen width/height/order button (wie bei mbooru) hinzufügen, Tag hat type.SPECIAL tag
//TODO Mainactivity unselect all einfügen
//TODO suchfeld bei tag/sub liste
//TODO subs update all subs button
//TODO Mainactiviy mom. ausgewählte tags anzeigen
//TODO pixiv
//Optimizations
//TODO Bei Moebooru tags speichern, damit sie nicht JEDES mal runtergeladen werden
//TODO logs in data abspeichern
//TODO AddTagDialog vorschläge beziehen sich immer auf den aktuellsten Tag (Falls man mehrere angibt)

//TODO beim Profiler ist die CPU belastung heavy
//TODO downloader braucht ne lock, synchronisieren
//TODO Code Kommentieren
//TODO strings vol pref_general in string.xml
//TODO md5 des bildes benutzen
//TODO subscription acitivty anders regeln, count wird bei jedem scrollen neu geladen
//TODO Share button
//Bugs
//TODO Mainactivity TagRecyclerView selecting view machen
//TODO Braucht Manager eine lock
//TODO stürzt ab wenn man UNKNOWN tags hinzufügt und updatemissing tags ausgeführt wird
//TODO updatemissing anders regeln, sowas sollte nicht in der datenbank stecken
//TODO when z.B. ein download crasht, muss ein event (oder awaitPicture...) abgebrochen werden