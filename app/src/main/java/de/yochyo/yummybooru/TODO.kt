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
//TODO Tags in png metadata Speichern
//TODO developer mode app flags setzen können
//TODO Suchen nach id in mainactiity
//TODO Tags und subs vereinen?
//TODO Update all subs button
//Optimizations
//TODO Normale Downloads auch über nen service? zumindest wenn die app geschlossen wird, downloads umlagern
//TODO Pictureactivity infotags multiselect mit Fab Button suchen
//TODO Wenn Ein Speicherpfad nicht existiert, erst versuchen ihn zu erstellen, dann resetten
//TODO Selection-Actionmode hinzufügen, statt alles einzeln zu machen
//TODO Bilder durch Vector Drawables ersetzen
//TODO Bei Moebooru tags speichern, damit sie nicht JEDES mal runtergeladen werden
//TODO logs in data abspeichern -- werden zu einem server geschickt
//TODO AddTagDialog vorschläge beziehen sich immer auf den aktuellsten Tag (Falls man mehrere angibt)

//TODO beim Profiler ist die CPU belastung heavy
//TODO downloader braucht ne lock, synchronisieren
//TODO Code Kommentieren
//TODO strings vol pref_general in string.xml
//TODO md5 des bildes benutzen
//TODO subscription acitivty anders regeln, count wird bei jedem scrollen neu geladen
//TODO Share button
//Bugs
//TODO Wenn ein Service downloaded während Server gewechselt wird wird im falschen Server gespeicheet
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub
//TODO Toolbar menu aktualisiert sich noch nicht immer?
//TODO Mainactivity TagRecyclerView selecting view machen
//TODO Braucht Manager eine lock
//TODO stürzt ab wenn man UNKNOWN tags hinzufügt und updatemissing tags ausgeführt wird
//TODO updatemissing anders regeln, sowas sollte nicht in der datenbank stecken
//TODO when z.B. ein download crasht, muss ein event (oder awaitPicture...) abgebrochen werden