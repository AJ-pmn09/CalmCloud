# Wearable & Health: Roadmap to Best-in-Market

This document lists **actionable improvements** to make MindAigle’s wearable/health experience best-in-class (on par with Samsung Health, Apple Health, Fitbit, Garmin Connect). It is based on a full pass over the app and backend.

---

## Current State (Summary)

- **Health Connect** integration: read-only steps, sleep, heart rate, active calories, distance, SpO₂, blood pressure, body mass. Data stays on device; no automatic push to backend.
- **Wearable tab**: Hero steps card, status chip, 2-column metric grid, permission flow, auto-refresh every 30s.
- **Student Home**: Shows `activityData` from backend (heart_rate, steps, sleep_hours, hydration, etc.) and local hydration. Backend `activity_logs` is populated by seed/other flows, not from the app’s Health Connect read.
- **Gap**: Wearable data is **only on the Wearable tab**. It is not synced to the backend, so Home, Trends, and staff dashboards do not see live wearable data.

---

## 1. Data & Sync

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 1.1 | **Sync Health Connect → Backend** | Optional (user consent) daily or on-open sync: push today’s steps, sleep, heart rate (e.g. latest or avg), distance, active calories from Health Connect to `activity_logs` via a new API (e.g. `POST /api/activity-logs/sync`). Upsert by `student_id` + `date`. | Unifies wearable data across Home, Trends, Achievements, and staff views. |
| 1.2 | **Backend API for activity sync** | Add `POST /activity-logs/sync` (or extend student-data) accepting `{ steps, sleepMinutes, heartRate, distanceKm, activeCalories, date? }`. Upsert into `activity_logs` for the authenticated student. | Enables 1.1 and keeps a single source of truth for “activity” in the app. |
| 1.3 | **Merge wearable + manual on Home** | On Home, if backend has no activity for today, show Health Connect data (steps, sleep, HR) as “from your wearable” and optionally offer “Sync to my profile” once. | User sees one coherent “today” story. |
| 1.4 | **Body mass (weight) on Wearable tab** | Health Connect already supports `BODY_MASS`; add permission + read + display in the metric grid (e.g. latest weight, trend). | Aligns with full health dashboards. |
| 1.5 | **Sleep stages (if available)** | Health Connect SleepSession can include stages (awake, light, deep, REM). Parse and show breakdown (e.g. “6h 20m – 45m deep, 1h 10m REM”). | Differentiates from apps that only show “total sleep”. |
| 1.6 | **Historical range sync** | Option to “Sync last 7/30 days” to backend so Trends and achievements have history even for new users. | Better first-time experience and accurate streaks. |

---

## 2. UX & UI (Wearable Tab)

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 2.1 | **Customizable step goal** | Let user set daily step goal (e.g. 8k, 10k, 12k). Steps hero card shows progress to that goal and “X% of your goal”. | Standard in top apps; increases engagement. |
| 2.2 | **Sleep goal** | Configurable target (e.g. 7h, 8h) and simple “Met goal” / “Under” indicator. | Reinforces sleep hygiene. |
| 2.3 | **Tap metric → detail** | Tapping a metric tile opens a detail screen: mini chart (e.g. last 7 days), source, last updated. | Power users expect drill-down. |
| 2.4 | **Steps / sleep / HR mini-charts** | On Wearable tab, small sparklines (e.g. last 7 days) under steps and sleep. Reuse existing chart components. | At-a-glance trend like Samsung/Apple Health. |
| 2.5 | **Source attribution** | Show “From Samsung Health” / “From Health Connect” / device name if available from Health Connect metadata. | Trust and clarity. |
| 2.6 | **Empty state illustrations** | When no data, show a simple illustration (e.g. watch + phone) and one CTA. | Feels polished and guides the user. |
| 2.7 | **Pull-to-refresh** | Add pull-to-refresh in addition to 30s auto-refresh. | Expected behavior on list/dashboard screens. |
| 2.8 | **Weight (body mass) tile** | Add tile for latest weight + optional 7-day trend; request READ_WEIGHT if not already. | Completes the “health snapshot”. |

---

## 3. Goals, Challenges & Motivation

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 3.1 | **Daily step challenge** | “Hit 10k today” with a checkmark and optional celebration when achieved. | Core engagement loop. |
| 3.2 | **Weekly step goal** | e.g. “50k this week” with progress bar and days left. | Encourages consistency. |
| 3.3 | **Sleep consistency** | “7+ hours, 5 nights this week” or “Same bedtime 3 nights”. | Sleep quality messaging. |
| 3.4 | **Streaks** | “3 days in a row meeting step goal”, “5 days good sleep”. Show on Wearable or Home. | Retention and habit formation. |
| 3.5 | **Achievements from wearable** | Unlock badges for “First 10k day”, “7-day step streak”, “Week of 7h+ sleep”, “100k steps in a month”. Backend already has achievements; wire rules to synced activity_logs. | Gamification that uses real data. |
| 3.6 | **Reminders** | “You’re at 8k steps – 2k to go!” or “Log sleep after you wake up” (if we add manual sleep log). | Gentle nudge without being annoying. |

---

## 4. Insights & Analytics

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 4.1 | **Weekly/Monthly summary** | “This week: 52k steps (↑12%), avg 6.5h sleep.” On Wearable or a “Summary” section. | High-level feedback. |
| 4.2 | **Steps vs mood/stress** | On Trends (or Wearable), show “Steps vs check-in mood/stress” when both are available (after 1.1 sync). | Unique value: link activity to wellbeing. |
| 4.3 | **Sleep vs next-day mood** | “After 7+ hours sleep, your next-day mood averaged X.” | Actionable insight. |
| 4.4 | **Resting heart rate trend** | If we have multiple HR readings, derive “resting HR” (e.g. overnight or morning) and show 7-day trend. | Common in fitness apps. |
| 4.5 | **Activity score / readiness** | Simple composite (e.g. steps + sleep + HR) as “Today’s activity score” or “Readiness” with a short tip. | One number that summarizes the day. |

---

## 5. Integration with Rest of App

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 5.1 | **Home: “Today from wearable”** | On Student Home, if we have Health Connect data (or synced activity), show steps/sleep/HR in the same cards that today use activityData, with a “Wearable” badge. | Single dashboard, not two silos. |
| 5.2 | **Trends: steps & sleep series** | In Student Trends, add optional “Steps” and “Sleep” series (from activity_logs after sync), with same date range and line charts. | Full picture over time. |
| 5.3 | **Check-in: prefill from wearable** | On check-in, optionally suggest “Your sleep last night: 6h 20m” or “Steps so far: 4,200” so the user can reference it. | Reduces effort, links check-in to behavior. |
| 5.4 | **Associate/Expert: student wearable summary** | In staff views, show “Last synced steps/sleep” per student (from activity_logs) so staff can reference activity in conversations. | Care team context. |
| 5.5 | **Hydration + wearable in one place** | Hydration is already on Home with local + API. Keep it there and add “Activity” (steps/sleep) from same data source after sync. | One “daily health” story. |

---

## 6. Backend & Care Team

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 6.1 | **activity_logs write API** | `POST /activity-logs` or `PUT /activity-logs/today` for app to upsert steps, sleep_hours, heart_rate, etc. by student + date. | Required for 1.1 and 5.x. |
| 6.2 | **FHIR Observations from activity** | When exporting FHIR, include activity_logs as Observations (e.g. steps, sleep, HR) if not already. fhirExport.js already maps activity_logs; ensure it’s used for all exports. | Interop and compliance. |
| 6.3 | **Last wearable sync timestamp** | Store `last_wearable_sync_at` per student (or in activity_logs) so UI and staff can show “Last synced: 2h ago”. | Trust and debugging. |
| 6.4 | **Privacy: sync opt-in** | Explicit “Sync my wearable data to my profile” toggle (and store consent). Only sync when enabled. | Compliance and trust. |

---

## 7. Reliability & Performance

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 7.1 | **Background sync (WorkManager)** | Optional background job (e.g. once per day) to read Health Connect and push to backend, so data is updated even if the user doesn’t open the Wearable tab. | Fresh data on Home/Trends without opening the tab. |
| 7.2 | **Offline / queue** | If sync to backend fails, queue payload and retry when online. | No lost data. |
| 7.3 | **Health Connect availability check** | On app start or when opening Wearable tab, re-check Health Connect status and show a gentle “Update Health Connect” or “Re-grant access” if needed. | Handles updates and permission resets. |
| 7.4 | **Reduce Toasts** | Keep only short, non-intrusive Toasts (e.g. “Synced” or “Steps: 8,234”); remove long instructional Toasts. | Already improved; keep it minimal. |

---

## 8. Onboarding & Education

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 8.1 | **First-time Wearable flow** | After onboarding, one clear path: “Connect your watch → Grant access → See your data.” Optional “Supported devices” (e.g. any watch that syncs to Health Connect). | Reduces drop-off. |
| 8.2 | **Supported brands** | Short list or link: “Works with Samsung, Fitbit, Garmin, Armitron, and any device that syncs to Health Connect.” | Sets expectations. |
| 8.3 | **Why we need access** | One screen or dialog: “We use steps and sleep to show your progress and link activity to your wellbeing. You can revoke access anytime in Health Connect.” | Trust and store policy. |

---

## 9. Future / Advanced

| # | Feature | Description | Why it matters |
|---|--------|-------------|----------------|
| 9.1 | **Wear OS companion** | Simple Wear OS tile or app that shows “Today: X steps” and optionally starts a check-in. | Differentiator for watch users. |
| 9.2 | **Apple Health (iOS)** | Implement HealthService for HealthKit (read steps, sleep, HR) and same sync-to-backend and UI as Android. | Doubles addressable market. |
| 9.3 | **Continuous HR / stress proxy** | If device provides continuous HR or HRV, show “Stress” or “Recovery” proxy (with caveats). | Premium feel. |
| 9.4 | **Notifications from wearable** | e.g. “You’ve been sedentary for 2 hours” or “Time to wind down for better sleep.” | Engagement and behavior change. |

---

## Suggested Priority Order

**Phase 1 – Unify data and core UX**  
1.1 Sync Health Connect → Backend  
1.2 Backend API for activity sync  
5.1 Home: “Today from wearable”  
2.1 Customizable step goal  
6.4 Privacy: sync opt-in  

**Phase 2 – Goals and engagement**  
3.1 Daily step challenge  
3.4 Streaks  
3.5 Achievements from wearable  
2.4 Mini-charts on Wearable tab  

**Phase 3 – Insights and polish**  
4.1 Weekly summary  
4.2 Steps vs mood/stress  
5.2 Trends: steps & sleep series  
2.3 Tap metric → detail  

**Phase 4 – Advanced**  
7.1 Background sync  
1.5 Sleep stages  
9.2 Apple Health (if building iOS)  

---

## Summary

To be **best-in-market**, the app should:

1. **Sync** wearable data to the backend (with consent) so Home, Trends, and staff see one story.  
2. **Goals & streaks** (steps, sleep) with clear progress and achievements.  
3. **Insights** that link activity to mood/stress and sleep to next-day wellbeing.  
4. **One dashboard** on Home that combines hydration, check-in, and wearable (steps/sleep/HR).  
5. **Reliability**: background sync, offline queue, and clear Health Connect status.  
6. **Privacy-first**: opt-in sync, short “why we need access” copy, and revocable permissions.

This roadmap is scoped so each item can be implemented incrementally without a full rewrite.
