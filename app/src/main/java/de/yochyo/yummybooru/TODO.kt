package de.yochyo.yummybooru

//Momentan
//TODO selected tags sind falsch markiert

//Features
//TODO support animated_png
//TODO Tests einfügen
//TODO multiselect pictureactivity kann auch einen tag hinzufügen (elsword eve_(elsword) oder so)
//TODO folder in taglist einfügen
//TODO Auto mode bei server hinzufügen
//TODO einen width/height/order button (wie bei mbooru) hinzufügen, Tag hat type.SPECIAL tag
//TODO Mainactivity unselect all einfügen
//TODO suchfeld bei sub liste
//TODO Mainactiviy mom. ausgewählte tags anzeigen
//TODO pixiv
//TODO Tags in png metadata Speichern
//TODO developer mode app flags setzen können
//TODO Suchen nach id in mainactiity
//TODO Tags und subs vereinen?
//TODO Normale Downloads auch über nen service? zumindest wenn die app geschlossen wird, downloads umlagern
//TODO Doppelklick auf image in previewactivity lädt das bild herunter oder so
//TODO SeveralTagsPreviewLayout -< siehe unten
//Optimizations
//TODO datenbank immer mit mutex, normale methoden addTagWithoutMutex... umbennenen
//TODO on Long click für info was ein menü button in der actionbar macht macht
//TODO updateMissingSubs/Tags überarbeiten
//TODO statt Tga.compareTo dem TreeSet einen eigenen Comparator geben
//TODO Selection-Actionmode hinzufügen, statt alles einzeln zu machen
//TODO logs in data abspeichern -- werden zu einem server geschickt
//TODO CPU leistung optimieren Code Kommentieren und übersichtlicher gestalten
//TODO preloading besser gestalten, sodass ladezeiten besser sind
//TODO mp4 implementieren
//Bugs
//TODO Es lädt automatisch Manager seite 2?
//TODO Selectableview when man beim auswählen auf ein Menu (Toolbar) klickt, kann man es immer aktivieren
//TODO nachschauen der downloadservice richtig runterlädt...
//TODO tag 6+girls geht nicht
//TODO discord schauen
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub //TODO icon updaten, unter anderem bei discord... sieht es komisch aus
//TODO Toolbar menu aktualisiert sich noch nicht immer?
//Wahrscheinlich gefixt
//TODO Tag.getCorrectTagType besser einbetten


//TODO SeveraltagsPreviewLayout
//  Zeigt mehrere tags an
//  der aktuelle tag wird in der Actionbar angezeigt

//Update Raus Bringen TODO List
//1: Build.gradle version updaten
//1.1: Changelog hinzufügen
//2: Twitter Post raushauen
//3: Discord Post raushauen
//4: Build auf Github hochladen (apk in format VersionName.apk)