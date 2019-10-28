package de.yochyo.yummybooru

//Momentan
//TODO selected tags sind falsch markiert

//Features
//TODO support animated_png
//TODO Tests einfügen
//TODO multiselect pictureactivity kann auch einen tag hinzufügen (elsword eve_(elsword) oder so)
//TODO folder in taglist einfügen
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
//TODO SeveralTagsPreviewLayout -< siehe unten
//TODO backuputils versionunabhängig machen
//Optimizations
//TODO backups erstellen muss mit verschiedenen versionen kompatibel sein
//TODO strings in string.xml nutzen
//TODO statt Tga.compareTo dem TreeSet einen eigenen Comparator geben
//TODO logs in data abspeichern -- werden zu einem server geschickt
//TODO CPU leistung optimieren Code Kommentieren und übersichtlicher gestalten || preloading besser gestalten, sodass ladezeiten besser sind
//TODO mp4 implementieren
//Bugs
//TODO default save path funktioniert nicht immer? nochmal probieren, lag vllt. daran, dass die preferences nicht gelöscht wurden
//TODO Selectableview when man beim auswählen auf ein Menu (Toolbar) klickt, kann man es immer aktivieren
//TODO tag 6+girls geht nicht
//TODO discord schauen
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub //TODO icon updaten, unter anderem bei discord... sieht es komisch aus
//TODO Toolbar menu aktualisiert sich noch nicht immer?
//Wahrscheinlich gefixt
//TODO nachschauen der downloadservice richtig runterlädt...
//TODO updateMissingSubs/Tags überarbeiten //kann man es auch einfach so lassen (überprüfen ob es gerade funktioniert)
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