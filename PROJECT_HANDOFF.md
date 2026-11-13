# DisasterAlert — Project Handoff (read this first)

Hackathon project, **Domain 1: Disaster Management**.
Prime question: *"How can we build reliable situational awareness and coordinate the right response when a disaster is evolving faster than information can be collected and communicated?"*

This file is the single source of truth shared by:

| Who | Owns | Does NOT touch |
|---|---|---|
| **Claude (backend session; from S4 also the instructor)** | Spring Boot backend, Telegram bot, API contract, data model, test scripts. **From S4 (3 Nov 2026) Claude writes the step-by-step prompts** that Rajeev gives to Antigravity (section 8.2) and decides, per checkpoint, whether backend or frontend goes first | Android UI code |
| **Antigravity (frontend; from S4 also builds the backend when a prompt says so)** | Android app (Kotlin + Jetpack Compose), screens, map, camera, chat UI. For S4a-S4c and S6 it may also build the backend, **one side per prompt, never both at once** (section 8.2) | The API shape: never change it without a section 9 entry |
| **Rajeev (user)** | Integration, testing on a real phone, decisions, demo | — |

> **If you are a new Claude session:** read sections 1–9, check the Status Board (section 4), ask Rajeev which slice is next, then work **one small step at a time** (section 8). Rajeev should attach the backend project zip (without `build/`, `.gradle/`, `data/`) together with this file.

---

## 1. The idea in one paragraph

Existing public alerting in India is one-way. **SACHET** (built by C-DOT for DoT/NDMA, CAP-based) already sends geo-targeted SMS in all 36 States/UTs, and a **Cell Broadcast** upgrade (works without internet, one-way) was launched in May 2026. Those systems *warn*. They do not collect **what is happening on the ground**: who is safe, who needs rescue and where, which roads are blocked or cleared, where food/water/medical help is needed, or what field sensors are reading.

**DisasterAlert is the two-way coordination layer on top of a warning.**
Authorities trigger an alert for a danger zone → citizens (no app install; Telegram in the prototype) reply *I'm safe / I need help* and share location → help requests appear on a map of India → geotagged field reports and ESP32 sensor data feed an operational picture → authorities and reporters coordinate inside the app and push verified updates (road blocked/cleared, food points) back to citizens.

Pitch positioning (be accurate, judges will check): *"SACHET and Cell Broadcast warn. We close the loop: two-way response, field reports, sensors, coordination."* In production our authority app would feed alerts to SACHET/CAP and use SMS/Cell Broadcast for reach; we do **not** claim access to them.

Sources: https://www.etvbharat.com/en/bharat/ndma-tests-cell-broadcast-message-to-alert-citizens-on-severe-disaster-enn26050201915 · https://www.thehitavada.com/Encyc/2026/5/27/vid-residents-receive-first-ever-heatwave-warning-on-cellphones.html

## 2. Users

| Role | Installs app? | Channel |
|---|---|---|
| Citizen | **No** | Telegram bot (prototype). Production: SMS (DLT-registered) / WhatsApp / Cell Broadcast |
| Authority (NDMA/SDMA/district) | Yes | Android app |
| Field reporter | Yes | Android app (camera + GPS) |

## 3. Architecture and stack

```
 Authority / Reporter (Android, Compose)
        │  HTTPS-ready REST + Bearer token  (dev: http://127.0.0.1:8080 via `adb reverse`)
        ▼
 Spring Boot 4.1.2 (Java 17, Gradle Kotlin DSL, Hibernate 7, H2 file DB)
   ├─ NotificationChannel (interface) ── TelegramNotificationChannel   [SMS/WhatsApp can replace it]
   ├─ Telegram long-polling bot (registration, I'm safe / Need help, location)
   ├─ [planned] report storage + EXIF check + AI vision assist
   ├─ [planned] sensor ingest + analysis → draft alerts
   └─ H2 ./data/disasteralert2
        ▲
 ESP32 sensor nodes (Wi-Fi HTTP POST; BLE relay via phone = stretch)
```

- Dev machine: **Windows 11, PowerShell**, JDK **17.0.12**, phone on **USB** (`adb reverse tcp:8080 tcp:8080`).
- Android package: `com.rustbeatz.disasteralert`. minSdk 24, compileSdk/targetSdk 37, Compose + Material3.
- Telegram is the **prototype channel only** (time and cost; SMS in India needs DLT registration ≈ 1–2 weeks and ≈ ₹6k). Always describe it that way.

### Folders (today; ideally move into one repo with `backend/`, `android/`, `docs/`)
- Backend: `C:\Users\DELL\Desktop\Rajeev\disasteralert-backend`
- Android: `C:\Users\DELL\AndroidStudioProjects\DisasterAlert` (Gradle project root is the folder holding `settings.gradle.kts`; module is `app/`)

## 4. Status Board (update at the end of every slice)

| Slice | What | Backend | Android | State |
|---|---|---|---|---|
| S0 | Emergency broadcast → Telegram → *I'm safe / Need help* recorded | DONE, verified end to end on Rajeev's machine | Delivered (login, broadcast, reach count, result screen); **first run on the phone not yet confirmed** | CLOSE OUT |
| S1 | Three-tap help flow (help → need → location) + help map for rescue planning | **DONE, verified on Rajeev's machine (2 Nov 2026):** real Gradle build, real Telegram taps, `/responses` and the seeder pass via `test-help-flow.ps1` (21 of 23 checks; the 2 fails were Emergency tapped instead of Medicine, not a bug). Test alert id was 67 | **Map tab built, reported done by Rajeev (2 Nov 2026)**; Claude has not seen it run | **S1 DONE (per Rajeev's report, 2 Nov 2026)** |
| S2 | Danger-zone (radius) targeting, "spreading" demo | — | — | |
| S3a | Roles (AUTHORITY/REPORTER), reporter accounts, geotagged photo reports, EXIF trust flags, History, authority list/detail/photo | **DONE, verified on Rajeev's machine (3 Nov 2026):** real Gradle build, `test-s3a.ps1` 53 of 53 | Reporter shell + report form + authority Reports screens; works on the phone per Rajeev (3 Nov 2026) | **S3a DONE** (contract: section 14.14) |
| S3b | Chat: group per State + District, private thread with the authority, report cards, 3 s polling | **Reported done by Rajeev (3 Nov 2026); built outside the Claude session that wrote S3a, so that Claude has not seen the code** | Reported done and tested on the real phone by Rajeev (3 Nov 2026) | **S3b DONE (per Rajeev's report)**. Next backend session: attach a fresh backend zip so the chat code is visible |
| S4a | Analysis: `ReportAnalyzer`, async worker, `ReportAnalysis`, retry, analysis chip in the authority detail | **Reported done by Antigravity (3 Nov 2026), test gate output reported, not yet re-run by Rajeev:** `./gradlew test` 23/23, `test-s3a.ps1` 53/53, `test-s4a.ps1` fake 43/43 and none 10/10. **Deviation:** it also wrote a `gemini` provider (`GeminiReportAnalyzer`, model `gemini-2.5-flash`) that was not asked for and has **never run against the real API**; `AI_PROVIDER` stays `none`/`fake` and the key must come only from the `GEMINI_API_KEY` environment variable | Authority report detail has the AI analysis card + Retry (mock pack `s4a-mock/`) | **S4a DONE (backend reported 3 Nov 2026; app card built and working per Rajeev, 3 Nov 2026)** |
| S4b | Review queue, decisions, clarification (in the thread), draft, idempotent publish, Telegram hazard message, History shows the alert | **DONE (reported 4 Nov 2026), real output pasted:** `./gradlew test` 40 tests 0 failed (incl. 5 integration tests with a recording fake channel: zero sends before publish, one send per citizen, parallel publish = one publisher, one failing citizen, restart resume), `test-s3a` 53, `test-s4a` fake 43 / none 10, `test-s4b-a` 32, `test-s4b-b` 62, all 0 failed. **Real Telegram delivery not yet verified** (needs Rajeev's bot token and a registered account) | **NEXT:** authority decision buttons, draft editor, publish confirmation, delivery status; reporter History shows clarification and alert (mock pack `s4b-mock/`) | **S4b BACKEND DONE (pending Rajeev's real Telegram check); frontend NEXT** |
| S4c | Updates and restoration with audience picker, redeliver, duplicate linking | — | — | Spec in section 14; same as S4b |
| S5 | ~~Incident chat (per-report thread)~~ | — | — | **Superseded 2 Nov 2026:** chat is core and is built as S3b (section 14.5 H) |
| S6 | **QR scan relay**, built LAST (Rajeev, 3 Nov 2026): the app (both roles) decodes a QR from a **photo of a QR code** (picked image or camera), sends the text to the backend, which stores it; the app displays it. Deliberately small, not a full process | — | — | After S4c. Spec in section 15; not started |
| S7 | Hardening + demo polish | — | — | |
| S8 | ESP32 sensors → analysis → draft alert (the old S6, optional stretch, spec kept in section 10) | — | — | Not planned |

### What exists now

**Backend (all working on Rajeev's machine)**
- Authority login: `POST /api/auth/login`; tokens are in-memory UUIDs (restart ⇒ log in again). Officers come from env/config `AUTHORITY_USERS` (`id:pin:Display Name,…`). Demo defaults `asha.dc/1234`, `ravi.sdma/5678` (**change before any public demo**).
- `POST /api/alerts` validates against generated data (36 States/UTs, 368 districts, 12 disaster types, 4 severities), stores the alert (audit log), delivers to citizens registered in that **State + District**. ≤ 25 recipients are delivered before the response returns, larger sends continue in a background thread (`status` SENDING → COMPLETED).
- Telegram bot: `/start` → State → District → optional typed city/locality (or Skip); `/status`, `/stop`; alert messages carry **✅ I'm safe / 🆘 I need help**; replies saved in `alert_replies` (one per citizen per alert, last tap wins).
- **S1 (delivered, pending Rajeev's run):** **I need help** → need buttons (Food / Water, Medicine, Emergency / Rescue, Shelter, Other) → **📍 Send my location** (Telegram reply keyboard) or Skip. `GET /api/alerts/{id}/responses` feeds the map and graph; `POST /api/demo/seed-help` creates simulated points.
- Citizens are targeted by State + District. City/locality is free text and is only descriptive.

**Android (delivered, pending first confirmed run)**
- Splash → **Officer login** → Home → broadcast screen (State, District, city, severity, disaster type, instructions).
- Broadcast screen shows "N registered citizens will receive this alert" (`/api/targets`), sends via `POST /api/alerts`, shows delivered/failed counts. Session-expired → back to login.
- Network layer: `data/network/BackendApi.kt` (HttpURLConnection + org.json + coroutines), `Session` object. Cleartext HTTP only allowed for `127.0.0.1`, `localhost`, `10.0.2.2` (`res/xml/network_security_config.xml`).
- **S1 map (reported by Rajeev, 2 Nov 2026; not verified by Claude):** a "Map" destination under the bottom tabs showing the help requests of the newest alert (selector for the last 20 alerts), summary chips, needs graph, Leaflet map and a list sheet.

**S3a / S3b (3 Nov 2026)**
- S3a backend (verified by Claude with `test-s3a.ps1`): `POST /api/auth/login` now also returns `role, userId, state, district`; authority-only `POST/GET /api/users`; reporter demo account `anil.rep / 1111` (Karnataka, Udupi) from `REPORTER_USERS`; `POST /api/reports` (multipart: JPEG ≤ 10 MB + JSON `data`), `GET /api/reports/mine`, authority `GET /api/reports`, `GET /api/reports/{id}`, `GET /api/reports/{id}/photo`. Role gating is in `TokenFilter` by path. Photos are on disk under `./uploads/reports/`. Every error body is `{"error": "..."}`. Full contract: section 14.14.
- S3b chat: reported working on the real phone by Rajeev; Claude has not seen its code. Confirm package and endpoint names from the backend zip before changing it.

### Backend source map
`com.rajeev.disasteralert`
- `DisasterAlertApplication` (`@EnableScheduling`)
- `auth/` `AuthService`, `TokenFilter` (Bearer; open: `/api/auth/login`, `/api/health`; sets request attribute `officerName`)
- `location/LocationData` and `alert/DisasterCatalog` — **generated from the app's `DisasterData.kt`**; names must stay identical. If `DisasterData.kt` changes, regenerate them.
- `alert/` `Alert`, `AlertReply`, `Severity`, `DisasterType`, `AlertService`, repositories, **`Need`** (S1 enum: label, emoji, priority), **`HelpService`** (S1: `/responses` totals + sorting, demo seeder)
- `recipient/` `Recipient`, `RecipientRepository`
- `user/` (S3a) `AppUser`, `UserRepository`, `UserService`, `PinHasher`, `ReporterSeeder`; `auth/Role`; `auth/AuthService` now holds `Session(userId, displayName, role, dbId, state, district)`
- `report/` (S3a) `Report`, `ReportRepository`, `ReportService`, `ReportInput`, `ReportCategory`, `ReportStatus`, `TrustFlags`, `ExifReader` (+ `ExifSource`, `ExifInfo`), `PhotoStorage`, `ReportDtos`; `api/ReportController`, `api/UserController`
- `chat/` (S3b) reported built by Rajeev's other session; not seen by Claude
- `notify/` `NotificationChannel`, `TelegramNotificationChannel`, `TelegramClient`, `BotPoller`, `BotUpdateHandler`, `Json` (Map helpers), `DeliveryResult`
- `api/` `ApiController`, `AuthController`, `Dtos` (records), `ApiExceptionHandler`, **`DemoController`** (S1, guarded by `demo.enabled`)

## 5. Current API contract (all need `Authorization: Bearer <token>` except login and health)

```
POST /api/auth/login        {officerId, pin}                       -> {token, officerName}
GET  /api/health            (open)                                 -> {status, botConfigured}
GET  /api/locations                                                -> [{name, code, unionTerritory, zone, districts[]}]
GET  /api/disaster-types                                           -> [{id, name, emoji, category, description}]
GET  /api/severities                                               -> [{id: RED|ORANGE|YELLOW|GREEN, label, codeName, description}]
POST /api/alerts            {disasterType, state, district, city?, severity, instructions, authorityId?}
                                                                   -> {id, alertCode, status, scope, recipients, delivered, failed, createdAt}
GET  /api/alerts                                                   -> last 20 alerts
GET  /api/alerts/{id}                                              -> {alert, safe, help, replies:[{name, city, response, at}]}
GET  /api/targets?state=&district=                                 -> {state, district, citizens}
GET  /api/recipients                                               -> [{id, name, state, district, city}]
GET  /api/coverage                                                 -> [{state, district, citizens}]
GET  /api/alerts/{id}/responses?type=ALL|HELP|SAFE                 -> S1 help map, full shape in section 10 (S1 → Contract)
POST /api/demo/seed-help    {alertId,count,centerLat,centerLon,radiusKm} -> {created}   (SIMULATED data, demo.enabled)
DELETE /api/demo/seed-help/{alertId}                               -> {removed}         (simulated rows only)
```
Errors: `400 {"error": "..."}` (unknown state/district/type/severity), `401` without a valid token. Timestamps are ISO-8601 UTC; show IST in the UI. `disasterType` accepts id or name; `state` accepts name or 2-letter code.
**Implemented since this list was written:** S3a (users, reports, photos; section 14.14) and S3b chat (reported by Rajeev). **Planned, NOT implemented:** the review, hazard-alert and update endpoints (section 14.5 C-G, I) and the QR scan endpoints (section 15).

## 6. Conventions and gotchas (learned the hard way)

**Backend**
- **Spring Boot 4 uses Jackson 3** (`tools.jackson.*`). Do **not** import `com.fasterxml.jackson.*`. Telegram JSON is handled as `Map<String,Object>` through the `Json` helper.
- Use explicit names: `@PathVariable("id")`, `@RequestParam("x")`.
- H2 with `ddl-auto=update`: **every new column must be nullable or have a default**, otherwise startup fails on existing rows. When a schema change is unavoidable, bump the DB file name (`disasteralert2` → `disasteralert3`) and tell Rajeev to re-register in the bot.
- Telegram `callback_data` ≤ 64 bytes → use codes/indexes, never long names. Only **one** backend instance per bot token (else HTTP 409).
- Secrets only from env vars: `TELEGRAM_BOT_TOKEN`, `AUTHORITY_USERS`, future `AI_API_KEY`. Never print or commit them. (A bot token was exposed in an earlier chat. Confirm it was rotated with BotFather `/revoke`.)
- Windows PowerShell 5 prints UTF-8 emojis as garbage — display quirk only.
- The Claude sandbox cannot run Gradle/Maven/Android SDK. Say clearly what was only syntax-checked. Rajeev's machine is the real test.

**Android**
- Dev base URL `http://127.0.0.1:8080` works only with `adb reverse tcp:8080 tcp:8080` (re-run after every re-plug). ESP32 and any phone-over-Wi-Fi setup need the laptop's LAN IP and a firewall rule for port 8080 instead.
- Source of truth for States/Districts/Disaster types: `data/DisasterData.kt`.
- Keep `HomeScreen.kt` structure; theme colors live in `ui/theme/Color.kt` (`EmergencyRed`, `EmergencyOrange`, …).

## 7. How Claude should work with Rajeev

- Guide **step by step, slowly**: give one step, wait for the result, then the next. Rajeev is on Windows/PowerShell.
- Deliver **complete files** (not snippets), zipped with the exact folder structure, plus the exact destination paths. Warn about which files are replaced and which are new. Mistakes so far came from files landing in the wrong folder (root vs `app/` build file).
- Ask Rajeev to paste terminal output **with tokens removed**.
- Be honest about uncertainty and verify facts that can change (prices, policies, library versions) by searching.
- Correct wrong assumptions politely. Rajeev asked to be corrected.

## 8. Working protocol: vertical slices, contract first

Never build the whole backend and then the whole frontend. For every slice:

1. **Claude — contract (≈15 min):** write the endpoint JSON (examples) into this file under the slice, plus a sample JSON file Antigravity can mock.
2. **Antigravity — UI on mock data:** builds the screens against a fake repository, runs on the phone.
3. **Claude — backend + bot + test script:** implements and verifies the contract; Rajeev restarts the backend and runs the script.
4. **Antigravity — swap mock for real calls.**
5. **Rajeev — acceptance test** (listed per slice), commit to Git, mark the slice DONE in the Status Board. Claude outputs the updated `PROJECT_HANDOFF.md`.

Contract changes only through section 9. A slice is not done until its acceptance test passes on the real phone.

### 8.1 Test gate (added 3 Nov 2026): tests before the phone

Before Rajeev installs anything on the phone, the side that was just built must pass tests that **were written from the contract in this file, not from the code just written**:
- **Backend:** a PowerShell script `test-<slice>.ps1` (same style as `test-s3a.ps1`: PASS/FAIL lines and a final count) plus JUnit tests for pure logic (`./gradlew test`) where there is any. The script is run against the running backend.
- **Android:** unit tests on the sample JSON files and role/routing/error logic (`./gradlew :app:testDebugUnitTest`) and a clean `./gradlew :app:assembleDebug`.
- The builder pastes the **real output** (tests run, passed, failed, skipped and why). Rajeev reads it. Tests may not be weakened or deleted to pass; a test that disagrees with the contract is reported, not "fixed".
- Camera, GPS, QR decoding and the real network cannot be unit tested, so the **phone acceptance test stays mandatory**.

### 8.2 Antigravity builds both sides (from S4, decided 3 Nov 2026)

For S4a, S4b, S4c and S6, Rajeev has Antigravity build the backend as well as the app, with Claude as instructor:
1. For each checkpoint Claude decides **backend first or frontend first** and gives Rajeev **one prompt for one side**. Never both sides in one prompt. The reason is written next to the decision (usually: if the risk is in server logic such as publishing or idempotency, backend first; if the screens are the unknown, frontend first on mock JSON).
2. The side is built, then the 8.1 test gate is passed and the output shown to Claude. Only then does Claude write the prompt for the other side.
3. Antigravity must have the right project open: the backend folder `C:\Users\DELL\Desktop\Rajeev\disasteralert-backend` for backend prompts, the Android project for app prompts. `PROJECT_HANDOFF.md` and the slice's sample JSON must sit in that folder's root.
4. Backend rules for Antigravity are in section 13.1; the contract in this file still rules and changes only through section 9.

## 9. Contract change requests (append here)

**S1 clarifications (nothing breaking; the frozen S1 JSON shape is unchanged).**
1. `totals` always cover the whole alert. `type=HELP|SAFE` only filters `items`, so the chips and the needs graph do not jump when the list is filtered.
2. A HELP with no need chosen yet counts as `UNSPECIFIED` in `byNeed`, has `need: null` and `priority: "MEDIUM"`. SAFE rows have `need: null` and `priority: null`.
3. `accuracyM` may be `null`: Telegram only sends `horizontal_accuracy` when the client has it, so null does not prove a hand-picked pin (Rajeev's real 📍 tap on 2 Nov arrived without it). Show "accuracy unknown" and no accuracy circle.
4. Added (additive): `DELETE /api/demo/seed-help/{alertId}` removes the simulated citizens of one alert. Demo endpoints answer 404 when `DEMO_ENABLED=false`; the default is **true** for the hackathon.
5. Errors: `type` other than ALL/HELP/SAFE → 400; unknown alert → 404; no token → 401.

**S3/S4 scope change (requested by Rajeev, 2 Nov 2026).** Reporter-generated hazard alerts with automated analysis, authority approval, Telegram distribution, a Reporter Group Chat, History updates and restoration updates are specified in section 14. This changes the earlier S3/S4 text as follows (nothing is built yet, so no existing contract breaks):
1. Report statuses become `SUBMITTED, PENDING_REVIEW, NEEDS_CLARIFICATION, VERIFIED, REJECTED, ALERT_PUBLISHED, RESOLVED` (was `NEW, VERIFIED, REJECTED, RESOLVED`).
2. AI severity uses RED/ORANGE/YELLOW/GREEN (was a 1-5 number).
3. Decision 7 (no general group chat) is revised: a Reporter Group Chat per State + District is added (section 11).
4. `NotificationChannel` will need a plain-text send for updates (the current `send(Recipient, Alert)` only handles `Alert`).
5. Only two app roles exist: AUTHORITY and REPORTER. The DEPARTMENT role that appeared in the early S3 text is removed everywhere.

**Chat design (requested by Rajeev, 2 Nov 2026; supersedes items 3 above and the group-chat parts of section 14 as first written).** Nothing is built yet, so no existing contract breaks.
1. The reporter app has **three tabs**: (1) **Group chat**, (2) **Chat with authority** (also where a report is submitted), (3) **History**.
2. **Group** = one per State + District. A submitted report appears there as a **card showing only category, area and status**, so other reporters see it is already reported. The card status follows the report: Under review → Verified → Alert published → Resolved. **Reporters can post text in the group** (decision 7 revised again).
3. **Private thread** = one per reporter with the authority. The submission arrives there as a card with photo, category and details. The authority's questions, updates and restoration notes arrive there.
4. Photo, exact GPS and reporter name never appear in the group. Nothing typed in the group or the private chat ever reaches citizens; citizens get only what an authority explicitly approves and publishes.
5. For every update or restoration the authority **picks the audience**: citizens on Telegram, the group, the reporter personally (also anyone whose duplicate report was linked). Updates can exist before any public alert (e.g. "crew dispatched", reporters only). So updates now attach to the **report**, not only to a published alert: `POST /api/reports/{id}/updates` replaces `POST /api/hazard-alerts/{id}/updates`.
6. `POST /api/reports/{id}/clarification-response` is removed: the question and the answer are messages in the private thread. `POST /api/reports/{id}/link` takes `primaryReportId` instead of `hazardAlertId`.
7. The group feed is no longer a system-only array; poll responses are `{messages, cards}` (14.5 H). Chat is polled every 3 s.
8. The old per-report incident chat (S5) is dropped; the private thread covers it. New slice **S3b** = chat (14.11).

---

**S4a behaviour change (3 Nov 2026; this was already announced in 14.14, nothing frozen is broken).**
1. `POST /api/reports` now answers `"analysisStatus":"QUEUED"` (was `NOT_STARTED`) and the call still never waits for the AI.
2. After analysis finishes, fails or is skipped, the report moves `SUBMITTED` → `PENDING_REVIEW` (asynchronously, normally within seconds; immediately when no AI is configured). The reporter's History and the authority list therefore show `PENDING_REVIEW` shortly after submit. `GET /api/reports/{id}` fills `analysis` (14.5 C). `test-s3a.ps1` must be updated for these two facts; the Android app already renders every status of 14.2.
3. New: `POST /api/reports/{id}/analysis/retry` (authority only, 14.5 C).
4. `analysis` in `GET /api/reports/{id}` is an object for every report submitted from S4a on: `{"status":"QUEUED|RUNNING|COMPLETED|FAILED|SKIPPED"}` plus the 14.5 C fields when `COMPLETED`, or `"error"` when `FAILED`/`SKIPPED`. It stays `null` only for reports that existed before S4a.
5. No AI provider is chosen yet (14.12 question 1). S4a ships with `AI_PROVIDER=fake` (clearly labelled "simulated", also the test hook) and `SKIPPED` when no provider is configured; the real adapter is a later step.

**S4b clarifications (3 Nov 2026; shapes the contract left open, nothing frozen changes).**
1. Decisions allowed in S4b: `VERIFY`, `REJECT`, `REQUEST_CLARIFICATION` (only from `PENDING_REVIEW`) and `RESOLVE` (only from `VERIFIED`, no public alert needed). Anything else is 409. Every decision is an append-only `ReportDecision` row.
2. `GET /api/reports/{id}` fills: `decisions[] = [{id, decision, note, decidedBy, decidedAt}]`, `clarifications[] = [{id, question, askedBy, askedAt, answer, answeredAt}]` and `hazardAlert = {id, alertCode, status, draftVersion, hazardType, severity, state, district, city, title, message, instructions, validUntil, audienceEstimate, dispatchStatus, createdBy, approvedBy, publishedAt, resolvedAt, delivery:{sent, failed, pending}}` (null until a draft exists). `possibleDuplicates` stays `[]` until S4c.
3. `instructions` of a hazard alert is **one string**; each line becomes a bullet in the Telegram message (14.6). Limits: `hazardType` 1-60, `title` 1-120, `message` 1-1000, `instructions` up to 1000, `severity` RED|ORANGE|YELLOW|GREEN.
4. `GET /api/hazard-alerts/{id}` (authority) returns the same `hazardAlert` shape. `redeliver` and the `updates` list arrive with S4c.
5. Reporter History (`GET /api/reports/mine`): `alert` is filled once the alert is `PUBLISHED` or `RESOLVED` (never for a draft) with `{id, alertCode, severity, publishedAt, resolvedAt}`; `clarification` is `{question, askedAt}` while the report is `NEEDS_CLARIFICATION`, otherwise null.
6. `NotificationChannel` gains a plain-text send (sent without any Markdown/HTML parse mode). A **dry-run** switch (`NOTIFY_DRY_RUN=true`, default false) marks deliveries `SENT` without calling Telegram, for tests only. A demo endpoint that creates simulated citizens for tests answers 404 unless `demo.enabled` AND dry-run are both on, so it can never pollute a real run.
7. **Accepted deviation:** `RESOLVE` is also allowed from `ALERT_PUBLISHED` (14.2 "manual close"): the report and the alert become `RESOLVED`, `resolvedAt` is set and **nothing is sent to citizens**. The app must warn "Citizens will not be told it is over"; real restoration messages are S4c.
8. Publishing is serialised with a database row lock (`SELECT ... FOR UPDATE`) instead of a conditional UPDATE; guarantees are the same (one publisher, second call `alreadyPublished:true`). Sending runs on a background executor after commit; pending rows are sent again on start-up.
9. Test-only endpoints `POST/DELETE /api/demo/seed-citizens` exist and answer 404 unless `demo.enabled` AND `NOTIFY_DRY_RUN` are true. `demo.enabled` still defaults to true.

## 10. Roadmap

Rough effort for one focused person per side. **Cut line for a hackathon: S1 + S3 + S6 (QR relay, section 15) + a manual version of S4 are the core. S2 is the best "spreading disaster" demo. Chat is now core (S3b), not optional; the old per-report S5 chat is dropped.**

### S1 — Three-tap help flow + help map for rescue planning (3-hour sprint) — DONE (see Status Board)

**Why:** this is the heart of the pitch: two-way response, then a map rescuers can plan from.

#### The citizen flow (decided by Rajeev): three taps

```
Alert message (existing)      [✅ I'm safe]  [🆘 I need help]

TAP 1  🆘 I need help   -> reply saved as HELP immediately (counts even if the citizen stops here)
                           bot sends a NEW message "What do you need most?"
                           [🍞 Food / Water] [💊 Medicine] [🚑 Emergency / Rescue] [🏠 Shelter] [✍️ Other]

TAP 2  one need         -> need saved; that message is edited to "Need recorded: 💊 Medicine"
                           bot sends a location request with a reply keyboard:
                           [📍 Send my location]   [Skip]
                           ("Other" first asks for a short typed note, then the same location request)

TAP 3  📍 Send my location -> Telegram sends message.location -> lat/lon/accuracy saved
                           bot confirms "✅ Help request recorded. Rescuers can see your position." and removes the keyboard
```

- Telegram never reveals location without this explicit tap. The first time, Telegram may show a confirmation dialog. The button works in Telegram mobile apps, not guaranteed on desktop.
- Callback data (≤ 64 bytes): existing `r:<alertId>:HELP`, new `n:<alertId>:<NEED>` with NEED in `FOOD_WATER | MEDICAL | EMERGENCY | SHELTER | OTHER`.
- Priority (server-side): `EMERGENCY=HIGH, MEDICAL=HIGH, FOOD_WATER=MEDIUM, SHELTER=MEDIUM, OTHER=LOW`.
- Edge cases: tapping HELP again does not duplicate (one row per citizen per alert, last tap wins). Tapping **I'm safe** later sets SAFE, clears the need, keeps any location (green marker). **Skip** = help counted, no marker. A location message with no pending flow attaches to that chat's most recent HELP reply. Pending state is in memory; after a server restart the fallback above applies.

#### Delivered backend notes (S1)
- Files: new `alert/Need`, `alert/HelpService`, `api/DemoController`, `test-help-flow.ps1`; replaced `alert/AlertReply`, `alert/AlertReplyRepository`, `api/ApiController`, `api/Dtos`, `notify/BotUpdateHandler`, `notify/TelegramClient`, `notify/Json`, `application.properties`, `README.md`. No Gradle change. No DB reset needed (new columns are nullable).
- Verified in the sandbox: all sources compiled against hand-written stubs of the Spring/Jakarta APIs, and 42 checks drove the real `BotUpdateHandler` and `HelpService` with in-memory repositories (three taps, Other + note, Skip, restart fallback, safe-after-help, forwarded/invalid/unknown inputs, `/responses` totals and sorting, seeder). Afterwards verified on Rajeev's machine (2 Nov 2026): real Gradle build and start-up, real H2 queries, `test-help-flow.ps1` and the real Telegram taps with a shared location.
- Extra rule: a forwarded location message is rejected (it is somebody else's position).

#### Backend (Claude)
- `Need` enum (label, emoji, priority). `AlertReply` gets **nullable** columns `need`, `note`, `latitude`, `longitude`, `accuracyM`, `locationAt` (`ddl-auto=update` adds them, no DB reset needed).
- `BotUpdateHandler`: the flow above. `TelegramClient`: add reply-keyboard support (`request_location`) and `remove_keyboard`.
- Demo helper (guarded by `demo.enabled=true`, authority-only, clearly labelled): `POST /api/demo/seed-help {alertId, count, centerLat, centerLon, radiusKm}` inserts simulated help requests ("Demo citizen N", negative chat ids) so the map can be tested and demoed without 20 phones. Say "simulated" when presenting.
- Test script `test-help-flow.ps1`.

#### Contract

```
GET /api/alerts/{id}/responses?type=ALL|HELP|SAFE        (default ALL; needs Bearer token)
-> {
  "alertId": 12, "alertCode": "ALERT-00012",
  "totals": {
    "safe": 3, "help": 5, "helpWithLocation": 4,
    "byNeed": {"EMERGENCY":1,"MEDICAL":1,"FOOD_WATER":2,"SHELTER":0,"OTHER":0,"UNSPECIFIED":1}
  },
  "items": [
    {"replyId":7, "name":"Asha", "response":"HELP", "need":"MEDICAL", "priority":"HIGH",
     "city":"Kollur", "latitude":13.8626, "longitude":74.8147, "accuracyM":35.0,
     "note":null, "at":"2026-11-02T05:41:12Z", "locationAt":"2026-11-02T05:41:40Z"}
  ]
}
```
`items` are sorted HELP first, then priority, then newest. `latitude/longitude` are null when no location was shared. `need` is null for SAFE. Timestamps are UTC ISO-8601 (show IST).

```
POST /api/demo/seed-help   {"alertId":12,"count":15,"centerLat":13.86,"centerLon":74.81,"radiusKm":8}
-> {"created":15}
```

#### Frontend (Antigravity)
- New `ResponseMapScreen(alertId)`, opened from the broadcast success screen ("VIEW RESPONSE MAP") and later from the alert list.
- Top: summary chips (🆘 Help N · ✅ Safe N · 📍 With location N) and a **needs graph**: horizontal bars for `byNeed` (this is the "graph"). Tapping a bar filters map and list.
- Middle (~55 % height): the map (spec below). Bottom: list sorted like the API; tapping a row centres the map; **Navigate** opens the maps app (`geo:` intent).
- Auto-refresh every 5 s while the screen is visible; states: loading, error + Retry, empty ("No help requests yet").
- Build it first on mock JSON (the contract above), then swap to `BackendApi.responses(alertId)`.
- **Layout as decided later (Rajeev, 2 Nov 2026):** the map is NOT a screen opened from a button. It is a full-screen "Map" tab under the bottom navigation. It loads `GET /api/alerts`, selects the newest alert by default (selector for the last 20), shows the chips and needs graph as a collapsible overlay on the map and the list in a bottom sheet. Everything else above (data, tile rules, markers, 5 s refresh) is unchanged. This note replaces the `ResponseMapScreen(alertId)` layout wording.

#### Map technology (decided): **Leaflet 1.9.4 inside an Android `WebView` + OpenStreetMap tiles**
- Why: no Gradle dependency, no API key or billing, nothing to sync, rich overlays (circles, clustering, legend). Rejected: **osmdroid** (its repository was archived read-only in Nov 2024), **Google Maps SDK** (key + billing), **MapLibre** (needs a vector-tile provider).
- **Tile rules (else OSM shows "blocked" tiles):** (1) set a unique WebView user agent, e.g. `webView.settings.userAgentString = "DisasterAlertHackathon/1.0"`; (2) load the page with `loadDataWithBaseURL("https://disasteralert.app/", html, "text/html", "UTF-8", null)` so an https Referer is sent (do **not** load a bare `file://` page); (3) include `<meta name="referrer" content="strict-origin-when-cross-origin">`; (4) show the attribution "© OpenStreetMap contributors"; (5) keep the tile URL in one constant (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`) so it can be switched to a keyed provider (MapTiler, Stadia, …) if OSM blocks. Demo-scale use only: heavy use of the public tiles is forbidden.
- Leaflet from a pinned CDN (`https://unpkg.com/leaflet@1.9.4/dist/leaflet.css` and `.js`). The phone needs internet (Wi-Fi or mobile data) for tiles and the CDN; the list view still works without it.
- Map page spec (single HTML string, JS functions called from Kotlin with `evaluateJavascript`):
  - `window.setData(payload)`: redraws markers from `payload.items` that have coordinates (reuse markers by `replyId`); fit bounds on the first load only; default view India (centre `[22.5, 79]`, zoom 5).
  - `window.focusOn(replyId)`: pan/zoom to that marker and open its popup.
  - Marker colours: SAFE green · EMERGENCY dark red · MEDICAL red · FOOD_WATER orange · SHELTER blue · OTHER grey; a faint circle for `accuracyM`; popup = name, need label, note, time (IST), a **Navigate** link that calls `Android.onNavigate(lat, lon, name)` (`addJavascriptInterface`).
  - Legend in a corner; marker clustering optional (`leaflet.markercluster@1.5.3`) if more than ~50 points.

#### Sprint plan (≈ 3 h, backend and frontend in parallel)
| Time | Claude (backend) | Antigravity (frontend) | Rajeev |
|---|---|---|---|
| 0:00–0:15 | Contract frozen (this file) | Gets the kickoff prompt + sample JSON | Starts the Antigravity session |
| 0:15–1:15 | Three-tap bot, new columns, `/responses`, seed endpoint, test script | `ResponseMapScreen` on mock data | Waits; checks the app builds |
| 1:15–1:40 | — | — | Restarts backend, runs the 3-tap flow in Telegram, runs `test-help-flow.ps1` |
| 1:40–2:20 | Fixes from testing | Swaps mock for real `/responses`, tests on the phone | Tests with the seed endpoint, then with a real tap + location |
| 2:20–3:00 | Updates this file's Status Board | Polish, empty/error states | Full demo run + 60 s backup video |

Risk points to watch: Gradle sync issues, blank/blocked map tiles (see tile rules), the phone having no internet, Telegram not offering the location button on desktop.

#### Acceptance test
Send an alert → in Telegram tap **I need help**, then **Medicine**, then **Send my location** → within ~5 s a red marker appears at the right place on the app map, the "Medicine" bar increases, and the row shows Navigate. Then seed 15 demo points around that location and confirm the map and the graph still look right.

### S2 — Danger-zone (radius) targeting: the "spreading disaster" demo (≈ 3–4 h)
*Backend:* during registration the bot optionally asks to share location (`Recipient.latitude/longitude/locationAt`, nullable). `POST /api/alerts` gets optional `zone {latitude, longitude, radiusKm}`; targets = registered citizens with a known location within the radius (Haversine), inside the chosen State/District. `GET /api/targets` accepts the same zone and returns `{citizens, withLocation, inRadius}`. Decide and document whether citizens without a stored location are included (`includeUnlocated` flag, default false).
*Frontend:* optional "Danger zone" toggle on the broadcast screen → tap the map to place a centre, slider for radius, circle overlay, live "N citizens in zone" count, registered citizens as dots.
*Demo:* alert 1 with 2 km radius, then alert 2 with 5 km ("it is spreading"); only people inside each ring are notified.
*Acceptance:* two teammates at different positions; each ring notifies only the right one.

### S3 — Roles and geotagged field reports (≈ 6–8 h)
> **Detailed workflow, statuses, contracts, failure handling and acceptance criteria: section 14. Where section 14 differs from the text below, section 14 wins.**
*Backend*
- `User` entity with `role` AUTHORITY | REPORTER (only two roles), assigned district. Keep config officers as AUTHORITY. `POST /api/users` (authority-only) creates reporter accounts. Login returns `role`; add role checks in `TokenFilter`/controllers.
- `POST /api/reports` as `multipart/form-data`: part `photo` (JPEG) + part `data` JSON `{category, note, state, district, latitude, longitude, accuracyM, capturedAt, mockLocation}`.
  Categories: `ROAD_BLOCKED, ROAD_CLEARED, FLOODED_AREA, LANDSLIDE, FOOD_NEEDED, WATER_NEEDED, MEDICAL_NEEDED, SHELTER, POWER_OUTAGE, OTHER`. Status: `NEW, VERIFIED, REJECTED, RESOLVED`.
  **Food/medical needs are not visible in a photo.** They come from the category the reporter selects plus the note. The photo is evidence. Reporters can file any category (road blocked, food, water, medicine/medical help, shelter, …), each with its own map icon.
- Store the file under `./uploads/reports/{id}.jpg` (serve through an authenticated endpoint only). Read EXIF GPS + timestamp (e.g. `com.drewnoakes:metadata-extractor` or Apache Commons Imaging; needs a Gradle dependency) and compute **trust flags**: `EXIF_MISSING`, `GPS_MISMATCH` (EXIF vs submitted > ~200 m), `OLD_PHOTO` (EXIF time vs upload), `MOCK_LOCATION`. **EXIF can be stripped or forged, so never trust it alone.** This is the answer to the hackathon's "conflicting information" problem.
- Endpoints: `GET /api/reports?district=&category=&status=`, `GET /api/reports/{id}`, `GET /api/reports/{id}/photo`, `PATCH /api/reports/{id}/status`.

*Frontend:* role-aware home. **Reporter:** CameraX capture screen, device GPS at capture time, write location into the JPEG EXIF (`ImageCapture.Metadata.location` / `ExifInterface.setGpsInfo`), preview, category chips, note, upload multipart, "my reports" list. **Authority:** reports list + map layer (icon per category), report detail with photo and trust flags. New dependencies (CameraX, exifinterface, location) go through the version catalog.
*Acceptance:* a reporter logs in on the phone, photographs something, submits; the authority account sees the pin on the map with the photo and a trust badge.

### S4 — Review queue, AI vision assist, situation-update broadcast (≈ 4–6 h)
> **Detailed workflow, statuses, contracts, failure handling and acceptance criteria: section 14. Where section 14 differs from the text below, section 14 wins.**
*Backend*
- AI assist (server side; key in `AI_API_KEY`; Claude or Gemini vision API): given photo + category + note + location, return JSON `{sceneSummary, suggestedCategory, severity 1-5, consistentWithCategory, caveats[], draftMessage}` stored on the report. **Advisory only.**
- `POST /api/reports/{id}/publish-update` (authority only): `{message, radiusKm? | district, severity}` → creates a `SituationUpdate` and sends it to nearby/district citizens through `NotificationChannel`. Example: "🚧 ROAD BLOCKED near Kollur (landslide). Use alternate route via …". **A human always approves before citizens receive anything.** Auto-sending AI output to the public risks false alarms.
*Frontend:* review queue (photo, AI chip, trust flags) with Verify / Reject / Publish-update (editable draft message, citizens-reached preview).
*Acceptance:* submit a "road blocked" report → authority verifies → edits and publishes → a registered Telegram user gets the update.

### S5 — Incident chat — SUPERSEDED (2 Nov 2026)
Replaced by the Group chat and the private reporter-authority thread, built as slice **S3b** and specified in section 14.5 H. Do not build a separate per-report chat.

### S6 — QR scan relay (≈ 3–4 h) — replaces the old ESP32 S6
Full spec: **section 15**. In one line: someone's phone shows a QR code, the app (reporter or authority) scans it, sends the decoded text to the backend, which validates and stores it; the app shows the parsed data. No QR generation in the app, no signing, no automatic alerts. The old ESP32 plan is kept below as optional S8.

### S8 — ESP32 sensors → record → analyse → alert (optional stretch, was S6; ≈ 5–7 h)
*Backend:* `Device(deviceId, name, type, state, district, latitude, longitude, apiKeyHash, thresholds, lastSeen)`, `SensorReading(deviceId, type, value, recordedAt)`.
`POST /api/sensors/readings` with header `X-Device-Key`: `{deviceId, readings:[{type:"water_level_cm", value:183.2}], battery?}`. Analysis service: moving average, rate of change, thresholds → `SensorEvent` → creates a **DRAFT alert** (pre-filled type, severity, location, text) that an authority confirms with one tap (`POST /api/alerts/drafts/{id}/confirm` reusing the existing send path). `GET /api/sensors` (last value, ONLINE/STALE) and `GET /api/sensors/{id}/readings?minutes=60`.
*Firmware (Claude writes the sketch):* Arduino `WiFi` + `HTTPClient`, POST JSON every 5 s. Easy demo sensors: HC-SR04 (water level), rain sensor/potentiometer, MPU6050 (vibration).
*Network:* the ESP32 posts to the **laptop's LAN IP** (same Wi-Fi/hotspot, firewall rule for 8080). `adb reverse` does not help it.
*Stretch ("scan the ESP32"):* the ESP32 advertises a BLE GATT characteristic; the Android app scans, reads and forwards to the backend when it has connectivity (store-and-forward). That directly addresses "communication networks unreliable".
*Frontend:* sensors screen (live values, simple line chart, ONLINE/STALE chip), device map layer, "Draft alert" banner → confirm.
*Acceptance:* raise the water level / turn the potentiometer past the threshold → draft alert appears → authority confirms → Telegram alert.

### S7 — Hardening and demo polish (≈ 3 h)
Overview endpoint (`GET /api/overview`: alerts, citizens, help requests, reports by status, devices online) and a dashboard card; token expiry + sign-out button; guarded demo-seed endpoint; rate limit on login; clear empty/error states; README and pitch script; **record a 60-second backup video**; use a mobile hotspot as backup network.

## 11. Decisions already made (do not re-litigate)

1. Telegram is the prototype channel. SMS (DLT) / WhatsApp / Cell Broadcast = production path through `NotificationChannel`.
2. Citizens never install anything.
3. Targeting is by State + District today; radius targeting arrives in S2. City is free text.
4. AI output is advisory. A human approves every public message.
5. Location is shared only by explicit tap (Telegram consent).
6. Reports are verified by comparing device GPS, EXIF and time. EXIF alone is not trusted.
7. **Revised again 2 Nov 2026 (Rajeev):** chat is core. Each State + District has a **Reporter Group Chat** where reporters **can post**; submitted reports appear in it as cards (category, area, status only). Each reporter also has a **private thread with the authority**. Nothing typed in either chat ever reaches citizens (section 14.5 H). Per-report incident threads (S5) are dropped. *(Original wording: "Chat = per-report threads, not a general group chat.")*
8. One backend instance, H2 file DB for the demo (Postgres is a production note).
9. The citizen help flow is exactly three taps: help → need → location (section 10, S1).
10. Maps: Leaflet in a WebView with OpenStreetMap tiles, configured per the tile rules in S1 (osmdroid is archived, so it is not used).
11. Two separate flows: **citizens** (Telegram, no app, no photo, three taps ending with a shared location, S1) and **field reporters** (Android app, camera photo + GPS at capture + EXIF checks + trust flags, categories such as road blocked/cleared, food, water, medicine, shelter, power outage, S3). Photos belong only to the reporter flow, so S1 stays inside its 3 hours.
12. Only two app roles: **AUTHORITY and REPORTER**. No DEPARTMENT role.
13. Reporter-generated hazard alerts: AI analysis runs automatically after submission but is advisory only; a public alert exists only after explicit authority approval; publishing is idempotent (section 14).
14. **Reporter app = three tabs** (Rajeev, 2 Nov 2026): Group chat · Chat with authority (also report submission) · History. Group cards show only category, area, status. The authority chooses the audience (citizens on Telegram, group, reporter personally) for each update or restoration; citizens only ever get authority-approved text.
15. **S6 is a small QR scan relay** (Rajeev, 3 Nov 2026), built last; the demo scans a **photo of a QR code** (a picked image or a camera shot of another phone's screen or a print), then sends it to the backend, stores it and displays it, for both roles. It is not a full process: no QR generation in the app, no payload signing, no pairing, and a scan never creates an alert, a report or a Telegram message by itself (section 15). ESP32 sensors moved to optional S8.
16. **Antigravity builds S4a-S4c and S6 on both sides, one side per prompt, with Claude as instructor** (Rajeev, 3 Nov 2026; section 8.2), and every side passes the test gate (section 8.1) before the phone.

## 12. Production notes (say these in the pitch, do not build)

SMS via DLT-registered templates; WhatsApp Business templates; Cell Broadcast / SACHET (CAP) integration through the authorities; HTTPS and a real identity provider; token expiry and audit export; data retention and privacy for photos and locations; offline-first/store-and-forward for field apps; LoRa/mesh for sensors; a contracted map-tile provider instead of the public OSM tile servers (their policy forbids heavy use and can block without notice); map boundaries that follow the official Survey of India boundary instead of OSM's own boundary lines.

**Two-way SMS (checked 2 Nov 2026, from one provider's India guide and news pages, so confirm with the chosen provider before promising it):** outbound SMS needs DLT registration (entity, sender header, approved templates); that guide quotes about ₹5,900 for the entity, 5-10 business days if nothing is rejected and 2-4 weeks with corrections, and lists two-way SMS as supported but does not explain how inbound replies work in India (number or shortcode, cost, approval). Cell Broadcast needs no registration or phone number but the sources reviewed do not describe any reply path, so we do not claim one. Our prototype's two-way loop runs on Telegram, which needs internet; SMS would also work on networks without data. An SMS channel would need an inbound-message webhook that maps replies to the same `AlertReply` rows, which is not built.

## 13. Kickoff prompt for Antigravity (copy, then add the slice)

> You are building the **Android frontend** (Kotlin, Jetpack Compose, package `com.rustbeatz.disasteralert`) of the DisasterAlert hackathon project. Read `PROJECT_HANDOFF.md` fully first. The backend is owned by another agent; never change its API. Work on **Slice `Sx` only**: build the screens against a mock repository using the JSON contract in section 10, then wait for the real backend before wiring `BackendApi.kt`. Reuse existing theme colors, `SafeAreaContainer` and components. Do not add dependencies without listing them and why. Deliver complete files, list new vs replaced files with exact paths, and keep the existing Splash → Login → Home flow working. Map: Leaflet 1.9.4 inside a WebView with OpenStreetMap tiles, exactly as specified in S1 (user agent, https base URL, attribution). No new Gradle dependencies and no API keys unless you list them first.

### 13.1 Kickoff prompt for Antigravity when it builds the BACKEND (copy, then add the slice and the step)

> You are building the **backend** (Spring Boot 4.1.2, Java 17, Gradle Kotlin DSL, Hibernate 7, H2) of the DisasterAlert hackathon project. The open folder is the backend project. Read `PROJECT_HANDOFF.md` fully first: sections 3, 5, 6, 8, 9, 11 and 14, and the slice named below. The API contract in that file is the source of truth: do not change request/response shapes, names or status codes; if you think something must change, stop and tell me instead. Obey section 6: Jackson 3 (`tools.jackson.*`, never `com.fasterxml.jackson.*`), explicit `@PathVariable("id")` / `@RequestParam("x")` names, every new column nullable or defaulted (`ddl-auto=update`, keep the DB file `disasteralert2`), secrets only from environment variables and never printed, `analysis/` and `chat/` code must not import `notify/`, nothing typed in a chat or produced by AI may reach Telegram or citizens without an explicit authority action. Keep `TokenFilter` role gating (reporters may only call the paths listed there; add new reporter paths only if the contract says so). Every error body is `{"error": "..."}`. Follow the test gate in section 8.1: write `test-<slice>.ps1` and any JUnit tests **from the contract**, run `./gradlew test` and the script against the running backend yourself, paste the real output, and never weaken a test to make it pass. Deliver: list of new and changed files with paths, how to start the backend, what you could not verify. Do not touch the Android project.

## 14. Reporter-Generated Hazard Alerts: automated analysis and authority approval (S3 + S4, detailed spec)

> **STATUS (3 Nov 2026): S3a is implemented and verified (14.14); S3b chat is reported done by Rajeev. Everything else in this section (AI analysis, review, publish, updates, duplicates) is SPEC ONLY: not implemented or verified.** It extends S3 (reports) and S4 (review, AI, publishing). Where it differs from the S3/S4 text in section 10, **this section wins**. Written 2 Nov 2026 from Rajeev's requirements; **revised the same day for the chat design (three-tab app, group cards, private thread, per-update audience)**; effort figures are Claude's rough estimates.

### 14.1 The workflow in one picture

```
Reporter (Android)            Backend                                      Authority (Android)             Citizens / others
 1 submit: photo, category,   store report + photo + trust flags, SUBMITTED
   note, GPS, accuracy        -> private thread: REPORT CARD (photo, details)
                              -> group: CARD (category, area, status only) ------------------------>  other reporters see it is already reported
                              2 AI analysis (async, advisory, failure never blocks)
                              PENDING_REVIEW  ------------------------->  review queue + reporter inbox
                                                                           VERIFY | REJECT | ASK (question goes to the private thread)
 answers in the private thread <-------------------------------------     
                              group card status follows the report: Under review -> Verified -> Alert published -> Resolved
                                            <-------------------------     3 edit draft, explicit approve + publish
                              hazard alert PUBLISHED (once, idempotent) ----------------------------->  citizens on Telegram (State + District)
                              4 later: update / restoration; authority ticks the audience  ---------->  any mix of: citizens (Telegram), group, reporter personally
                                                                                                        (+ reporters whose duplicate report is linked)
```

**Hard rules:** (1) AI output is advisory and is never broadcast by itself. (2) Citizens receive only text an authority explicitly approved and published; **nothing typed in the group or the private chat ever reaches citizens**. (3) AI code and chat code never call notification (Telegram) code. (4) Publishing the same alert twice must be impossible. (5) Group cards expose only category, area and status: never the photo, exact GPS or the reporter's name. (6) Reporter-only updates are allowed before any alert exists.

### 14.2 Status models (three separate state machines)

**Report status** (what the reporter and authority see)

| Status | Meaning | Moves to |
|---|---|---|
| `SUBMITTED` | stored, analysis queued | `PENDING_REVIEW` (analysis finished OR failed OR skipped) |
| `PENDING_REVIEW` | waiting in the authority queue | `VERIFIED`, `REJECTED`, `NEEDS_CLARIFICATION` |
| `NEEDS_CLARIFICATION` | authority asked the reporter a question | `PENDING_REVIEW` (reporter answered) |
| `VERIFIED` | authority confirmed the hazard is real | `ALERT_PUBLISHED`, `RESOLVED` (no public alert needed) |
| `REJECTED` | authority rejected it (reason kept) | terminal |
| `ALERT_PUBLISHED` | a public alert exists for it | `RESOLVED` |
| `RESOLVED` | hazard over (restoration update or manual close) | terminal |

S3's old set `NEW, VERIFIED, REJECTED, RESOLVED` is replaced by this one (`NEW` = `SUBMITTED`).

**Group card status** (what other reporters see; derived live from the report, never copied):

| Report status | Card status (`status` / `statusLabel`) |
|---|---|
| `SUBMITTED`, `PENDING_REVIEW`, `NEEDS_CLARIFICATION` | `UNDER_REVIEW` / "Under review" |
| `VERIFIED` | `VERIFIED` / "Verified" |
| `ALERT_PUBLISHED` | `ALERT_PUBLISHED` / "Alert published" |
| `RESOLVED` | `RESOLVED` / "Resolved" |
| `REJECTED` | `CLOSED` / "Closed" (no reason shown; assumption, see 14.12) |

A report linked as a duplicate (14.5 I) shows the **primary** report's status.

**Analysis status** (per report, separate column): `NOT_STARTED → QUEUED → RUNNING → COMPLETED | FAILED | SKIPPED`. `SKIPPED` = no `AI_API_KEY` configured. After 3 automatic attempts a failure stays `FAILED`, the report still moves to `PENDING_REVIEW`, and the authority can press **Retry analysis**.

**Hazard alert status** (the public alert created from a report): `DRAFT → PUBLISHED → RESOLVED` (optional `WITHDRAWN` from `DRAFT`/`PUBLISHED`). Delivery progress is tracked separately as `dispatchStatus`: `SENDING → COMPLETED | COMPLETED_WITH_FAILURES` (same idea as today's `Alert.status`).

### 14.3 Persistent records (all linked, nothing is deleted)

| Record | Key fields | Linked to |
|---|---|---|
| `Report` | id, `reportCode`, `clientReportId` (UUID from the app, unique per reporter), reporterId, category, note, state, district, latitude, longitude, accuracyM, capturedAt, mockLocation, photoPath, trustFlags[], status, analysisStatus, `linkedToReportId` (nullable), createdAt | reporter |
| `ReportAnalysis` | id, reportId, `inputHash` (unique with reportId), model, promptVersion, result JSON (14.5), status, error, createdAt | report (1..n, latest is shown) |
| `ReportDecision` | id, reportId, decision `VERIFY|REJECT|REQUEST_CLARIFICATION|RESOLVE`, note, decidedBy, decidedAt | report (append-only audit trail) |
| `Clarification` | id, reportId, question, askedBy, askedAt, answer, answeredAt | report (question and answer are also thread messages) |
| `HazardAlert` | id, `alertCode`, reportId (**unique for the primary alert**), hazardType, severity (RED/ORANGE/YELLOW/GREEN), state, district, city?, title, message, instructions, validUntil?, status, `draftVersion`, createdBy, approvedBy, approvedAt, publishedAt, dispatchStatus | report |
| `ReportUpdate` (replaces `HazardAlertUpdate`) | id, reportId, hazardAlertId?, `clientUpdateId` (unique per report), kind `UPDATE|RESTORATION`, message, severity?, `audienceCitizens`, `audienceGroup`, `audienceReporters` (booleans), publishedBy, publishedAt, dispatchStatus (Telegram part only) | report, optional alert |
| `Delivery` | id, updateRef (alert or update), channel `TELEGRAM`, recipientKey (chat id), status `PENDING|SENT|FAILED`, error, attempts, sentAt, **unique (updateRef, channel, recipientKey)** | hazard alert / update |
| `ReporterGroup` | id, state, district (one group per State + District) | reporters assigned to that district |
| `GroupMessage` | id, groupId, kind `REPORT_CARD|TEXT|UPDATE|RESTORATION|SYSTEM`, sender `REPORTER|AUTHORITY|SYSTEM`, senderId?, senderName?, reportId?, updateId?, text?, createdAt; **unique (updateId, groupId)** | group |
| `ThreadMessage` | id, reporterId (thread owner), kind `REPORT_CARD|QUESTION|TEXT|UPDATE|RESTORATION|SYSTEM`, sender `REPORTER|AUTHORITY|SYSTEM`, senderId?, senderName?, reportId?, updateId?, text?, createdAt; **unique (updateId, reporterId)** | reporter |

Chat channels are plain database rows, not `Delivery` rows: a group or thread message is written in the same transaction as the update that caused it, so it cannot half-fail. Only Telegram needs `Delivery` rows (`GROUP_CHAT` is no longer a delivery channel). `GroupMessage` of kind `REPORT_CARD` holds only `reportId`; the card's category, area and status are read live from the report.

Every table is created with nullable or defaulted columns (section 6 rule). The original report, analysis, decisions, alert, updates, deliveries and chat messages stay readable forever for audit and History.

### 14.4 Roles and permissions

| Action | Reporter | Authority | Citizen (Telegram) |
|---|---|---|---|
| Submit a report | yes | no | no |
| See own reports and their status (History), own photo | yes | n/a | no |
| See all reports, photos, AI analysis, trust flags | no | yes | no |
| Read the group chat | own district's group | all groups | no |
| Post text in the group chat | yes (own district) | yes (any group) | no |
| Private thread with the authority (read, write) | own thread | all threads (reporter inbox) | no |
| Answer a clarification | yes, in own thread | no | no |
| Verify / reject / ask clarification | no | yes | no |
| Edit draft, approve and publish alert | no | yes | no |
| Send an update / restoration and choose its audience | no | yes | no |
| Receive public alerts, updates, restorations | via group / thread when the authority ticks them | yes | **Telegram only, only what an authority approved** |
| Retry analysis, link duplicate reports | no | yes | no |

**Only two app roles exist: AUTHORITY and REPORTER** (Rajeev, 2 Nov 2026). Citizens are not app users and never see any chat.

Reporters never see the AI analysis or internal authority notes. Other reporters see only what a group card shows (category, area, status) and what people type in the group. Role checks live in `TokenFilter`/controllers (S3 `User.role`); the current in-memory officers stay AUTHORITY.

### 14.5 API contracts (all PLANNED, none implemented)

Bearer token on everything. Timestamps are UTC ISO-8601. Errors: `400 {"error"}`, `401`, `403` wrong role, `404`, `409` state conflict.

**A. Reporter submits** `POST /api/reports` (multipart: part `photo` JPEG, part `data` JSON). JPEG only, proposed limit 10 MB.
```
data: {"clientReportId":"b3f1...uuid","category":"ROAD_BLOCKED","note":"Landslide across the road near the bridge",
       "state":"Karnataka","district":"Udupi","city":"Kollur",
       "latitude":13.8626,"longitude":74.8147,"accuracyM":12.5,"capturedAt":"2026-11-02T05:41:12Z","mockLocation":false}
-> 201 {"id":41,"reportCode":"REPORT-00041","status":"SUBMITTED","analysisStatus":"QUEUED",
        "trustFlags":["EXIF_MISSING"],"duplicate":false,"createdAt":"..."}
```
Sending the same `clientReportId` again returns **200** with the existing report and `"duplicate":true` and starts no second analysis. Categories: `ROAD_BLOCKED, ROAD_CLEARED, FLOODED_AREA, LANDSLIDE, POWER_OUTAGE, FOOD_NEEDED, WATER_NEEDED, MEDICAL_NEEDED, SHELTER, OTHER`.

**B. Reporter history** `GET /api/reports/mine`
```
-> [{"id":41,"reportCode":"REPORT-00041","category":"ROAD_BLOCKED","status":"ALERT_PUBLISHED","createdAt":"...",
     "clarification":null,
     "alert":{"id":9,"alertCode":"HAZARD-00009","severity":"ORANGE","publishedAt":"...","resolvedAt":null}}]
```

**C. Authority review queue** `GET /api/reports?status=PENDING_REVIEW&district=&category=` and `GET /api/reports/{id}` (adds `photoUrl`=`/api/reports/{id}/photo`, `trustFlags`, `analysis`, `decisions[]`, `clarifications[]`, `hazardAlert`, `possibleDuplicates[]`).
```
analysis: {"status":"COMPLETED","sceneSummary":"Mud and rock across a two-lane road, no vehicles visible.",
  "suggestedCategory":"LANDSLIDE","hazard":"Road blocked by landslide","severity":"ORANGE",
  "consistency":"CONSISTENT","confidence":0.78,
  "caveats":["Photo shows no scale","Location not verified against EXIF"],
  "draftAlert":{"title":"Road blocked near Kollur","message":"A landslide is blocking the road near Kollur bridge.",
                "instructions":["Do not use this road","Use the alternate route via ..."]},
  "model":"...","promptVersion":"v1","analyzedAt":"..."}
```
`consistency` is `CONSISTENT | PARTIAL | INCONSISTENT | UNCLEAR`. `severity` reuses the existing RED/ORANGE/YELLOW/GREEN scale (this replaces S4's 1-5 number). `analysis.status` can be `FAILED` or `SKIPPED` with `error` set and no other fields; the report is still reviewable.
`POST /api/reports/{id}/analysis/retry` (authority) re-queues it; the same `inputHash` returns the stored result instead of calling the AI again.

**D. Authority decision** `POST /api/reports/{id}/decision`
```
{"decision":"VERIFY","note":"Confirmed with district control room"}
{"decision":"REJECT","note":"Photo does not match the location"}
{"decision":"REQUEST_CLARIFICATION","note":"Is the road fully blocked or one lane open?"}
-> {"reportId":41,"status":"VERIFIED"}          409 if the transition is not allowed
```
`REQUEST_CLARIFICATION` writes the note as a `QUESTION` message in the reporter's private thread (14.5 H). The reporter answers by posting in that thread with `reportId` set; the answer is stored on the `Clarification` and the report returns to `PENDING_REVIEW`. (The old `clarification-response` endpoint is removed.)

**E. Edit the draft** `PUT /api/reports/{id}/hazard-alert` (creates or updates the single `DRAFT`; allowed only when the report is `VERIFIED`)
```
{"hazardType":"Landslide","severity":"ORANGE","state":"Karnataka","district":"Udupi","city":"Kollur",
 "title":"Road blocked near Kollur","message":"...","instructions":"Do not use this road. Use the alternate route via ...",
 "validUntil":null}
-> {"id":9,"alertCode":"HAZARD-00009","status":"DRAFT","draftVersion":3,"audienceEstimate":42}
```
`audienceEstimate` = registered citizens in that State + District (same logic as `/api/targets`). Radius targeting arrives with S2; until then the area is State + District, City is descriptive text.

**F. Explicit approval and publication** `POST /api/hazard-alerts/{id}/publish`
```
{"confirm":true,"draftVersion":3}
-> 200 {"id":9,"status":"PUBLISHED","dispatchStatus":"SENDING","publishedAt":"...","alreadyPublished":false}
```
Rules: `confirm` must be exactly `true`; `draftVersion` must equal the current one (409 if the draft changed after the authority looked at it). The server flips `DRAFT → PUBLISHED` with one conditional database update inside a transaction; a second call (double tap, retry, two officers) gets **200 with `"alreadyPublished":true`** and sends nothing. The report moves to `ALERT_PUBLISHED`, approver and time are recorded, and `Delivery` rows are created for citizens (Telegram). The group card flips to "Alert published" by itself and a `SYSTEM` line is added to the reporter's private thread. Whether the alert text is also posted in the group is **not** automatic: the authority can send it as an update with `group:true` (G).

**G. Updates and restoration** `POST /api/reports/{id}/updates` (authority only; replaces `/api/hazard-alerts/{id}/updates`)
```
{"clientUpdateId":"uuid","kind":"UPDATE","message":"Crew dispatched, expected in 40 minutes.",
 "audience":{"citizens":false,"group":false,"reporters":true}}
{"clientUpdateId":"uuid","kind":"UPDATE","message":"Debris clearing started, one lane open from 6 pm.","severity":"YELLOW",
 "audience":{"citizens":true,"group":true,"reporters":true},"confirm":true}
{"clientUpdateId":"uuid","kind":"RESTORATION","message":"Road reopened to all traffic.",
 "audience":{"citizens":true,"group":true,"reporters":true},"confirm":true}
-> 201 {"id":3,"kind":"RESTORATION","reportStatus":"RESOLVED","dispatchStatus":"SENDING",
        "sent":{"reporters":2,"group":true,"citizens":"SENDING"}}
```
Rules:
- At least one audience flag must be `true` (400). The app defaults to **reporters only**; the authority must tick the others.
- `citizens:true` needs the report's alert to be `PUBLISHED` (409 otherwise) and `confirm:true` (400 otherwise), because this is a public message. Citizens = those who received the original alert (delivery rows), same Telegram layout as 14.6 with heading `UPDATE` or `RESTORED`. New registrants are not added in v1.
- `group:true` writes an `UPDATE`/`RESTORATION` message into the district's group (shows the authority's name, e.g. "District control room").
- `reporters:true` writes into the private thread of the report's reporter **and** of every reporter whose duplicate report is linked to this report (I).
- `UPDATE` is allowed while the report is `VERIFIED` or `ALERT_PUBLISHED`; `RESTORATION` while `VERIFIED` or `ALERT_PUBLISHED`. Otherwise 409.
- `RESTORATION` sets the report to `RESOLVED` and, if an alert is `PUBLISHED`, the alert to `RESOLVED`, whatever audience is ticked (decided 2 Nov 2026: status describes the hazard, not who was messaged). Group cards then show "Resolved". Safeguard in the app, not the server: when an alert is `PUBLISHED` the app pre-ticks *Citizens*, and if the authority unticks it a dialog says "Citizens received the original alert and will not be told it is over. Continue?". The server only records the result; the response includes `citizensNotified` (true/false) so the alert detail can show "Resolved, citizens not notified".
- The same `clientUpdateId` again returns **200** with the stored update and sends nothing twice.

`GET /api/hazard-alerts/{id}` returns the alert, its updates and a Telegram delivery summary `{sent, failed, pending}`. `POST /api/hazard-alerts/{id}/redeliver` retries only `FAILED`/`PENDING` Telegram rows.

**H. Chat: Group and private thread** (polling every 3 s with `afterId`; text 1-1000 characters, trimmed, else 400)

Reporter side:
```
GET  /api/groups/mine/messages?afterId=0
-> {"groupId":3,"groupName":"Udupi, Karnataka",
    "messages":[
      {"id":12,"kind":"REPORT_CARD","senderName":null,"text":null,"reportId":null,
       "card":{"category":"ROAD_BLOCKED","area":"Kollur, Udupi","status":"UNDER_REVIEW","statusLabel":"Under review"},"createdAt":"..."},
      {"id":13,"kind":"TEXT","sender":"REPORTER","senderName":"Anil","text":"Water rising near the bus stand","card":null,"createdAt":"..."},
      {"id":15,"kind":"UPDATE","sender":"AUTHORITY","senderName":"District control room","text":"Debris clearing started, one lane open from 6 pm.","card":null,"createdAt":"..."}],
    "cards":{"12":"VERIFIED"}}
POST /api/groups/mine/messages {"text":"..."}                     -> 201 the message
GET  /api/chat/thread/messages?afterId=0                         -> {"messages":[...],"cards":{"31":"PENDING_REVIEW"}}
POST /api/chat/thread/messages {"text":"One lane is open","reportId":41?}   -> 201 the message
```
Authority side:
```
GET  /api/groups                                   -> [{"groupId":3,"state":"Karnataka","district":"Udupi","lastMessageAt":"..."}]
GET  /api/groups/{groupId}/messages?afterId=       -> same shape as above, but cards carry reportId so a tap opens the report detail
POST /api/groups/{groupId}/messages {"text":"..."}
GET  /api/chat/threads                             -> [{"reporterId":5,"reporterName":"Anil","state":"Karnataka","district":"Udupi",
                                                        "lastMessageText":"...","lastMessageAt":"...","awaitingReply":true}]
GET  /api/chat/threads/{reporterId}/messages?afterId=
POST /api/chat/threads/{reporterId}/messages {"text":"...","reportId":41?}
```
Thread messages:
```
{"id":31,"kind":"REPORT_CARD","sender":"REPORTER","senderName":"Anil","reportId":41,"text":"Landslide across the road near the bridge",
 "card":{"reportCode":"REPORT-00041","category":"ROAD_BLOCKED","status":"PENDING_REVIEW","photoUrl":"/api/reports/41/photo"},"createdAt":"..."}
{"id":32,"kind":"QUESTION","sender":"AUTHORITY","senderName":"Officer Asha","reportId":41,"text":"Is the road fully blocked or one lane open?","card":null,"createdAt":"..."}
{"id":33,"kind":"TEXT","sender":"REPORTER","senderName":"Anil","reportId":41,"text":"One lane is open","card":null,"createdAt":"..."}
```
Rules:
- A successful `POST /api/reports` (A) creates the group card and the thread `REPORT_CARD` together; a repeated `clientReportId` creates neither again.
- `cards` maps `messageId -> status` for the latest 100 cards, so card statuses refresh on every poll even though `afterId` hides old messages. Group cards never contain photo, GPS, reporter name or `reportCode`; `reportId` is filled for authority only.
- A reporter can post only in their own district's group (403 otherwise) and only reference their own `reportId`. If that report is `NEEDS_CLARIFICATION`, the message counts as the answer.
- `QUESTION`, `UPDATE`, `RESTORATION`, `REPORT_CARD` and `SYSTEM` messages are created only by the server (decision, updates and submit endpoints), never by a plain POST. Status changes add a short `SYSTEM` line to the private thread.
- The `chat/` code has no import of `notify/`: nothing posted in a chat can reach Telegram or citizens.
- Messages are never edited or deleted in v1.

**I. Duplicate reports of the same hazard** (planned): `possibleDuplicates[]` lists reports with the same category within ~500 m and 6 hours (thresholds are a guess). `POST /api/reports/{id}/link {"primaryReportId":41}` (authority) marks this report a duplicate of the primary one: it sets `linkedToReportId`, no second public alert is created, History and the group card show the primary's status, and `reporters:true` updates on the primary also reach this reporter's private thread. 409 if the report already has its own alert, or if the target is itself linked.

### 14.6 Citizen message (Telegram, existing architecture)

Sent through `TelegramClient`/a `NotificationChannel` plain-text method (a small addition: the current `send(Recipient, Alert)` only knows `Alert`).
```
ORANGE ALERT - LANDSLIDE
Road blocked near Kollur, Udupi, Karnataka

A landslide is blocking the road near Kollur bridge.
- Do not use this road
- Use the alternate route via ...

Reported: 02 Nov 2026, 11:11 am IST
Approved and published: 02 Nov 2026, 11:40 am IST   (valid until: ...)
Issued by: <officer / authority id>
```
Updates and restorations use the same layout with the heading `UPDATE` or `RESTORED` and a reference to the original alert code. Audience for the first alert: registered citizens of the alert's State + District. For updates and restorations the authority chooses the audience (14.5 G).

### 14.7 Architecture requirements and package split

- `report/` (storage, trust flags, status transitions), `analysis/` (`ReportAnalyzer` interface, provider adapter, async worker, **no imports from `notify/`**), `hazard/` (draft, approval, publish, updates, Telegram delivery rows), `chat/` (`ReporterGroup`, `GroupMessage`, `ThreadMessage`, polling endpoints, a `ChatService` that `report/` and `hazard/` call to write cards, updates and system lines; **no imports from `notify/`**), `notify/` (existing Telegram code, reused unchanged except the plain-text send). Only `hazard/` triggers Telegram, and only from the authority endpoints.
- Analysis runs on its own executor/queue after the report is committed; the submit call never waits for the AI.
- Idempotency: `Report.clientReportId`, `ReportUpdate.clientUpdateId`, unique `(reportId, inputHash)` on analyses, unique primary `HazardAlert` per report, conditional `DRAFT → PUBLISHED` update, unique `Delivery (updateRef, channel, recipientKey)`, unique `(updateId, groupId)` and `(updateId, reporterId)` on chat messages. A restart during sending resumes the `PENDING` Telegram rows only.
- Chat writes happen in the same transaction as the action that causes them (submit, decision, update); Telegram sending happens after commit.
- Secrets only from env vars (`AI_API_KEY`). The photo is read from `./uploads/reports/` and served only through the authenticated endpoint, to the authority or to the owning reporter.

### 14.8 Failure handling

| Situation | Behaviour |
|---|---|
| AI timeout / provider error | up to 3 automatic retries with backoff, then `analysisStatus=FAILED`; report still goes to `PENDING_REVIEW`; authority sees "analysis unavailable" and can retry |
| AI returns invalid or unparsable JSON | treated as failure; raw text is kept in `error` for debugging, never shown as an alert |
| `AI_API_KEY` missing | `SKIPPED`; the whole flow works manually |
| AI says `INCONSISTENT` or low confidence | shown as a warning chip; does not block or auto-reject |
| Not a JPEG / too large / photo missing | `400`, nothing stored |
| EXIF missing, GPS mismatch, mock location | accepted; trust flags shown (EXIF can be forged, never trusted alone) |
| Same `clientReportId` sent twice (retry on bad network) | `200 duplicate:true`, no second analysis, no second card |
| Same `clientUpdateId` sent twice | `200` with the stored update, nothing sent twice |
| Publish pressed twice / two officers | second call `200 alreadyPublished:true`, nothing re-sent |
| Draft edited after the officer opened it | publish with the old `draftVersion` gets `409`; the officer re-reads and approves again |
| Update with `citizens:true` but no published alert, or without `confirm:true` | `409` / `400`, nothing sent anywhere |
| Some Telegram deliveries fail (user blocked the bot, HTTP error) | alert stays `PUBLISHED`, `dispatchStatus=COMPLETED_WITH_FAILURES`, failures listed, `redeliver` retries only those |
| Server restarts while sending | `PENDING` Telegram rows are picked up again on startup; `SENT` rows are never resent |
| A chat write fails | the whole action (submit, decision, update) rolls back, so nothing is half-sent; the app shows an error and the user retries with the same client id |
| Reporter posts while offline | not queued in v1; the app shows "not sent, tap to retry" |
| Chat poll fails | the app keeps the last messages and shows "reconnecting", retries every 3 s with backoff |
| Reporter posts in another district's group, or references another reporter's report | `403` |
| Text empty or over 1000 characters | `400` |
| Authority token expired | `401`, app returns to login, drafts are saved server-side so nothing is lost |

### 14.9 Acceptance criteria (a slice is not done until these pass on the real phone)

1. A reporter submits a photo report; it appears in `GET /api/reports/mine` as `SUBMITTED` within 2 seconds and the call does not wait for the AI.
2. Submitting the same `clientReportId` twice creates one report, one analysis, one group card and one thread card.
3. With a valid `AI_API_KEY` the report gets a stored analysis with all fields in 14.5; with no key or a forced failure the report still reaches the authority queue with the analysis marked unavailable.
4. **No citizen receives anything before the authority publishes.** Verified in Telegram after submission, after `VERIFY`, and after text is typed in the group and in the private chat.
5. The authority sees photo, GPS (and accuracy), details, AI suggestions and trust flags on one screen.
6. `REQUEST_CLARIFICATION` shows the question in the reporter's private thread; the reporter's reply there returns the report to the queue.
7. Only an authority can publish or send updates; a reporter token gets `403`.
8. `publish` without `confirm:true`, or with a stale `draftVersion`, sends nothing.
9. Publishing once delivers the Telegram message (hazard type, location, severity, instructions, times) to the registered citizens of that State + District; the group card changes to "Alert published" and the reporter's History row changes to `ALERT_PUBLISHED`.
10. Calling `publish` a second time returns `alreadyPublished:true` and no extra Telegram message arrives.
11. **Group card privacy:** a second reporter in the same district sees the first reporter's card within one poll (≤ 3 s) with category, area and status only; the JSON contains no photo URL, coordinates, reporter name or report code.
12. The card status follows the report through Under review → Verified → Alert published → Resolved without the reporter reloading.
13. A reporter can post text in the group; other reporters and the authority see it within one poll; it never reaches Telegram.
14. An update with `reporters:true` only ("crew dispatched") reaches only that reporter's private thread: not the group, not citizens.
15. An update with `citizens:true` reaches the original Telegram recipients only after an explicit confirmation; `group:true` posts it in the group.
16. With a linked duplicate, a `reporters:true` update also lands in the duplicate reporter's private thread.
17. A `RESTORATION` reaches exactly the audience that was ticked; the alert and report become `RESOLVED` and the group card shows "Resolved". With an alert published, citizens are pre-ticked, and unticking them shows the warning dialog; if the officer continues, the alert detail shows "Resolved, citizens not notified".
18. After a forced Telegram failure for one citizen, the alert shows `COMPLETED_WITH_FAILURES` and `redeliver` sends only to that citizen.
19. Report, analysis, decisions, alert, updates, deliveries and chat messages are all still readable after resolution.

### 14.10 Implemented vs planned

| Item | State |
|---|---|
| Telegram bot, State + District recipients, `NotificationChannel`, `TelegramClient` | **Implemented and verified** (S0, S1) |
| Authority login (in-memory officers) | **Implemented** |
| Existing broadcast `POST /api/alerts` with I'm safe / I need help | **Implemented and verified** |
| Roles beyond AUTHORITY, reports, photo upload, EXIF/trust flags, History | **Implemented and verified (S3a, 3 Nov 2026)**, `test-s3a.ps1` 53/53 |
| Group chat, private thread, report cards, polling, authority group view and reporter inbox | **Reported done by Rajeev (3 Nov 2026), tested on the real phone; code not seen by Claude** (S3b) |
| AI analysis, queue, retry | Planned (S4a); not started, no provider chosen or keyed |
| Review queue, decisions, clarification (via private thread) | Planned (S4b) |
| Draft, approval, idempotent publish, Telegram hazard message, History link | Planned (S4b) |
| Updates and restoration with per-update audience, redelivery, duplicate linking | Planned (S4c) |
| Radius targeting | Planned (S2); until then State + District |

### 14.11 Suggested build order (vertical slices, section 8 protocol; estimates are rough guesses)

| Slice | Content | Rough effort |
|---|---|---|
| S3a | `User` roles, `Report` + photo upload + trust flags, `/reports/mine`, authority list/detail/photo, History screen, reporter camera/report form, reporter three-tab shell (tabs 1 and 2 are placeholders; "New report" button lives in tab 2) | 6-8 h — **DONE (3 Nov 2026)** |
| S3b | **Chat:** `ReporterGroup`, `GroupMessage`, `ThreadMessage`, cards on submit, polling endpoints (14.5 H), reporter tabs 1 and 2 live, authority Groups view and reporter Inbox, 3 s polling | 4-6 h (Claude's guess, both sides) — **DONE (3 Nov 2026)** |
| S4a | `ReportAnalyzer` + async worker + `ReportAnalysis`, analysis chip in the authority detail screen, retry | 3-4 h |
| S4b | decisions + clarification (in the thread), draft + publish (idempotent), Telegram hazard message, History shows published alert | 4-5 h |
| S4c | updates and restoration with audience picker, redeliver, duplicate linking | 3-4 h |

Each slice starts with the contract (this section) and a sample JSON for Antigravity, then mock UI, then backend, then the real-phone acceptance test.

### 14.12 Open questions for Rajeev

Answered 2 Nov 2026: reporters **can** post in the group (was question 1).
1. Which AI vision provider and key (Claude or Gemini), and is a paid key available for the demo? **Open (3 Nov 2026).** S4a is built provider-agnostic with a fake analyzer; the real adapter is a later step once this is answered.
2. Should updates also reach citizens who registered after the original alert (v1 says no)?
3. Should an authority be able to delete a group message (moderation)? v1 says no deletion.
4. A rejected report's group card shows "Closed" with no reason. Or should it disappear from the group?

QR relay (section 15), assumptions to confirm: (a) the demo QR types are `STATUS` and `READING` only; (b) the friend's phone shows the QR from a small static demo page that Claude writes (`qr-demo.html`); (c) scans appear in a Scans list, with an optional map pin as a stretch.

### 14.13 App layout (for Antigravity; mock-first on the 14.5 JSON)

**Reporter app, bottom tabs:**
1. **Group:** header "Udupi, Karnataka"; feed of compact cards (category icon, area, status chip), typed messages as bubbles with the sender's name, authority updates styled as announcements; compose bar. No photo anywhere.
2. **Authority:** the private thread. Report cards show the photo thumbnail, category and status; questions, updates and restoration notes arrive as authority bubbles; compose bar; a **New report** button opens camera → category, note, location → submit, and the card appears in the thread.
3. **History:** `GET /api/reports/mine` list with status chips, link to the published alert, "linked to REPORT-xxxxx" for duplicates.

**Authority app, new destinations beside the existing tabs:**
- **Groups:** group picker → feed → compose; tapping a card opens the report detail (`reportId`).
- **Inbox:** reporter threads sorted by last message, badge for `awaitingReply` → thread view with compose.
- **Report detail → Send update:** three switches: *Citizens on Telegram*, *Reporter group*, *Reporter personally (and linked duplicates)*. Citizens is off by default, disabled with a hint when no alert is published, and needs a confirmation dialog ("Send to N citizens?") that sets `confirm:true`. A **Restoration** option uses the same sheet; with a published alert, Citizens is pre-ticked and unticking it shows a warning dialog (14.5 G).

Polling: every 3 s while the screen is visible (`LaunchedEffect`), `afterId` for new messages, apply `cards` to existing card messages, stop when hidden, show "reconnecting" on errors. Unread badges can be computed client-side from the last seen id.


### 14.14 S3a contract (FROZEN 2 Nov 2026; backend implemented and verified 3 Nov 2026)

> **STATUS (3 Nov 2026):** backend done, `test-s3a.ps1` 53 of 53 on Rajeev's machine; Android reported working on the real phone by Rajeev. Sample JSON for the mock lives in `s3a-mock/`.

Scope of S3a: login with roles, authority creates reporter accounts, reporter submits a geotagged photo report, reporter History, authority list/detail/photo. **Not in S3a:** chat (S3b), AI analysis (S4a), decisions/publish/updates (S4b/S4c). Mock files: `s3a-mock/*.json` (file name in brackets below).
Everything in 14.5 A/B/C still applies; this section pins down the exact S3a behaviour and adds what 14.5 left open. Where they differ, **this section wins for S3a**.

### Changes to existing endpoints (additive)
`POST /api/auth/login {officerId, pin}` keeps its request. The response gains fields; old fields are unchanged. [`login-authority.json`, `login-reporter.json`]
```
-> {"token","officerName","role":"AUTHORITY"|"REPORTER","userId","state":null|"Karnataka","district":null|"Udupi"}
```
- `officerName` is the display name for both roles (kept for compatibility). `state`/`district` are the reporter's assigned area, null for authority.
- **Role gating (new):** a REPORTER token calling any authority endpoint (`/api/alerts`, `/api/targets`, `/api/recipients`, `/api/coverage`, `/api/demo/*`, `/api/users`, `GET /api/reports`, `GET /api/reports/{id}`) gets **403**. Reporters may call: `/api/auth/login`, `/api/health`, `/api/locations`, `POST /api/reports`, `GET /api/reports/mine`, `GET /api/reports/{id}/photo` (own reports only). Authority may call everything except `POST /api/reports` (403) and `/api/reports/mine` (403).
- Tokens stay in memory (restart = log in again). 401 without a valid token as before.

### New: reporter accounts (authority only)
```
POST /api/users  {"userId","pin","displayName","state","district"}          [users-create-request.json]
-> 201 {"id","userId","displayName","role":"REPORTER","state","district"}   [users-create-201.json]
GET  /api/users  -> [{id,userId,displayName,role,state,district}]            [users-list.json]
```
- `userId` 3-30 chars `[a-z0-9._-]`, `pin` 4-8 digits, `state`/`district` validated against the generated catalog (name or 2-letter code accepted, stored as the canonical name). 409 if `userId` already exists (also when it equals a config officer ID). 400 on bad input.
- Demo seed (config `REPORTER_USERS`, same format idea as `AUTHORITY_USERS`: `userId:pin:Name:State:District,...`): **`anil.rep / 1111`, Anil, Karnataka, Udupi** exists on first start. Change before any public demo. Reporters are stored in the database; officers stay in config.

### New: submit a report (reporter only)
`POST /api/reports`, `multipart/form-data`, two parts: `photo` (file, `image/jpeg`) and `data` (JSON, content type `application/json`). [`report-create-data-part.json` is the `data` part]
```
data: {"clientReportId":"<uuid>","category":"ROAD_BLOCKED","note":"...","state":"Karnataka","district":"Udupi","city":"Kollur",
       "latitude":13.8626,"longitude":74.8147,"accuracyM":12.5,"capturedAt":"2026-11-02T05:41:12Z","mockLocation":false}
-> 201 {"id","reportCode","status":"SUBMITTED","analysisStatus":"NOT_STARTED","trustFlags":[...],"duplicate":false,"createdAt"}   [report-create-201.json]
-> 200 same body with "duplicate":true when clientReportId was already used by this reporter                                     [report-create-duplicate-200.json]
```
Validation (all 400 with `{"error"}`, nothing stored; messages in `errors.json`):
- `photo`: required; **JPEG only** (checked by file content, not only the header); **max 10 MB**. The app should scale the long side to about 1600 px at JPEG quality 80-85 so normal photos stay far below 10 MB.
- `clientReportId`: required UUID. **The app generates it once per report and reuses it on every retry.**
- `category`: one of `ROAD_BLOCKED, ROAD_CLEARED, FLOODED_AREA, LANDSLIDE, POWER_OUTAGE, FOOD_NEEDED, WATER_NEEDED, MEDICAL_NEEDED, SHELTER, OTHER`.
- `note`: optional, trimmed, max 500 characters. `city`: optional, max 60.
- `latitude` -90..90, `longitude` -180..180 (required). `accuracyM` optional, may be null. `capturedAt` required ISO-8601 UTC, not more than 5 minutes in the future. `mockLocation` required boolean (the app reads `Location.isMock` / `isFromMockProvider`).
- `state`/`district` must be valid catalog names **and equal the reporter's assigned State + District** (else 400 `Report must be inside your assigned district`). In S3a the app pre-fills them from the login response and shows them read-only. (Assumption; relax later if a reporter should cover other districts.)
- Status after submit is `SUBMITTED` and **stays `SUBMITTED` in S3a** (no analysis or review exists yet). S4a changes the initial `analysisStatus` to `QUEUED` and moves reports to `PENDING_REVIEW`; the UI must render every status in 14.2, not only the ones S3a produces.

**Trust flags** (server computed, advisory, never block a report): `EXIF_MISSING` (no readable EXIF GPS in the photo), `GPS_MISMATCH` (EXIF GPS more than 200 m from the submitted position), `OLD_PHOTO` (EXIF time more than 30 minutes before the server received the report), `MOCK_LOCATION` (`mockLocation=true`). EXIF can be stripped or forged; flags are hints for the human reviewer. The thresholds are guesses and are constants in one place.
Should the app write GPS into the JPEG EXIF at capture (14.13)? Yes where possible; if it cannot, `EXIF_MISSING` is the honest result.

### New: reporter History (reporter only)
`GET /api/reports/mine` -> the caller's reports, **newest first**, no paging in S3a (cap 200). [`reports-mine.json`]
```
[{"id","reportCode","category","note","state","district","city","status","createdAt",
  "linkedToReportCode":null|"REPORT-00041","clarification":null,"alert":null|{"id","alertCode","severity","publishedAt","resolvedAt"}}]
```
S3a always returns `clarification: null`, `alert: null`, `linkedToReportCode: null` (S4 fills them). The sample file contains later-slice states (`REJECTED`, `ALERT_PUBLISHED`, `RESOLVED`, a linked duplicate) **only so the UI can be built once**; the real S3a backend will only return `SUBMITTED`.

### New: authority list, detail, photo
```
GET /api/reports?status=&state=&district=&category=&limit=      [reports-list-authority.json]
-> newest first, default limit 100, max 500; every filter optional; unknown status/category -> 400
[{"id","reportCode","category","state","district","city","latitude","longitude","accuracyM","status","analysisStatus",
  "trustFlags":[],"reporterId","reporterName","createdAt"}]
```
(`status=PENDING_REVIEW` returns reports in `PENDING_REVIEW` only. In S3a the useful queue is `status=SUBMITTED`; the app should call without a status filter.)
```
GET /api/reports/{id}                                            [report-detail-authority.json, report-detail-with-exif.json]
-> list fields + note, capturedAt, mockLocation, photoUrl, linkedToReportId,
   "exif":{"hasGps","latitude","longitude","takenAt","distanceToReportedM"},
   and the stable S4 placeholders: "analysis":null,"decisions":[],"clarifications":[],"hazardAlert":null,"possibleDuplicates":[]
GET /api/reports/{id}/photo   -> the JPEG bytes (Content-Type image/jpeg); authority: any report; reporter: own report only (403 otherwise); 404 unknown
```
**Photo needs the Bearer header**, so an image library must send it (Coil `ImageRequest.Builder.addHeader("Authorization","Bearer …")`) or the app downloads the bytes through `BackendApi` and decodes a Bitmap. No public photo URLs exist. Photos are stored under `./uploads/reports/{id}.jpg`, outside the database.

### Error summary
`400 {"error"}` validation · `401` no/invalid token · `403 {"error"}` wrong role or someone else's report · `404 {"error"}` · `409 {"error"}` duplicate `userId`. Timestamps UTC ISO-8601, show IST.

### What Antigravity builds in S3a (mock first)
1. **Role-aware routing after login** using `role`: AUTHORITY keeps today's Home; REPORTER lands in the three-tab shell.
2. **Reporter shell:** tabs *Group* (placeholder "Coming in the next update"), *Authority* (placeholder + a **New report** button), *History* (live: list from `reports-mine.json`, status chip per 14.2, category icon, empty state, pull to refresh).
3. **New report flow:** CameraX capture → preview with retake → category chips → note (500 max, counter) → location block (device GPS taken at capture, accuracy shown, `Location.isMock` → `mockLocation`) → Submit. Generate `clientReportId` when the form opens and reuse it on retry. States: sending, success (shows `reportCode`), error with Retry (same id), duplicate treated as success.
4. **Authority additions:** a *Reports* destination: list (category icon, area, trust-flag chips, time in IST, status chip) → detail (photo loaded with the Bearer header, coordinates with accuracy, EXIF block, trust flags, note, reporter name, **Navigate** via `geo:`). Optional stretch: show report pins as a second layer on the existing Map tab using `latitude/longitude` from the list.
5. **Authority → Reporters** (small screen or dialog): create a reporter with `POST /api/users`, list with `GET /api/users`.
6. New dependencies expected: CameraX (`camera-core, camera-camera2, camera-lifecycle, camera-view`), `androidx.exifinterface`, Google Play services **or** the platform `LocationManager` for GPS (list which one before adding), Coil only if you do not decode bytes yourself. Manifest: `CAMERA`, `ACCESS_FINE_LOCATION`. Listing them first is required by the kickoff prompt.

### S3a acceptance test (real phone)
1. Authority logs in, creates reporter `meena.rep / 2222` (Udupi). 2. Log out, log in as `anil.rep / 1111` -> three-tab shell. 3. New report: photograph something, pick *Road blocked*, add a note, Submit -> success with `REPORT-000xx`, row appears in History as *Submitted*. 4. Submit again with the same form (airplane-mode retry or double tap) -> still one report. 5. Log in as authority -> Reports shows the report with the trust flags; detail shows the photo, coordinates and EXIF block. 6. As reporter, open another reporter's report id by URL (test script) -> 403. 7. Reporter token on `/api/alerts` -> 403. 8. Send a text file renamed `.jpg` -> 400.

### Backend notes (Claude, for S3a)
New package `report/` (`Report`, `ReportRepository`, `ReportService`, `PhotoStorage`, `TrustFlags`), `user/` (`AppUser`, `UserRepository`), changes to `AuthService` (DB reporters + role), `TokenFilter` (role + userId request attributes), `ApiController`/new `ReportController`/`UserController` (role checks), `ApiExceptionHandler` (400 for oversize upload, 403), `application.properties` (multipart limit 10 MB, `uploads.dir`, `REPORTER_USERS`), `build.gradle.kts` (+ `com.drewnoakes:metadata-extractor` for EXIF). All new columns nullable/defaulted, no DB reset. New test script `test-s3a.ps1`. **Only syntax-checkable in the Claude sandbox; Rajeev's Gradle build is the real test.**

## 15. S6 — QR scan relay (field data from another phone, scanned into the app)

> **STATUS: SPEC ONLY (written 3 Nov 2026 from Rajeev's request). Nothing here is implemented.** Deliberately small: a demo-grade relay, not a full process. Effort figures are Claude's rough guesses.

### 15.1 What it is, and what it is not

Rajeev saw this at a hackathon: a friend's phone shows a QR code, the app scans it, the data reaches the backend and appears on the screen. Available to **REPORTER and AUTHORITY**.

**Demo plan (Rajeev, 3 Nov 2026): the app scans a *photo* of a QR code**, i.e. an image of another phone's screen or a printed code, either taken in the app or **picked from the gallery** (also a screenshot sent over WhatsApp). The photo is decoded on the phone; only the decoded text is sent to the backend. Live camera scanning is a secondary option if time allows. The photo itself is not uploaded or stored.

We build only: **scan → send to backend → store → display.** Accurate pitch line: *a person, a sensor tag or a printed card can hand over a small status packet with no network of their own; the responder's phone carries it to the backend.* We do not claim more than that.

**Not built:** generating QR codes inside the app, signing or encrypting payloads, accounts for the QR owner, pairing, BLE/NFC, ESP32 (now optional S8), and any automatic alert, report or Telegram message from a scan.

### 15.2 QR payload (v1)

Plain UTF-8 JSON text, at most 1000 characters. Only the whitelisted fields below are kept; unknown fields are dropped.

```
STATUS  (a person or group):
{"v":1,"type":"STATUS","name":"Ravi","status":"NEED_HELP","need":"WATER","people":3,
 "latitude":13.8626,"longitude":74.8147,"note":"Roof of the school","at":"2026-11-03T07:10:00Z"}
  required: v, type, status (SAFE | NEED_HELP)
  optional: name (≤ 60), need (FOOD_WATER | MEDICAL | EMERGENCY | SHELTER | OTHER), people (1-500),
            latitude + longitude (both or neither), note (≤ 200), at (ISO-8601 UTC)

READING  (a sensor tag or a manual gauge):
{"v":1,"type":"READING","sensor":"water_level_cm","value":183.2,"unit":"cm","deviceId":"ESP-01",
 "latitude":13.86,"longitude":74.81,"at":"2026-11-03T07:10:00Z"}
  required: v, type, sensor (≤ 40), value (number)
  optional: unit (≤ 10), deviceId (≤ 40), latitude + longitude, note (≤ 200), at
```

A small static page `qr-demo.html` (Claude writes it with the backend step) lets the friend's phone fill in these fields and show the QR, with no app install and no internet needed once loaded. It is a demo tool and is labelled as such.

### 15.3 API contract (PLANNED; sample JSON is written when the slice starts)

Both roles may call these (TokenFilter: reporter may call `POST /api/scans`, `GET /api/scans`, `GET /api/scans/{id}`). Bearer token as everywhere.

```
POST /api/scans
{"clientScanId":"<uuid>","raw":"<the decoded QR text>","scannedAt":"2026-11-03T07:11:02Z",
 "latitude":13.8631,"longitude":74.8150,"accuracyM":14.0}          (scanner's own GPS, optional)
-> 201 {"id":7,"scanCode":"SCAN-00007","type":"STATUS","scannerName":"Anil","scannerRole":"REPORTER",
        "payload":{"status":"NEED_HELP","need":"WATER","people":3,"name":"Ravi","latitude":13.8626,"longitude":74.8147,
                   "note":"Roof of the school","at":"2026-11-03T07:10:00Z"},
        "scannedAt":"...","createdAt":"...","duplicate":false}
-> 200 same body with "duplicate":true   (same clientScanId, or the same raw text from the same user within 60 s)
-> 400 {"error":"This QR is not a DisasterAlert code"}   (empty, > 1000 chars, not JSON, v != 1, unknown type,
                                                         missing/invalid field; nothing is stored)

GET /api/scans?type=&limit=      AUTHORITY: all scans, newest first (default 50, max 200). REPORTER: own scans only.
GET /api/scans/{id}              same body as above; a reporter may open only their own (403 otherwise), 404 unknown
```

Rules: the payload is **untrusted data**. It is stored as data, shown as plain text (never as HTML, never opened as a link), never sent to Telegram, and never creates an alert or a report by itself. The UI labels it **"Scanned data, not verified"**. The scanner's GPS (where the scan happened) is stored separately from any location inside the payload. There is no delete or edit.

Record `QrScan`: id, scanCode, `clientScanId` (unique per scanner), scannerId, scannerRole, scannerName, type, rawHash, payloadJson, scannedAt, scannerLatitude/Longitude/AccuracyM (nullable), createdAt. All columns nullable or defaulted (section 6 rule).

### 15.4 App (Antigravity, mock first)

- A **Scan QR** action reachable by both roles (authority: Home; reporter: a button in the shell). Antigravity picks the spot and reports it.
- Scanner screen with two inputs: **Pick a photo** (system photo picker, no storage permission needed) and **Take a photo** (CameraX still capture); live camera scanning is optional. QR codes only. The image is decoded on the device (ML Kit barcode scanning `InputImage.fromFilePath`/bitmap, or ZXing); if no QR is found the screen says "No QR code found in this photo, try a closer, straighter photo" and sends nothing. One code per photo (the first). Then sending → result card with the parsed fields, the "not verified" label and **Scan another**. Errors: the 400 text above; offline → "Not sent, tap to retry" with the same `clientScanId`.
- **Scans** list (reporter: own; authority: all with scanner name); tap a row for detail. If the payload has coordinates: **Navigate** (`geo:`); an optional pin on the Map tab is a stretch.
- New dependency expected (list it first): a QR decoder, e.g. ML Kit barcode scanning (bundled variant) on top of the CameraX already used in S3a, or ZXing.

### 15.5 Acceptance test (real phone)

1. A friend opens `qr-demo.html` on their phone and shows a STATUS QR (NEED_HELP, WATER, 3 people, a location); Rajeev takes a photo of that screen (or the friend sends a screenshot).
2. The reporter scans that **photo** in the app (picked or taken): within 2 s the result card shows the parsed fields and a row appears in Scans.
3. On the authority phone the same row appears (with the scanner's name) after a refresh.
4. Scanning the same QR again creates no second row.
5. A photo with no QR in it shows "No QR code found"; scanning any other QR (a website address) shows "This QR is not a DisasterAlert code" and stores nothing.
6. Nothing arrives in Telegram; no alert and no report is created.

### 15.6 Build order (section 8 protocol)

Claude: contract + `s6-mock/` sample JSON + Antigravity prompt (≈ 20 min) → Antigravity: scanner and Scans UI on mock → Claude: backend (`scan/` package, TokenFilter paths, `test-s6.ps1`, `qr-demo.html`) → Antigravity: swap mock for real → Rajeev: acceptance test, commit, Status Board.
