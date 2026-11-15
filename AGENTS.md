# AGENTS.md — Acronix Launcher development contract

These repository-local instructions apply to the whole repository when working on the
Acronix development branch. They complement the user's full requirements in
[PR #1, master brief](https://github.com/maikikiriki/The-Launcher/pull/1#issuecomment-6890928047),
\`ACRONIX_ROADMAP.md\`, \`ACRONIX-ESTADO.md\` and \`INSTALACION-SEGURA.md\`.
**The user wants finished, tested code, not another audit or sequence of requests to continue.**

## Mission and constraints

- Work in \`acronix-auditoria-desarrollo\` and update draft PR #1 targeting
  \`acronix-v0.1\`. Do not merge or alter \`master\`/upstream Grace.
- Preserve Grace v1.2.4 and all user data. Acronix is \`com.acronix.launcher\`.
  Target hardware is Galaxy Z Fold 8 SM-F971B, Android 17, both displays.
- Do not root, unlock bootloader, modify Knox, bypass Play Protect, expose
  secrets or personal data, or uninstall/clear Grace or Acronix to get around
  a package-signing or migration failure. Use official Android APIs and
  user-consented permissions only.
- Restoration in \`AcronixBackup.restore\` must remain DISABLED until recovery
  across Room/SharedPreferences/files after process death is demonstrated by
  adversarial and interruption tests. Never advertise non-restorable exports
  as verified backups. No unsafe fallback-to-destructive Room migrations.
- Never provision a private signing key into source control, public workflow
  artifacts or logs. Physical device installation, changing the default
  launcher and any potentially destructive action require explicit owner
  authorization. A GitHub Actions build is not physical Fold validation.

## Work autonomously instead of repeatedly planning

1. At session start: fetch current branch/PR and existing local work, read
   this file and the master brief; inspect CI from the **latest applicable
   commit**. Keep remote and local progress; do not force-push or reset work.
2. **Implement**, test and commit production code in cohesive batches.
   Each active session should attempt useful code + tests, not merely docs.
   Avoid posting repetitive "continue" messages and repeated audits.
3. For CI failures: retrieve logs and JUnit results, group failing cases by
   **root cause**, reproduce a representative test, fix implementation or
   genuinely faulty setup, run focused suite and then full suite. Never
   delete/ignore/skip failing tests or relax valid assertions to inflate the
   pass count. Record total/passed/failed/skipped before and after.
4. Do **not** create a new push every few seconds: the complete emulator suite
   is slow. Batch commits/pushes and let the prior GitHub Actions run finish;
   meanwhile implement independent features locally. A queued run is not a
   failure. Run the full CI on the final head of each coherent batch.
5. When one subtask needs the physical Fold, a user-provided signing key or
   another external dependency, record exactly what remains unverified and
   **continue working on unblocked modules**. Do not claim or pretend you can
   access the phone, secrets or execute between sessions.
6. Maintain one canonical progress tracker \`ACRONIX-ESTADO.md\`: feature,
   commit, production functionality, test evidence/link, outstanding blocker,
   next code task. Update draft PR succinctly after substantive milestones,
   not after every microchange.

## Priority delivery queue (code first)

### 1 — Eliminate emulator failures by cause, not by test count

Historical API 35 baseline run 43646827390: 98 failed / 450 completed.
Run 43689388816: 97 failed / 450 completed. Later results supersede these
numbers; obtain newest results before using them. Existing work includes:
shared Room migrations 1→34, fixes in launch-routing fixture, collapsed clock
test setup, first FoldLayouts data model, and a read-only candidate checker.

Prioritize cross-cutting Compose semantics/layout/host window/navigation
state and fixtures behind many failures. Fix Room and profile isolation
regressions without changing real-user behavior. Identify test expectations
that are obsolete only from explicit functional evidence. Target **zero
unexpected unit and instrumented failures** in a completed run, not merely a
green compilation job. Produce concise and verifiable diagnostics.

### 2 — Safe backup + reliable recovery (no premature enablement)

Implement an audited inventory/allowlist, globally coherent app-data
snapshot (coordinated writes + Room transaction + files/preferences staging),
versioned complete archive manifest, hashes, strict ZIP limits/rejection,
optional authenticated encryption for sensitive exports, persistent journal,
durable private rollback snapshots, startup recovery and idempotent restore.
Keep at most three verified automatic recovery points by default; preserve the
most recent good copy if a new write fails. Test crashes at every phase, schema
mismatches, truncation, malicious ZIPs, low storage, cancellation and recovery
after restart. Enable restore UI/API **only when ALL necessary tests pass**.
Otherwise retain safe disabled state, clearly explaining missing guarantees.

### 3 — Fold 8 functions fully wired into rendering

The \`FoldLayouts\` model/store is infrastructure, NOT an implemented feature
until connected to actual UI. Wire shared and independent outer/inner layouts
to an officially supported fold/window-state source; safe migration of
existing preferences, copy A↔B, no widget-ID cloning, responsive layout,
recreation/rotation/multiwindow and persistence. Unit and instrumented tests
must prove transitions keep widgets, favorites, folders and screen state.
Verify full Grace/Acronix features requested: Samsung Contacts widget repair
with platform authorization, Vivaldi/browser pinned shortcuts, work-profile
favorites/icon packs, nested folders, notification gesture and colored dots,
search priority, permissions and power efficiency. Explicitly label cases
which need real Fold 8 tests rather than claiming success in an emulator.

### 4 — Stable signing and no-data-loss installation path

Production release uses owner-controlled keystore only. Verify certificate
fingerprint with \`apksigner\`, versionCode monotonicity and app ID before
creating an upgrade path. A **different signing certificate** from the one
already installed cannot be silently treated as an in-place update.
Never suggest uninstalling/clearing user data to resolve incompatibility.
Prepare read-only ADB preflight and candidate validation; no install step
without user consent. Keep Grace installed and selectable, Knox and Play
Protect unchanged. Don't generate or publish a "stable" APK before gates.

## Acceptance/reporting format

For each milestone include: (a) changed code and tests/commit links; (b)
specific completed GitHub Actions run, CI job and numeric test results;
(c) verified functionality vs. still pending hardware/permission/key;
(d) unresolved risks and **the next code task**, which should be started
during the current session if technically possible.

Do not stop at a plan, documentation, green unit tests, a partially fixed
test suite or an unfinished proof of concept. Make as much verifiable progress
as session limits and safety permit. Do not fake completion or physical
validation. Only call a version installation-ready after all applicable
safety and CI gates pass.
