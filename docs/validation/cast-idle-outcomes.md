# Cast idle outcome translation

The MediaRouter status parser previously mapped every Cast `IDLE` message to normal completion. A receiver playback error, explicit stop or replacement load therefore looked like a finished item to the requesting client.

The parser now uses `idleReason` only while the player is `IDLE`: `ERROR` becomes AndroidX `PLAYBACK_STATE_ERROR`; wire `CANCELLED` and `INTERRUPTED` become `PLAYBACK_STATE_CANCELED`. `FINISHED` retains normal completion. Other player states ignore idle reasons. To keep this a bounded compatibility repair, absent and unknown idle reasons retain the earlier fallback; the one-argument helper remains available. Session, position, duration, request ownership, lifecycle, direct STOP acknowledgement behavior and transport code are unchanged.

Protocol sources: [Google Cast media messages](https://developers.google.com/cast/docs/media/messages#MediaStatus) defines the wire reasons, including `CANCELLED` with two Ls and `INTERRUPTED` on a replacement LOAD. [AndroidX MediaItemStatus](https://developer.android.com/reference/androidx/mediarouter/media/MediaItemStatus) distinguishes failed, canceled and normally finished playback.

`RemotePlaybackIdleOutcomeTest` adds five focused parser regression methods. The first three assert the previously missing distinctions. The remaining methods preserve normal and legacy fallback outcomes, active states and session/position/duration metadata. Existing tests are unchanged.

This delivery contains source and regression cases. No Android module, APK or physical-receiver execution is asserted here; reuse the existing Cast validation environment for that execution rather than interpreting the source-assembly workflow as a test pass. No new bounty claim, acceptance or payment is implied.
