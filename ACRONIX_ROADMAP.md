# Acronix Launcher — roadmap and acceptance criteria

Working branch: `acronix-v0.1`. Upstream baseline: Grace Launcher v1.2.4.
Target: Galaxy Z Fold 8, Android 17, folded and unfolded.

## 0. Reference recording (14 Nov 2026)

A user-provided recording of Grace Launcher v1.2.4 demonstrates settings for:
- Productivity; clock/calendar and agenda; media; popup menus.
- Home, app list, widget screen, categories, work profile, private space, folders and stacks.
- AMOLED appearance, Josefin Sans typeface, Horux Exported Icon Pack.
- Gestures, status screen, search settings and related appearance.
- Personal contact/account/calendar details from the recording are **not** committed.

The recording is a **visual reference**, not an export of the private Room database.
Do not overwrite the existing Grace installation.

## 1. Search order and Android settings — implemented in v0.1.2, validate on device

- Persist a category order; default stays Apps → Contacts → Web → Device (Device off by default).
- In Search settings, move Contacts, Apps, Web, Device up/down.
- Optional Android settings results for developer options, Wi-Fi, Bluetooth, display, battery saver, apps, notifications, accessibility and security.
- Open through documented public `android.provider.Settings` intents with failure fallback.
- Do not request privileged permissions or use a background search index.
- QA: Contacts → Apps → Web priority, then search for "desarrollador" (once Device enabled); verify settings page opens on Samsung; restart and confirm order is retained.

## 2. Import, export and share configuration

- Entry in Settings: Import, Export, Share.
- User-initiated SAF document picker and Android Sharesheet; no broad storage permission.
- Versioned integrity-checked local archive: Room database, SharedPreferences, local font/icon files, favorites, layout, folders, settings and metadata; exclude caches and tokens unnecessary for migration.
- Import after explicit confirmation with rollback snapshot and safe database lifecycle.
- Host IDs and bindings for Android widgets may need renewed authorization: never silently assume widget providers will remain bound.
- Upstream Grace v1.2.4 currently has **no supported comprehensive export path**. Acronix's own backup implementation cannot extract Grace's private app data on unrooted devices. This requires separate, user-authorized migration options.
- QA: backup → edit favorites/folders → restore with correct content and no leaked backup on shared storage.

## 3. Widget reliability

- Investigate AppWidgetHost and persisted widget IDs when providers change, reboot, Fold state changes or process restarts.
- Rebind unavailable widgets through Android's official consent UI; preserve placement/size where possible.
- Recover Contact widget failures without rebuilding all widgets.
- Prefer platform callbacks rather than polling.

## 4. Vivaldi pinned shortcuts

- Inspect ShortcutManager/LauncherApps pin requests and saved shortcut identity/intent.
- Handle stale shortcut IDs/activities after updates; show a repair/re-pin action while preserving placement.
- Validate Vivaldi, alternative browser, reboot, app update, Fold transitions; don't retain obsolete URI grants.

## 5. Battery and performance

- Measure before/after CPU wakeups, wake locks, battery stats, memory, startup and jank.
- No periodic polls for notifications/widget/shortcut recovery.
- Coalesce application inventory refreshes, cache icon decoding, dispose off-screen observers.
- Do not claim better battery life without repeatable measurements on device.

## 6. Swipe-on-label notifications and multicolor dots

- Swipe right-to-left **over a home label** to show notifications only for that application, or only for the applications belonging to the selected group.
- Each notification is a dot, colored per application (e.g. app A=1 dot and app B=3 dots).
- Decide accessible overflow when counts are high, and count distinct active notifications without double-counting summaries.
- Respect profiles, privacy, revoked permissions and competing swipe gestures.
- **Blocked pending permission design**: debug v0.1.1+ removed NotificationListenerService and AccessibilityService from merged manifest after a Play Protect warning. Never silently re-enable privileged services or bypass Play Protect. Evaluate explicit user-opt-in, relevant SDK/platform rules, legitimate distribution, and permission safety before implementation.
- Notification text never leaves the device.

## Release safety

- Keep original Grace Launcher installed and its data untouched.
- Preserve GPLv3/attribution.
- Validate each APK via GitHub Actions and on both Fold displays.
- Retain Play Protect and Knox status, never request root.
