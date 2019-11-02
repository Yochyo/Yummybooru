package de.yochyo.yummybooru

//Momentan
//TODO selected tags sind falsch markiert

//Features
//TODO Show translations of images
//TODO allow to swipe through images in Pictureactivity
//TODO support animated_png
//TODO add tests
//TODO multiselect pictureactivity can add tags like elsword eve_(elsword)
//TODO Taglists add folders
//TODO add a width/height/order button
//TODO Mainactivity add unselect all
//TODO searchfield in SubscriptionActivity
//TODO Mainactiviy show currently selected tags
//TODO pixiv
//TODO safe Tags in png metadata
//TODO developer mode set app flags
//TODO combine Tags and Subs in one class
//TODO SeveralTagsPreviewLayout -< look at bottom
//TODO backuputils independent of version
//Optimizations
//TODO add constrains to database
//TODO add foreign key constraint to favorite database
//TODO use strings from string.xml
//TODO use Comparator in Treeset instead of Tag.compareTo
//TODO send logs to a server, too many logs fill up space
//TODO Optimize CPU performance, make code more readable | optimize preloading of app
//TODO implement mp4
//TODO use a wrapper for SubscriptionActivity's count values
//Bugs
//TODO some times are saved two times with (1) at the end of their name
//TODO Icons sind nicht ganz stimmig, fehlen bei der Nofication und wenn man auf die AppÜbersicht geht - round icon fehlt glaub //TODO icon updaten, unter anderem bei discord... sieht es komisch aus
//Wahrscheinlich gefixt


//TODO SeveraltagsPreviewLayout
//  Zeigt mehrere tags an
//  der aktuelle tag wird in der Actionbar angezeigt

//Update Raus Bringen TODO List
//1: Build.gradle version updaten
//1.1: Changelog hinzufügen
//2: Twitter Post raushauen
//3: Discord Post raushauen
//4: Build auf Github hochladen (apk in format VersionName.apk)