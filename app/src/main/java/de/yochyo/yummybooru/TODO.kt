package de.yochyo.yummybooru

//Features
//TODO multiselect pictureactivity kann auch einen tag hinzufügen (elsword eve_(elsword) oder so)
//TODO download and add all authors
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
//TODO wenn mp4... eingeführt ist, muss downloader loggen
//TODO Normale Downloads auch über nen service? zumindest wenn die app geschlossen wird, downloads umlagern
//TODO Pictureactivity infotags multiselect mit Fab Button suchen
//TODO Wenn Ein Speicherpfad nicht existiert, erst versuchen ihn zu erstellen, dann resetten
//TODO Selection-Actionmode hinzufügen, statt alles einzeln zu machen
//TODO Bilder durch Vector Drawables ersetzen
//TODO logs in data abspeichern -- werden zu einem server geschickt

//TODO beim Profiler ist die CPU belastung heavy
//TODO Code Kommentieren, neu anordnen
//TODO Share button
//Bugs
//TODO es kann passieren, dass ein bild größer als 25MB ist, oder nicht in den heap passt
//TODO downloadservice geht nicht
//TODO speicherordner resettet sich
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub
//TODO Toolbar menu aktualisiert sich noch nicht immer?
//TODO Mainactivity TagRecyclerView selecting view machen
//TODO Braucht Manager eine lock
//TODO stürzt ab wenn man UNKNOWN tags hinzufügt und updatemissing tags ausgeführt wird