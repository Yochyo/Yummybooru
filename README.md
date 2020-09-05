# Yummybooru for android :3

This application's goal is to be the best available booru client in all aspects. To suggest a feature, join our discord server.
https://discord.gg/tbGCHpF

## Tutorial
#### Start screen
<img src="https://i.ibb.co/3FCQzHF/Screenshot-Mainactivity.png" height="300" hspace="20"><img src="https://i.ibb.co/ZKHWwfp/Screenshot-Mainactivity2.png" height="300" hspace="20">
- You can add a new server by clicking on the `+` icon
- Pressing the search icon allows you to choose tags to search for

- You can select one or more tags (danbooru has a limit of 2 tags) before searching
- Click on the search icon to start the search

- The plus icon allows you to search and add tags to your history
- The X icon will deselect all selected tags

#### Preview images
<img src="https://i.ibb.co/SRZRwZQ/Screenshot-Previewactivity.png" height="300" hspace="20">
- The +/- icon allows you to add/remove a tag to your history
- Click the heart icon to (un-)favorite a tag
- Long click an image or click on "Select all" in the toolbar to select one or more images. You can (de-)select more images by clicking or dragging

#### Large images
<img src="https://i.ibb.co/FBC6XQV/Screenshot-Pictureactivity.png" height="300" hspace="20"><img src="https://i.ibb.co/cFqhtbp/Screenshot-Pictureactivity2.png" height="300" hspace="20">
- Swipe up or press the save icon to save the image to your storage
- Swipe up two times to add all authors to your tag history
- Swipe left/right to see the last/next image
- Swipe down to go back to the preview images

- Swipe left from the right border or click on the info icon to see the posts tags

#### Followed tags
<img src="https://i.ibb.co/BN2XS3N/Screenshot-Followingactivity.png" height="300" hspace="20">

- Following tags are the major feature of Yummybooru. Following a tag allows you to see all images posted since the last time you checked them.

- When following a tag, you can see new posts by clicking on "following" in the left navigation view of the start screen
- You will see all followed tags and how many new posts were made since the last time you checked them
- By clicking the update icon or selecting one or more tags, you can set the last time you checked of all/the selected tags to now

## Settings
#### Downloads
- "Hide images from gallery" will create a .nomedia file. Gallery apps will ignore this folder, so the images are not shown in your gallery
- "Download extra large picture" will save the biggest possible resolution of an image. I recommend switching this on except if you wifi is reeaaaallllly slow. Switching it off
 will result in smaller images with worse quality being saved to your storage

- "Download ugoira as webm" should be enabled as well. An ugoira is a format made by pixiv, similar to a gif. If you disable this, a .zip file will be downloaded instead
- "Parallel downloads (default 5)" is more or less self-explanatory. Reduce the amount of parallel download if you experience long loading times for a single image
- "Post per page" will change the amount of preview images loaded with each page

#### Preview images
<img src="https://i.ibb.co/SRZRwZQ/Screenshot-Previewactivity.png" height="300" hspace="20"><img src="https://i.ibb.co/M5g0cjK/Screenshot-Previewactivity2.png" height="300" hspace="20">
- Enable "staggered mode" to use staggered mode (right screenshot). I recommend setting the amount of columns to 2

#### Large images
- "Preload large images (default 1)" will load more large images in the background, for a smoother experience when swiping left/right.
- You can enable "Click for next image" to replace swiping left with a click

#### Backups
- "Create backup" will create a backup of your settings, tag history and servers in your save directory
- "Restore backup" will restore a backup

## Thanks to
- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [PhotoView](https://github.com/chrisbanes/PhotoView)
- [Glide](https://github.com/bumptech/glide)
- [Google Firebase](https://firebase.google.com/)
- [Anko SQLite](https://github.com/Kotlin/anko/wiki/Anko-SQLite)
- [Drag Select Recycler View](https://github.com/afollestad/drag-select-recyclerview)
- [Disk LRU Cache](https://github.com/JakeWharton/DiskLruCache)
- [AppIntro](https://github.com/AppIntro/AppIntro)
