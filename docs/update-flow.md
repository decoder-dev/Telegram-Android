# BananaGram Android updates

## User experience

Settings → BananaGram Extras → Updates shows the installed version, selected channel,
last check time and available release. The page has a **Check now** action, a **Stable /
Release candidate** channel selector, **Check on launch**, and an optional **Download
automatically on unmetered Wi-Fi** switch. Manual download is always available when
automatic download is off.

Automatic checks run when the app returns to the foreground and are rate-limited to
once per 24 hours. Automatic downloads require an explicit opt-in and an unmetered
Wi-Fi network. The app never installs or restarts itself. When a verified APK is ready, the
user reviews the update and Android's package installer asks for confirmation. If
Android requires permission to install packages from this source, the app opens the
system setting and resumes only after the user returns. This follows Android's
[`canRequestPackageInstalls()`](https://developer.android.com/reference/android/content/pm/PackageManager#canRequestPackageInstalls())
and [unknown-app installation guidance](https://developer.android.com/studio/publish#publishing-unknown-apps).

## Release selection

- Query releases from `decoder-dev/Telegram-Android` over HTTPS. Stable mode ignores
  prereleases; RC mode allows prereleases. A new RC installation defaults to the RC
  channel; an explicitly stored channel choice is preserved. Draft releases are always ignored.
- Require an APK asset matching the installed ABI (or a universal APK), package id,
  and a version newer than the installed build. A source-only release is not offered
  as an installable update.
- Select by semantic version (`major.minor.patch`, with `-rc.N` ordered before the
  final version), never by lexicographic tag order or release date.
- The release metadata's SHA-256 digest is required. Verify it after download and
  before passing the APK to Android. Verify package id and version from the APK;
  compare the APK signing certificates with those of the installed application before
  installation. Android's installer performs its own signature verification.
- Do not follow arbitrary asset URLs. Require HTTPS GitHub release asset hosts,
  enforce a bounded download size, and discard partial or invalid files.

## Delivery and recovery

Use GitHub Releases as the sole update feed; the release workflow publishes a signed
universal APK, SHA-256 digests, certificate details and the matching R8 mapping. Keep stable and RC tags distinguishable and use
the in-app channel preference to opt into RCs. A failed or cancelled download remains
resumable; an invalid hash, wrong package, downgrade, or mismatched signature is
rejected and the temporary file is deleted. The app displays the release notes before
download and retains the previous installed app and its data until Android confirms
the user-approved installation.

## Rollout order

1. Publish an RC with matching signed APKs and digests.
2. Verify update detection, channel filtering, ABI selection, interrupted download,
   digest rejection and the Android install-confirmation flow on a clean emulator.
3. Promote the same tested commit to a stable release; do not rebuild under the same
   version tag.

This document describes the intended flow. The update UI and release feed must pass
the rollout checks before the updater is advertised as ready.
