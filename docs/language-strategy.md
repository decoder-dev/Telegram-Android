# Java, Kotlin and Rust

Decision accepted on 2026-11-01. Implementation starts **after the feature audit**;
the audit is still open. This decision does not authorize a wholesale rewrite.

Keep Telegram's existing Java implementation and native libraries, so upstream
updates remain reviewable. Fix defects in the language of the surrounding code.
Write new Android screens and settings in Kotlin, using the existing fragment,
theme, account and lifecycle APIs. Do not introduce Compose or convert existing
Java screens just to change their language. Java and Kotlin interoperation is
supported by [Android's incremental adoption guide](https://developer.android.com/kotlin/add-kotlin).

After the audit, configure Kotlin in the application library before adding the
first Kotlin screen. Check Java/Kotlin compilation, R8 release builds and the
Android 13–16 UI matrix. The audit repaired the bookmark flow inside the existing
Java Extras screen; a future dedicated screen should follow the Kotlin decision.

Consider Rust only for an isolated file processor, parser or computation with a
small, explicit interface. Measure the existing Java implementation first. Compare
the whole operation, including JNI calls, buffer copies and allocation, on identical
fixtures and devices: cold/warm latency, median and p95, CPU time, peak memory,
APK size and startup cost. Record device, build variant, ABI and input sizes.
Adopt Rust only when those results justify its build and maintenance cost. No Rust
module is selected or added by this decision.

JNI must use bounded inputs and explicit ownership, translate errors at the
boundary and keep panics from unwinding into Java. Preserve cancellation and
account checks. Verify every shipped ABI and 16 KiB native-library compatibility.
Keep platform cryptography on the existing Android/JCA APIs. A Rust parser does
not by itself replace the process isolation of the Java regex evaluator.

The linked [AOSP Rust patterns](https://source.android.com/docs/setup/build/rust/building-rust-modules/android-rust-patterns)
describe platform modules and native interoperation. This Gradle app requires its
own integration; copying an `Android.bp` example does not configure its build.

The audit must distinguish source wiring, local regression tests, physical-device
measurements and authenticated Telegram scenarios. A passing source scan cannot
close the audit or justify an RC by itself.
