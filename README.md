# QuickLyric

QuickLyric is an android app that fetches your lyrics for you almost instantly. Its main perks is that it's very fast, it doesn't have ads, it features Music ID, a search engine, the downloading of lyrics on your device and some handy settings. While most lyrics app require you to type the name of the song you're searching for or annoy you with tedious notifications every time the song has changed, QuickLyric automatically detects what song is playing on your device and instantly delivers the lyrics to you. The app's also very pretty since it mimics the design of the Google Play apps for Android.

![alt text](http://i.imgur.com/VlSv894.png "Banner Logo")

QuickLyric is of course written in Java and runs on phones and tablets equipped with *at least* Android 2.2 Froyo. The build system relies on Gradle and Android Studio is therefore recommended.

## Features

The app features a slick drawer menu on the left of the screen. Here are the contents of that menu :

* **Lyrics** - Shows the lyrics of the song you are listening to at the moment. In the action bar you'll find buttons to update the lyrics when the next song has started playing, to download the lyrics on your device (for offline usage) and to share the URL to a friend.
* **Music ID** - Just like Shazaam, Soundhound & co., QuickLyric can recognize music via the microphone of the device. Once the song is identified, QuickLyric shows you the lyrics.
* **Local Storage** - In this screen, you can see a list of the lyrics you've chosen to download.
* **Search** - Search and even find lyrics.
* **Settings** - QuickLyric offers a minimal choice of settings to avoid an overwhelming amount of possibilities. 3 choices are offered to the user : whether transitions will be animated, whether the app should try to find lyrics for tracks that are longer than 20 minutes (those are presumably podcasts and not songs) and whether to automatically update the lyrics when the song has changed, without having to press the refresh button in the action bar. In this screen You'll also find the "About" info.

## Notes
* You might find some commentaries/var names in french. I'm really sorry if that's the case. Report a bug and I'll be happy to correct it.
* The app is currently in RC2. Almost ready for public release, needs some more testing.
* The app supports all screen formats, but I only own a Nexus 4. Therefore you might experience issues on bigger screens. 7" tablets should be fine but I'm not too sure about 10" tablets since I don't own one.

## Build
* Download the sources 
* Get an API key for both Last.FM (mandatory) and Gracenote (optional). Put those in be.geecko.quicklyric.Keys .
* If you want the MusicID feature : head over to  developer.gracenote.com, download the mobile library, put it in the libraries folder and sign up for a license. (you need armeabi/, armeabi-v7a/ and GN_Music_SDK.jar in the /QuickLyric/libs folder)
* Input your signature keystore, login and passwords into the QuickLyric/build.gradle file
* cd to the sources and execute "gradle build"
**OR**
* Import the project with the LATEST version of Android Studio. Android Studio updates often break compatibility with older versions of Gradle, meaning the build.gradle file has to be updated regularly.

## APK
Please don't ask for a compiled apk. QuickLyric will hopefully someday be distributed via some app store, maybe F-Droid, maybe the Google Play Store. But until the licensing issues are sorted out, no apk will be distributed.

Finally, if you've compiled an apk of QuickLyric, please please don't distribute it before I do. I spent a lot of time on this project, I hope you can respect that. :)

## Credits

Credits are due to : Gracenote, Last.FM, [ProgressWheel](https://github.com/Todd-Davies/ProgressWheel),  Roman Nurik for his scroll tricks, Ficus Kirkpatrick for Volley, Jake Wharton for his nineoldandroids lib, [ShowcaseView](https://github.com/amlcurran/ShowcaseView).

## Screenshots

![Imgur](http://i.imgur.com/bKq0GLW.png)
![Imgur](http://i.imgur.com/mCTmuGE.png)
![Imgur](http://i.imgur.com/7ZPOTYh.png)
![Imgur](http://i.imgur.com/RtIdK24.png)
![Imgur](http://i.imgur.com/dXlxpmJ.png)
