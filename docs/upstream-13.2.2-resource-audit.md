# Resource audit of the Telegram 13.2.2 import

Scope: BananaGram import `4c770f79c2043a55f5518358b1547d3365ec5586`,
original upstream commit [`21f7d109da6a`](https://github.com/DrKLO/Telegram/commit/21f7d109da6ab3506c9e9577f98c6a38771ab335).
This records ownership defects and bounded-resource changes, not a measured
explanation of the rc.16 crash report's total native memory.

## Imported widget engine

- `WidgetRemoteEngine.prewarm()` originally acquired a permanent application
  hold. It prevented idle unbinding even with zero widget sessions. This was an
  explicit startup-latency tradeoff in upstream, not an unreachable object leak.
  The hold now expires after 30 seconds and is released when the application
  backgrounds. Actual sessions retain their independent holds and the existing
  idle shutdown grace.
- Each `WidgetRemoteEngine.Client` retained the peak capacity of its multipart
  `ByteArrayOutputStream` after `reset()`, potentially 16 MiB per live client.
  Completed or rejected arguments now drop scratch buffers larger than 64 KiB;
  the per-argument limit remains enforced.
- `WidgetFrameNode`'s new software snapshot path created a temporary hardware
  bitmap wrapper and copied its pixels, but left wrapper cleanup to GC. The
  wrapper is now recycled in `finally` after producing the independent copy.
  Discarding an unpublished node also clears its owned software snapshot.
- The isolated widget process must skip client initialization during Android
  configuration changes as well as in `Application.onCreate`. The configuration
  callback now has the same process guard.

## Screen capture

The new Android 12+ resize branch created an unowned `Surface` wrapper on every
format change. Initial display creation had the same ownership gap. The capturer
now retains and reuses its own wrapper, releases it after its `VirtualDisplay`,
and cleans partial startup/resize failures. It never releases the helper-owned
`SurfaceTexture`. Live MediaProjection permission and rotation still require
device/emulator acceptance outside the unattended smoke suite.

## Glass lifecycle found during integration

These paths predate the 13.2.2 import, but are relevant to the new appearance
selector's view rebuilding:

- A bubble lens used full-resolution intermediate nodes and retained them while
  its cell was hidden. Intermediate nodes are now downsampled threefold in each
  dimension and their display lists are released on hidden/detached states.
- Bitmap captures used width for both dimensions and scaled their recording
  canvas in the wrong direction. Rectangular and downsampled captures now retain
  their aspect ratio and coordinate mapping.
- Baked bitmap/color wallpaper captures used a timer instead of content
  revisions. Bitmap pixels, matrices, nested wrappers and crossfades now
  invalidate the recorded source when they change.
- A surviving fragment's glass engine retained registrations from old view
  trees across rebuilding. Registrations owned by the replaced root are now
  pruned while current/unparented/separate-root views remain intact. Retired
  capture positions also discard their RenderNode display lists.

## Verification and limits

The instrumentation suite contains direct tests for multipart buffer ownership,
service warmup expiry, offscreen widget snapshots, source revisions, rectangular
captures, bubble geometry and hidden-cell cleanup. The suite runs on Android
13–16 through the repository's Android checks workflow.

Screen counts in a crash report are weak-reference samples. A destroyed screen
still appearing in that sample does not by itself establish a leak. Retained
heap paths and physical-device GPU measurements are needed to attribute the
reported native memory or claim an overall memory reduction.
