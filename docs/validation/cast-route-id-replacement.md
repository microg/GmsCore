# Cast receiver-ID replacement

A resolved NSD service name can change its advertised receiver ID. Previously,
`serviceCastIds.put(name, id)` replaced the mapping but left the unused previous
receiver in the published route list. Losing the service later removed only the
new receiver.

The provider now removes the previous receiver when its ID changed, no other
service name references it, and the existing `isInUse` check says it is unused.
A second service alias or a held route controller preserves the old route.
The existing controller-release cleanup and final `publishRoutes()` remain in use.

## Focused regression, 2026-10-04

The existing `CastRouteAliasTest` now has six cases: its three original alias/loss
cases and three additions covering sole-service replacement, another live alias,
and a held controller followed by release. The sole-service case also checks
same-ID rediscovery and subsequent loss.

Both versions compiled the complete provider and the two unchanged helper
sources, then ran this class with Java 17 (`--release 8`), JUnit 4.13.2,
Robolectric 4.14.1, and Android API 28. Existing Android/AndroidX/Cast dependencies
were reused offline; route mapping was not reimplemented in a test substitute.

| Provider version | Compilation | Result |
| --- | --- | --- |
| Before | Passed | 5 passed, 1 failed: expected `[new-id]`, observed `[old-id, new-id]` |
| After | Passed | `OK (6 tests)` |

The failing method before the fix was
`replacingTheOnlyServiceRemovesItsPreviousReceiver`. The same test source ran
against both versions. The controller-release case uses the real route
controller and drains the Robolectric main looper before asserting cleanup.

## Source identities

| Source | Git blob |
| --- | --- |
| Original provider | `751d4813177a4c267e3aaabd3db76b47eaa243e4` |
| Fixed provider | `e490a3a9748fa6cfb20a24cd1d390b27dc1182a7` |
| Expanded existing test | `949b3c6e7b020510582b58054533823e09abd7b4` |
| Unchanged CastDiscoveryState | `ce286bed813ab14c7748749d49581bb220a96b83` |
| Unchanged RemotePlaybackControlActions | `03fdd81f93afb0ea6fdf2425d2ae73628e6a7310` |

The helper bytes were checked against branch commit
`6384d9d9acc4975ab0011006e339a2d71974bc59`. Provider CRLF and test LF line endings
are preserved.

This verifies provider route replacement and retention under Robolectric.
It does not establish physical mDNS/Chromecast behavior or a complete Android
project build.
