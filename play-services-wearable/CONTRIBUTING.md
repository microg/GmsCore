# Contributing to Wear OS Support

## Development Setup

```bash
git clone https://github.com/microg/GmsCore.git
cd GmsCore
git checkout -b feature/wearos-enhancement
```

## Project Structure

```
play-services-wearable/
├── core/
│   ├── build.gradle
│   └── src/
│       ├── main/java/org/microg/gms/wearable/
│       │   ├── CompanionPairingManager.java   # Device pairing lifecycle
│       │   └── WearableServiceImpl.java       # Channel & routing service
│       └── test/java/org/microg/gms/wearable/
│           ├── CompanionPairingManagerTest.java
│           └── WearableServiceImplChannelTest.java
├── docs/
│   ├── WEAROS_ARCHITECTURE.md
│   ├── API_REFERENCE.md
│   └── TESTING_GUIDE.md
├── CHANGELOG.md
├── README.md
└── run_tests.sh
```

## Coding Standards

- Follow Android Java code style (AOSP)
- Use `android.util.Log` with appropriate log levels: `Log.d()` for debug, `Log.w()` for warnings, `Log.e()` for errors
- All public methods must have Javadoc
- Bluetooth operations run on background threads via `Handler`
- Thread-safety: use `ConcurrentHashMap` for shared state, `synchronized` for critical sections
- Error handling: return error codes, never throw unchecked exceptions to callers
- Constants: UUIDs and prefixes in `CompanionPairingManager`, paths in respective classes
- Preferences: use `SharedPreferences` for persistent state, apply() not commit()
- Null safety: check all Binder callback parameters for null before invoking

## Test Guidelines

- Unit tests use Robolectric (no emulator needed)
- Instrumented tests use AndroidX Test + emulator
- Each pairing/scenario gets at least one test
- Test both success paths and error/edge cases
- Mock system services when testing Bluetooth-dependent code

## Commit Convention

```
feat: short description
- detailed bullet points

test: short description
docs: short description
fix: short description
```

## CI Pipeline

All PRs trigger:
1. Unit tests (Robolectric) — 2 min
2. Lint checks — 1 min
3. Assemble (Debug + Release) — 3 min

## Review Checklist

- [ ] All new public methods have Javadoc
- [ ] Unit tests pass: `./gradlew :play-services-wearable:core:test`
- [ ] Lint passes: `./gradlew :play-services-wearable:core:lint`
- [ ] No hardcoded strings (use string resources)
- [ ] Bluetooth permissions declared in manifest
- [ ] Thread-safe for concurrent access
- [ ] Backward compatible (API 18+)
