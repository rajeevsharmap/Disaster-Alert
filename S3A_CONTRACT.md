## 14.14 S3a contract (FROZEN 2 Nov 2026) — roles, reports, photo, History

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
