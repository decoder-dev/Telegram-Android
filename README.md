# AyuGram for Android

![AyuGram Logo](.github/AyuGram.png)

## Features

**AyuGram4A** is built on top of exteraGram with reworked patches from Telegraher:

- Built with official keys
- No emulator detection
- Screenshots in secret chats
- Messages history (+ deleted ones)
- No ads
- Probably something else, check commits

...and **ghost mode**.

AyuGram4A does **NOT** include proprietary exteraGram features.

## Downloads?

Follow our [Telegram channel](https://t.me/ayugram1338).

Alpha versions can be downloaded
from [GitHub Actions](https://github.com/AyuGram/AyuGram4A/actions/workflows/release.yml).

## Want to throw some money?

Developing AyuGram is not such a simple task.
We'd be grateful for any donation <3

In addition, you'll receive **AyuGram MVP** status.

## AyuGram MVP? Seriously?

Not the best name, I guess, but why not.
You'll get a supporter mark in profile and access to the official AyuSync server.

## AyuSync? What is it?

**AyuSync** is our synchronization service available to **AyuGram MVP** users.
It can sync read states and message history.

You can implement a sync server by yourself if you don't trust us and specify it in the AyuGram
Preferences. If you do so, you don't need **AyuGram MVP** status.

Server specification can be found [here](https://github.com/AyuGram/AyuSyncSpecification).

## Why there's no `X` feature from Telegraher?

Because I'm too lazy, and some Telegraher's features are useless for most of the people.

## Want to contribute?

I'd be grateful for any contribution, since I don't really like Java. :)

Work on any feature you want.

## Want to fork?

If you're making a fork, you **should** specify **AyuGram** & **exteraGram** in credits.
Otherwise, open source won't really work.

Depending on your GitHub profile, we'll decide whether to send an invitation to the proprietary repo or not.

And, if you're a kid that doesn't really want to give credits properly - go and cry writing boilerplate code.

## How to build

1. Clone source code using `git clone https://github.com/AyuGram/AyuGram4A.git`
2. Open the project in Android Studio. It should be opened, **not imported**
3. Implement the `AyuMessageUtils` & `AyuHistoryHook` classes. It's not that hard, but if you're
   making your **very** own fork, then you should take some time to write this part of code
4. Create dummy classes for extera's proprietary methods & classes (in `boost` folder)
5. Replace `google-services.json` (we don't want to see crash reports from your app...)
6. Generate application certificate and fill API_KEYS:
   ```
   APP_ID = 6
   APP_HASH = "eb06d4abfb49dc3eeb1aeb98ae0f581e"
   MAPS_V2_API = NA
   
   SIGNING_KEY_PASSWORD = <...>
   SIGNING_KEY_ALIAS = <...>
   SIGNING_KEY_STORE_PASSWORD = <...>
   ```
7. You are ready to compile `AyuGram`

- **AyuGram** can be built with **Android Studio** or from the command line with **Gradle**:

```
./gradlew assembleAfatRelease
```

## AyuGram Localization

[![Crowdin](https://badges.crowdin.net/ayugram/localized.svg)](https://crowdin.com/project/ayugram)
[![Crowdin](https://badges.crowdin.net/exteralocales/localized.svg)](https://crowdin.com/project/exteralocales)

We have our own [Crowdin](https://crowdin.com/project/ayugram).

But since AyuGram is based on **exteraGram**, also join their project
at [Crowdin](https://crowdin.com/project/exteralocales)!

## Credits

- [exteraGram](https://github.com/exteraSquad/exteraGram)
- [Telegraher](https://github.com/nikitasius/Telegraher)
- [Cherrygram](https://github.com/arsLan4k1390/Cherrygram)
- [Nagram](https://github.com/NextAlone/Nagram)
- [Telegram FOSS](https://github.com/Telegram-FOSS-Team/Telegram-FOSS)
