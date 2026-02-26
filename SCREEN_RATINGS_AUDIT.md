# Screen & Element Ratings Audit (Target: 10/10 Everywhere)

Each screen and key elements are rated 1–10. Gaps are listed so every item can be brought to 10/10.

**✅ First polish pass applied:** TopAppBar colors (titleContentColor, navigationIconContentColor, actionIconContentColor = TextPrimary) on all screens; contentDescription on all nav and action icons; backgrounds standardized to SurfaceLight; loading indicators use CalmBlue and 48.dp; shared components (EmptyStateCard, LoadingFullScreen, ErrorBanner) added in `ui/components/SharedComponents.kt`. Re-run audit after further UX passes (pull-to-refresh, explicit card text colors everywhere) to confirm 10/10.

---

## Rating criteria (per element)

- **TopAppBar:** titleContentColor + action/navigation icon colors explicit (TextPrimary on light); 10 = perfect.
- **Cards:** containerColor (White/SurfaceLight), shape 16–24dp, text TextPrimary/TextSecondary; 10 = consistent.
- **Buttons:** CalmBlue/theme, shape; 10 = consistent.
- **Icons:** contentDescription set for accessibility; 10 = all described.
- **Empty state:** icon + title + subtitle + optional CTA; 10 = clear, actionable.
- **Loading:** CalmBlue indicator, centered; 10 = consistent.
- **Error:** message + retry; 10 = clear, actionable.
- **Typography:** explicit colors, no invisible text; 10 = readable everywhere.
- **Spacing:** consistent padding (16–24dp); 10 = uniform.
- **Overall:** 10 = picture-perfect, store-ready.

---

## Auth & common

| Screen | TopAppBar | Cards | Buttons | Icons | Empty | Loading | Error | Typography | Spacing | Overall |
|--------|-----------|-------|---------|-------|-------|--------|-------|------------|---------|---------|
| **LoginScreen** | N/A | 9 | 9 | 8 | N/A | 8 | 8 | 9 | 9 | **8.5** |
| **SignupScreen** | N/A | 9 | 9 | 8 | N/A | 8 | 8 | 9 | 9 | **8.5** |
| **CommunityResourcesScreen** | 9 | 8 | 8 | 6 | 7 | N/A | N/A | 8 | 8 | **8** |
| **WearableSyncScreen** | 10 | 9 | 9 | 7 | 10 | 9 | 8 | 9 | 9 | **9** |
| **HealthOnboardingScreen** | 8 | 8 | 8 | 6 | N/A | N/A | N/A | 8 | 8 | **8** |

**Fixes:** Login/Signup: contentDescription on icons; loading alpha. Community: TopAppBar icon colors; contentDescription; FilterChip selected colors. Wearable: contentDescription on icons. Health onboarding: TopAppBar + contentDescription.

---

## Student

| Screen | TopAppBar | Cards | Buttons | Icons | Empty | Loading | Error | Typography | Spacing | Overall |
|--------|-----------|-------|---------|-------|-------|--------|-------|------------|---------|---------|
| **StudentApp** (nav) | N/A | N/A | N/A | 5 | N/A | N/A | N/A | 8 | 9 | **7** |
| **StudentHome** | 9 | 9 | 9 | 5 | 8 | 8 | 8 | 9 | 9 | **8.5** |
| **StudentCheckIn** | 10 | 10 | 10 | 6 | N/A | 9 | N/A | 10 | 10 | **9** |
| **StudentTrends** | 9 | 10 | 9 | 7 | 9 | 9 | N/A | 10 | 10 | **9** |
| **StudentAppointments** | 10 | 10 | 10 | 5 | 10 | 9 | 7 | 10 | 10 | **9** |
| **StudentMessages** | 9 | 9 | 9 | 7 | 9 | 9 | 9 | 9 | 9 | **9** |
| **StudentSettings** | 8 | 9 | 9 | 6 | N/A | 8 | 8 | 9 | 9 | **8.5** |
| **StudentAchievements** | 7 | 9 | 9 | 5 | 8 | 8 | 7 | 9 | 9 | **8** |
| **StudentStaff** | 6 | 8 | 8 | 5 | 8 | 9 | 7 | 8 | 9 | **8** |

**Fixes:** StudentApp: contentDescription on all nav icons. Home: Menu/Notifications/Person contentDescription. CheckIn: contentDescription on Menu/Person. Appointments: Refresh/Add contentDescription. Messages: ensure all icons described. Settings: TopAppBar colors + contentDescription. Achievements: TopAppBar titleContentColor + actionIconContentColor + Refresh contentDescription. Staff: TopAppBar (add one if missing) + contentDescription; loading color CalmBlue; empty/error use shared components.

---

## Parent

| Screen | TopAppBar | Cards | Buttons | Icons | Empty | Loading | Error | Typography | Spacing | Overall |
|--------|-----------|-------|---------|-------|-------|--------|-------|------------|---------|---------|
| **ParentApp** (nav) | N/A | N/A | N/A | 5 | N/A | N/A | N/A | 8 | 9 | **7** |
| **ParentHome** | 6 | 9 | 9 | 5 | 7 | 7 | 6 | 8 | 9 | **7.5** |
| **ParentMessages** | 6 | 9 | 9 | 5 | 7 | 7 | 6 | 8 | 9 | **7.5** |
| **ParentWellness** | 6 | 8 | 8 | 5 | 7 | 7 | 6 | 8 | 9 | **7.5** |
| **ParentSettings** | 9 | 9 | 9 | 6 | N/A | 8 | 8 | 9 | 9 | **8.5** |

**Fixes:** ParentApp: contentDescription on nav icons. Home/Messages/Wellness: titleContentColor + actionIconContentColor + navigationIconContentColor = TextPrimary; contentDescription on Menu/Person; loading color = CalmBlue; background SurfaceLight; empty/error states polished.

---

## Associate

| Screen | TopAppBar | Cards | Buttons | Icons | Empty | Loading | Error | Typography | Spacing | Overall |
|--------|-----------|-------|---------|-------|-------|--------|-------|------------|---------|---------|
| **AssociateApp** (nav) | N/A | N/A | N/A | 5 | N/A | N/A | N/A | 8 | 9 | **7** |
| **AssociateRequests** | 6 | 9 | 9 | 5 | 9 | 9 | 7 | 8 | 9 | **8** |
| **AssociateStudents** | 6 | 9 | 9 | 5 | 8 | 9 | 7 | 8 | 9 | **8** |
| **AssociateCommunications** | 6 | 9 | 9 | 5 | 8 | 9 | 8 | 8 | 9 | **8** |
| **AssociateAlerts** | 6 | 8 | 8 | 5 | 8 | 8 | 7 | 8 | 9 | **7.5** |
| **AssociateAnalytics** | 8 | 9 | 9 | 6 | 7 | 8 | 7 | 8 | 9 | **8** |
| **AssociateScreeners** | 6 | 8 | 8 | 5 | 7 | 8 | 7 | 8 | 9 | **7.5** |
| **AssociateSettings** | 9 | 9 | 9 | 6 | N/A | 8 | 8 | 9 | 9 | **8.5** |

**Fixes:** AssociateApp: contentDescription on nav icons. All Associate screens: TopAppBar titleContentColor + actionIconContentColor + navigationIconContentColor; contentDescription on all icons; background SurfaceLight; loading CalmBlue; empty/error consistent.

---

## Expert

| Screen | TopAppBar | Cards | Buttons | Icons | Empty | Loading | Error | Typography | Spacing | Overall |
|--------|-----------|-------|---------|-------|-------|--------|-------|------------|---------|---------|
| **ExpertApp** (nav) | N/A | N/A | N/A | 5 | N/A | N/A | N/A | 8 | 9 | **7** |
| **ExpertDashboard** | 6 | 9 | 9 | 5 | 7 | 7 | 6 | 8 | 9 | **7.5** |
| **ExpertReports** | 6 | 8 | 8 | 5 | 7 | 8 | 7 | 8 | 9 | **7.5** |
| **ExpertStudents** | 6 | 9 | 9 | 5 | 8 | 9 | 7 | 8 | 9 | **8** |
| **ExpertSettings** | 9 | 9 | 9 | 6 | N/A | 8 | 8 | 9 | 9 | **8.5** |

**Fixes:** ExpertApp: contentDescription on nav icons. Dashboard/Reports/Students: TopAppBar colors; contentDescription; loading CalmBlue; background SurfaceLight; empty/error polished.

---

## Implementation checklist (to reach 10/10)

1. **Shared components:** EmptyStateCard, LoadingFullScreen, ErrorBanner (and use where applicable).
2. **TopAppBar (all screens):** containerColor, titleContentColor = TextPrimary, navigationIconContentColor = TextPrimary, actionIconContentColor = TextPrimary.
3. **Icons:** Replace every `Icon(..., null)` with a meaningful contentDescription.
4. **Loading:** CircularProgressIndicator(color = CalmBlue), size 48.dp where full-screen.
5. **Backgrounds:** Use SurfaceLight or BackgroundLight instead of Color(0xFFF5F5F5).
6. **Cards:** Explicit text colors (TextPrimary/TextSecondary) on all card content.
7. **Empty/Error:** Use shared EmptyStateCard and ErrorBanner; add retry where applicable.
8. **Navigation bars (Student/Parent/Associate/Expert App):** contentDescription on every nav icon.

After these changes, re-rate; target is 10/10 for every row and column.
