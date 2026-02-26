# Development Roadmap: All Screens Equally

This doc outlines improvements to apply **equally across all roles** (Student, Parent, Associate, Expert) so every screen feels polished and store-ready.

---

## 1. Already Done

- **StudentTrends.kt** – Deprecation fix: `Icons.Filled.ShowChart` → `Icons.AutoMirrored.Filled.ShowChart`.
- **Student:** Check-in, Trends, Appointments, Home (water persistence), Wearable sync, Achievements – colors and polish.
- **Shared:** Wearable roadmap, Health onboarding, activity sync API.

---

## 2. Cross-Screen Standards (Apply Everywhere)

### 2.1 TopAppBar

- **Title:** `titleContentColor = TextPrimary` (or theme inverse on colored bars).
- **Icons:** Tint = `TextPrimary` or `Color.White` when on colored background.
- **Back:** Same behavior and icon on all secondary screens.

**Screens to align:** Any screen that still uses default title/icon colors. Audit: ParentHome, ParentWellness, ParentMessages, AssociateStudents, AssociateAlerts, AssociateScreeners, AssociateCommunications, ExpertDashboard, ExpertReports, ExpertStudents, CommunityResourcesScreen, LoginScreen, SignupScreen.

### 2.2 Empty States

- **Pattern:** One shared component (e.g. `EmptyStateCard`) with:
  - Icon (64.dp, tint TextSecondary)
  - Title (titleMedium, TextPrimary)
  - Subtitle (bodyMedium, TextSecondary)
  - Optional primary button (CalmBlue).
- **Use on:** Every list/dashboard that can be empty (messages, requests, students, appointments, communications, alerts, reports, etc.).

### 2.3 Loading States

- **Pattern:** Full-width or centered `CircularProgressIndicator(color = CalmBlue)` while `isLoading && items.isEmpty()`.
- **Consistency:** Same size (e.g. 48.dp) and padding across Student, Parent, Associate, Expert.

### 2.4 Error States

- **Pattern:** Card or banner with StatusUrgent (or StatusWarning) tint; message + “Retry” action where applicable.
- **Use on:** Every screen that loads data from API (messages, appointments, students, communications, alerts, reports, etc.).

### 2.5 Pull-to-Refresh

- **Where:** Any list or dashboard that fetches fresh data (Messages, Appointments, Students, Requests, Communications, Alerts, Reports, Home dashboards).
- **Style:** Same indicator color (CalmBlue) and `onRefresh` semantics across roles.

### 2.6 Cards and Typography

- **Cards:** Prefer `containerColor = Color.White` or `SurfaceLight`; `RoundedCornerShape(16.dp)` or 20.dp; explicit `TextPrimary`/`TextSecondary` for text.
- **Typography:** Use theme `MaterialTheme.typography` with explicit colors so all screens look good in light/dark and on Samsung/Apple.

### 2.7 Accessibility

- **Icons:** `contentDescription` for every decorative or action icon.
- **Touch targets:** Min 48.dp for tappable areas.

---

## 3. Shared Components to Add

| Component           | Purpose                                      |
|---------------------|----------------------------------------------|
| `EmptyStateCard`    | Icon + title + subtitle + optional button   |
| `LoadingFullScreen` | Centered CalmBlue progress                   |
| `ErrorBanner`       | Error message + retry                        |
| `PullRefreshBox`    | Wrapper for pull-to-refresh + content        |

Put these in e.g. `ui/components/` and use from all roles.

---

## 4. Role-by-Role “More Development” Ideas

### Student

- **Home:** Streak / engagement summary card; quick actions (Check-in, Book appointment).
- **Messages:** Read/unread states; reply from list.
- **Staff:** Staff roles and availability hints.
- **Achievements:** Categories and progress bars already improved; add share or celebrate animation.

### Parent

- **Home:** Summary cards (alerts, upcoming, wellness tip); same card style as Student.
- **Messages:** Same empty/loading/error and pull-to-refresh as Student Messages.
- **Wellness:** Simple trends or history if data exists.

### Associate

- **Requests:** Filters (date, type, status); same empty/loading/error.
- **Students:** Search and filters; consistent cards.
- **Communications / Alerts / Analytics:** Same loading/error and pull-to-refresh; consistent TopAppBar and cards.

### Expert

- **Dashboard:** Key metrics cards; same empty/loading as others.
- **Reports / Students:** Same list patterns, empty states, and pull-to-refresh.

### Common

- **Community Resources:** Same TopAppBar and card style; optional categories/filters.
- **Wearable / Health onboarding:** Already improved; keep consistent with theme.

---

## 5. Suggested Implementation Order

1. **Add shared components** – `EmptyStateCard`, `LoadingFullScreen`, `ErrorBanner`, `PullRefreshBox`.
2. **TopAppBar pass** – One pass to set `titleContentColor` and icon tints on all screens.
3. **Empty / loading / error pass** – Replace ad-hoc implementations with shared components.
4. **Pull-to-refresh** – Add where lists or dashboards load data.
5. **Cards and typography** – Sweep for `Color.White`/`SurfaceLight` and TextPrimary/TextSecondary.
6. **Accessibility** – contentDescription and touch targets.
7. **Feature parity** – Add the “more development” items per role (e.g. summary cards, filters, read states) so each role feels equally developed.

---

## 6. Files to Touch (by area)

- **Theme/colors:** `Color.kt`, `Theme.kt`.
- **Shared UI:** New or existing in `ui/components/`.
- **Student:** StudentHome, StudentMessages, StudentAppointments, StudentTrends, StudentCheckIn, StudentStaff, StudentAchievements, StudentSettings.
- **Parent:** ParentHome, ParentMessages, ParentWellness, ParentSettings.
- **Associate:** AssociateRequests, AssociateStudents, AssociateCommunications, AssociateAlerts, AssociateAnalytics, AssociateScreeners, AssociateSettings.
- **Expert:** ExpertDashboard, ExpertReports, ExpertStudents, ExpertSettings.
- **Common:** CommunityResourcesScreen, WearableSyncScreen, HealthOnboardingScreen.
- **Auth:** LoginScreen, SignupScreen.

Using this order keeps the codebase consistent and makes “all aspects developed more on all screens equally” systematic rather than one-off.
