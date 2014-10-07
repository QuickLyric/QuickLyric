# QuickLyric

QuickLyric is an android app that fetches your lyrics for you almost instantly. Its main perks is that it's very fast, it doesn't have ads, it features Music ID, a search engine, the downloading of lyrics on your device and some handy settings. While most lyrics app require you to type the name of the song you're searching for or annoy you with tedious notifications every time the song has changed, QuickLyric automatically detects what song is playing on your device and instantly delivers the lyrics to you. The app's also very pretty since it mimics the design of the Google Play apps for Android.

![alt text](http://i.imgur.com/VlSv894.png "Banner Logo")

QuickLyric is of course written in Java and runs on phones and tablets equipped with *at least* Android 2.2 Froyo. The build system relies on Gradle and Android Studio is therefore recommended.

## Features

The app features a slick drawer menu on the left of the screen. Here are the contents of that menu :

* **Lyrics** - Shows the lyrics of the song you are listening to at the moment. In the action bar you'll find buttons to update the lyrics when the next song has started playing, to download the lyrics on your device (for offline usage) and to share the URL to a friend.
* **Local Storage** - In this screen, you can see a list of the lyrics you've chosen to download.
* **Search** - Search and even find lyrics.
* **Settings** - QuickLyric offers a minimal choice of settings to avoid an overwhelming amount of possibilities. 3 choices are offered to the user : whether transitions will be animated, whether the app should try to find lyrics for tracks that are longer than 20 minutes (those are presumably podcasts and not songs) and whether to automatically update the lyrics when the song has changed, without having to press the refresh button in the action bar. In this screen You'll also find the "About" info.
* **System Integration** - Access the lyrics you want after you've identified a song with Shazam or Soundhound via the share button. Open URLs from AZLyrics, LyricsNMusic and LyricWikia directly with the app.
* **NFC** - Share lyrics with your friends via Android Beam.
* **More than 10 languages supported** - Including German, Greek, Spanish, French, Italian, Japanese, Dutch, Polish, Russian, Turkish and English.

## Notes
* The app is now ready for release.
* Because I could not test it on many different devices, I chose to use a bugtracker (ACRA). This causes F-Droid to mark my app with the "Tracking" AntiFeature.
* Your language is not supported? We'll take contributions from anyone :).
* The "Material" branch requires Android L-Preview and will be merged when Android L & its support libraries come out.

## Build
* Make sure you have the **latest** version of Gradle installed. You should have at least the version that is used in Quicklyric/build.gradle.
* Download the sources
* Input your signature keystore, login and passwords into the QuickLyric/build.gradle file
* cd to the sources and execute "gradle build"

## APK

Should be available soon on F-droid.

## Open Source Contributors

I welcome all forks & pull requests but *please*, if you want to fork QL, immediately update be.geecko.quicklyric.Keys with your own LastFM key.

## Credits

Credits are due to : Last.FM, Roman Nurik for his scroll tricks, Ficus Kirkpatrick for Volley, Jake Wharton for his nineoldandroids lib, [ShowcaseView](https://github.com/amlcurran/ShowcaseView).

## Screenshots

![Imgur](http://i.imgur.com/bKq0GLW.png)
![Imgur](https://i.imgur.com/bEdjfIn.png)
![Imgur](http://i.imgur.com/RtIdK24.png)
![Imgur](http://i.imgur.com/dXlxpmJ.png)
