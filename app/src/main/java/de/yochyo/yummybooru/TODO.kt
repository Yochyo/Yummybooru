package de.yochyo.yummybooru

//Features
//TODO Tests einfügen
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
//Optimizations
//TODO wenn mp4... eingeführt ist, muss downloader loggen
//TODO Normale Downloads auch über nen service? zumindest wenn die app geschlossen wird, downloads umlagern
//TODO Pictureactivity infotags multiselect mit Fab Button suchen
//TODO Wenn Ein Speicherpfad nicht existiert, erst versuchen ihn zu erstellen, dann resetten
//TODO Selection-Actionmode hinzufügen, statt alles einzeln zu machen
//TODO Bilder durch Vector Drawables ersetzen
//TODO logs in data abspeichern -- werden zu einem server geschickt
//TODO Mainactivity TagRecyclerView selecting view machen
//TODO beim Profiler ist die CPU belastung heavy
//TODO Code Kommentieren, neu anordnen
//Bugs
//TODO moebooru tag reinfolge ist glaub noch falsch
//TODO discord schauen
//TODO es kann passieren, dass ein bild größer als 25MB ist, oder nicht in den heap passt
//TODO downloadservice geht nicht
//TODO speicherordner resettet sich
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub //TODO icon updaten, unter anderem bei discord... sieht es komisch aus
//TODO Toolbar menu aktualisiert sich noch nicht immer?
//TODO Braucht Manager eine lock

//Update Raus Bringen TODO List
//1: Build.gradle version updaten
//1.1: Changelog hinzufügen
//2: Twitter Post raushauen
//3: Discord Post raushauen
//4: Build auf Github hochladen (apk in format VersionName.apk)