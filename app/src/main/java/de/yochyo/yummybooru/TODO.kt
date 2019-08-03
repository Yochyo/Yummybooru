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





//Optimizations
//TODO logs in data abspeichern
//TODO toolbars transparent machen
//TODO AddTagDialog vorschläge verstecken wenn der erste eintrag der mom. Eingabe entspricht
//TODO seite laden bevor man das ende erreicht previweaactivity
//TODO pictureactivity padding preview kleiner
//TODO Mainactiviy mom. ausgewählte tags anzeigen
//TODO subs haben negative zahl wenn man kein internet hat
//TODO locks überarbeiten
//todo normale downloads auch über einen service laufen lassen?
//TODO beim Profiler ist die CPU belastung heavy
//TODO downloader braucht ne lock, synchronisieren
//TODO Code Kommentieren
//TODO strings vol pref_general in string.xml
//TODO md5 des bildes benutzen
//TODO namen der xml elemente optimieren
//TODO Wenn man den ausgewählten server ändert, wo wird der wieder ausgewählt
//TODO subscription acitivty anders regeln, count wird bei jedem scrollen neu geladen
//TODO tag array entfernen und alles in strings speichern (damit man nicht immer .toTagString machen muss)
//TODO Ende Der Seiten erreiht benachrichtigung kommt zu oft
//Bugs
//TODO menüs in toolbars werden nicht actualisiert
//TODO when z.B. ein download crasht, muss ein event (oder awaitPicture...) abgebrochen werden
//TODO bitmap = BitmapFactory.decodeStream(stream)!! in downloader schlägt selten fehl //wahrscheinlich weil mein png/jpg
//Nettes zeug für irgendwann
//TODO Subs nicht nur mit :id>X filtern, sondern auch per hand