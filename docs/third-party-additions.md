# Sources and licenses for the new engines

- `BananaOfflineVoice` and `VoiceResampler` adapt the GPL-2.0-or-later
  `GlassgramVoice` implementation from
  https://github.com/shashachkaaa/Glassgram/tree/b30831c800ab.
  Android integration, download bounds and ownership guards are maintained here.
- Vosk Android 0.3.75: https://github.com/alphacep/vosk-api (Apache-2.0).
  JNA 5.18.1: https://github.com/java-native-access/jna (LGPL-2.1-or-later OR Apache-2.0).
  The selected small RU 0.22 / EN-US 0.15 models are downloaded on explicit request
  from https://alphacephei.com/vosk/models, whose catalog lists Apache-2.0 for both.
- youtubedl-android library/FFmpeg 0.18.1:
  https://github.com/yausername/youtubedl-android and the maintained
  https://github.com/JunkFood02/youtubedl-android distribution (GPL-3.0).
  Its yt-dlp/Python/FFmpeg/QuickJS distributions retain their bundled notices.
  These additions are combined under GPL-3.0-or-later as permitted by Telegram's
  GPL-2.0-or-later source headers. Existing source files retain their notices.
- Instrumentation `voice-fixture.ogg` derives from Vosk's public
  `python/example/test.wav` at commit
  `78e66149f8ac56a64aa64306c1e8fc132c4eb154` (Apache-2.0):
  https://github.com/alphacep/vosk-api/blob/78e66149f8ac56a64aa64306c1e8fc132c4eb154/python/example/test.wav.
  Conversion: `ffmpeg -i test.wav -c:a libopus -b:a 24k voice-fixture.ogg`.
  Fixture SHA-256: `abfe0a498f9c14e563334047df2973b6ca23f9bb0c2e67db56e9feaaa839f510`.
  This synthetic/public test recording contains spoken digits, with no user audio.
