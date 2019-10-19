package de.yochyo.yummybooru

//Momentan
//TODO selected tags sind falsch markiert

//Features
//TODO support animated_png
//TODO Tests einfügen
//TODO multiselect pictureactivity kann auch einen tag hinzufügen (elsword eve_(elsword) oder so)
//TODO gifs und videos
//TODO folder in taglist einfügen
//TODO Auto mode bei server hinzufügen
//TODO einen width/height/order button (wie bei mbooru) hinzufügen, Tag hat type.SPECIAL tag
//TODO Mainactivity unselect all einfügen
//TODO suchfeld bei tag/sub liste
//TODO Mainactiviy mom. ausgewählte tags anzeigen
//TODO pixiv
//TODO Tags in png metadata Speichern
//TODO developer mode app flags setzen können
//TODO Suchen nach id in mainactiity
//TODO Tags und subs vereinen?
//TODO Normale Downloads auch über nen service? zumindest wenn die app geschlossen wird, downloads umlagern
//TODO Doppelklick auf image in previewactivity lädt das bild herunter
//TODO SeveralTagsPreviewLayout -< siehe unten
//Optimizations
//TODO datenbank immer mit mutex, normale methoden addTagWithoutMutex... umbennenen
//TODO on Long click für info was ein menü button in der actionbar macht macht
//TODO updateMissingSubs/Tags überarbeiten
//TODO Tag.getCorrectTagType besser einbetten
//TODO statt Tga.compareTo dem TreeSet einen eigenen Comparator geben
//TODO Pictureactivity, Mainactivity infotags multiselect
//TODO Selection-Actionmode hinzufügen, statt alles einzeln zu machen
//TODO logs in data abspeichern -- werden zu einem server geschickt
//TODO CPU leistung optimieren Code Kommentieren und übersichtlicher gestalten
//TODO preloading besser gestalten, sodass ladezeiten besser sind
//Bugs
//TODO wenn das internet weg ist, lädt die manager seite nicht weiter runter
//TODO list is empty bei absturz fixen
//TODO tag 6+girls geht nicht
//TODO wenn man kein Internet hat kann es passieren, dass die EndOfManager Fehlermeldung kommt
//TODO zu viele logs verbrauchen doch gut speicherplatz
//TODO moebooru tag reinfolge ist glaub noch falsch
//TODO discord schauen
//TODO downloadservice geht nicht
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub //TODO icon updaten, unter anderem bei discord... sieht es komisch aus
//TODO Toolbar menu aktualisiert sich noch nicht immer?

//Wahrscheinlich gefixt
//TODO speicherordner resettet sich
//TODO es kann passieren, dass ein bild größer als 25MB ist, oder nicht in den heap passt

//TODO SeveraltagsPreviewLayout
//  Zeigt mehrere tags an
//  der aktuelle tag wird in der Actionbar angezeigt

//Update Raus Bringen TODO List
//1: Build.gradle version updaten
//1.1: Changelog hinzufügen
//2: Twitter Post raushauen
//3: Discord Post raushauen
//4: Build auf Github hochladen (apk in format VersionName.apk)