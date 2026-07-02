# Ghid rapid de navigare prin cod

Acest document explică structura proiectului la nivel mare, ca să știi unde să cauți rapid când vrei să explici o funcționalitate sau să verifici o afirmație din lucrare.

## 1. Structura generală

```text
backend/    aplicația Spring Boot: API, securitate, servicii, modele, PostgreSQL, OpenSearch
frontend/   aplicația React + TypeScript: pagini, formulare, dashboard, grafice, search UI
docs/       documentație tehnică, thesis_final și ghiduri de proiect
bachelor_template/ template-ul și ghidul FMI pentru redactarea lucrării
```

Aplicația este împărțită pe feature-uri, nu într-un singur pachet mare. În backend, aproape fiecare modul are aceeași logică de organizare:

```text
domain/model          entități și enum-uri
application           servicii, reguli de business, comenzi, access checks
infrastructure        repository-uri, integrare cu DB/OpenSearch
web                   controllere REST și DTO-uri
```

Nu toate pachetele au exact toate aceste directoare, dar acesta este modelul mental corect.

## 2. Backend

Rădăcina backend-ului este:

```text
backend/src/main/java/com/thesis/workout
```

Cele mai importante pachete sunt:

| Pachet | Ce conține | Unde te uiți prima dată |
|---|---|---|
| `auth` | register/login, JWT, refresh token, BCrypt, security config | `auth/application/AuthService.java`, `auth/infrastructure/security/SecurityConfig.java`, `JwtService.java` |
| `exercise` | exerciții oficiale/custom și grupe musculare | `ExerciseService.java`, `Exercise.java`, `ExerciseRepository.java` |
| `routine` | rutine de warm-up/cool-down | `RoutineService.java`, `Routine.java` |
| `gym` | săli și echipamente | `GymService.java`, `EquipmentService.java` |
| `template` | programe de antrenament, zile, exerciții planificate | `TemplateService.java`, `TemplateDayService.java`, `TemplateDayExerciseService.java` |
| `session` | pornire/finish/cancel workout, snapshot-uri, seturi | `WorkoutSessionService.java`, `WorkoutSetService.java`, `WorkoutSession.java`, `SessionExercise.java` |
| `history` | lista de istoric | `HistoryService.java`, `HistorySessionRepository.java` |
| `analytics` | statistici/progres calculate din sesiuni finalizate | `AnalyticsService.java`, `AnalyticsQueryRepository.java` |
| `marketplace` | publish/unpublish, vote/save/use template | `MarketplaceQueryService.java`, `TemplateInteractionService.java`, `TemplateCopyService.java` |
| `analyzer` | analizator determinist pentru structura programelor | `TemplateAnalyzerService.java` |
| `coaching` | coach-client, invitații, ReBAC, acces la istoric/client | `CoachAccess.java`, `CoachViewService.java`, `CoachSearchService.java` |
| `search` | OpenSearch: indexuri, documente, query-uri, rebuild | `TemplateSearchService.java`, `WorkoutSearchService.java`, `SearchRebuildService.java` |
| `benchmark` | comparația OpenSearch vs PostgreSQL | `SearchBenchmarkRunner.java`, `PostgresSearchBaseline.java`, `OpenSearchBenchmark.java` |
| `demo` | date demo pentru prezentare | `DemoDataSeeder.java` |
| `shared` | erori comune, paginare, handler global | `GlobalExceptionHandler.java`, `ApiException.java`, `PagedResponse.java` |

## 3. Baza de date

Migrațiile sunt aici:

```text
backend/src/main/resources/db/migration
```

Fișierele importante:

```text
V1__full_initial_schema.sql
V2__seed_official_exercises.sql
V3__one_active_workout_per_user.sql
```

Regula principală de ținut minte:

```text
PostgreSQL este sursa de adevăr.
Flyway creează schema.
Hibernate doar validează schema, nu o generează.
```

Dacă vrei să verifici o tabelă, caută în `V1__full_initial_schema.sql`. Pentru seed-ul de exerciții oficiale, caută în `V2__seed_official_exercises.sql`. Pentru regula de o singură sesiune activă per user, uită-te în `V3__one_active_workout_per_user.sql`.

## 4. Fluxuri principale

### Register/Login

Pornește de aici:

```text
auth/web/AuthController.java
auth/application/AuthService.java
auth/infrastructure/security/JwtService.java
auth/web/RefreshCookieFactory.java
```

Idei importante:

- parolele sunt hash-uite cu BCrypt;
- access token-ul este JWT;
- refresh token-ul este opac, stocat ca SHA-256 fingerprint;
- refresh token-ul este în cookie HttpOnly;
- serverul este stateless.

### Template/program

Pornește de aici:

```text
template/web/TemplateController.java
template/application/TemplateService.java
template/application/TemplateDayService.java
template/application/TemplateDayExerciseService.java
```

Un template este programul reutilizabil. Are zile (`template_days`), exerciții planificate (`template_day_exercises`) și rutine atașate zilelor (`template_day_routines`).

### Pornire workout și snapshot-uri

Pornește de aici:

```text
session/web/WorkoutSessionController.java
session/application/WorkoutSessionService.java
session/domain/model/WorkoutSession.java
session/domain/model/SessionExercise.java
session/domain/model/SessionRoutine.java
session/domain/model/WorkoutSet.java
```

La `start`, aplicația copiază datele relevante din template/gym/exerciții în tabelele de sesiune:

```text
workout_sessions
session_exercises
session_exercise_muscle_groups
session_routines
workout_sets
```

Aceasta este partea de snapshot: istoricul păstrează string-uri și valori copiate la momentul sesiunii, nu citește live din template.

### Istoric și analytics

Istoricul simplu:

```text
history/application/HistoryService.java
history/infrastructure/repository/HistorySessionRepository.java
```

Statisticile:

```text
analytics/application/AnalyticsService.java
analytics/infrastructure/repository/AnalyticsQueryRepository.java
frontend/src/features/analytics/AnalyticsView.tsx
```

Analytics calculează live din PostgreSQL, doar peste sesiuni `FINISHED`.

### Marketplace

Pornește de aici:

```text
marketplace/web/MarketplaceController.java
marketplace/web/TemplatePublishController.java
marketplace/application/MarketplaceQueryService.java
marketplace/application/TemplatePublishingService.java
marketplace/application/TemplateInteractionService.java
marketplace/application/TemplateCopyService.java
```

Ce face:

- publicare/unpublicare template;
- browse marketplace;
- upvote/downvote;
- save/unsave;
- use/copy template;
- deep copy pentru template-uri publice.

Statisticile marketplace sunt în `template_stats`.

### Analizator

Pornește de aici:

```text
analyzer/web/AnalyzerController.java
analyzer/application/TemplateAnalyzerService.java
frontend/src/features/analyzer/TemplateAnalysisPanel.tsx
```

Analizatorul este determinist, bazat pe reguli. Nu este AI și nu oferă recomandări medicale.

### OpenSearch

Pornește de aici:

```text
search/application/TemplateSearchService.java
search/application/WorkoutSearchService.java
search/application/TemplateDocumentAssembler.java
search/application/WorkoutSessionDocumentAssembler.java
search/infrastructure/TemplateIndexer.java
search/infrastructure/WorkoutSessionIndexer.java
search/infrastructure/event/SearchIndexEventListener.java
search/application/SearchRebuildService.java
```

Mapping-urile sunt aici:

```text
backend/src/main/resources/search/templates_v1.json
backend/src/main/resources/search/workout_sessions_v1.json
```

Ideea importantă:

```text
OpenSearch este read model derivat.
PostgreSQL rămâne sursa de adevăr.
Rezultatele sunt revalidate în PostgreSQL.
```

### Coaching

Pornește de aici:

```text
coaching/web/CoachController.java
coaching/web/CoachingController.java
coaching/application/CoachAccess.java
coaching/application/CoachViewService.java
coaching/application/CoachClientService.java
coaching/application/RelationshipService.java
```

Ideea importantă:

- RBAC: `/api/coach/**` cere rol `COACH`;
- ReBAC: coach-ul vede clientul doar dacă există relație `ACTIVE`;
- coach-ul citește datele clientului, dar nu le modifică.

## 5. Frontend

Rădăcina frontend-ului este:

```text
frontend/src
```

Paginile sunt organizate pe feature-uri:

| Feature | Ce conține |
|---|---|
| `auth` | login/register/protected route |
| `dashboard` | pagina principală |
| `exercises` | CRUD exerciții |
| `routines` | CRUD rutine |
| `gyms` | săli și echipamente |
| `templates` | lista de programe și builder-ul |
| `workouts` | start workout, live workout, summary |
| `history` | istoric + search în istoric |
| `analytics` | grafice Recharts |
| `marketplace` | marketplace browse/search/detail |
| `analyzer` | panel analizator în template builder |
| `coaching` | coach/client pages |
| `search` | componente comune pentru OpenSearch UI |

Fișiere utile:

```text
frontend/src/App.tsx
frontend/src/shared/api/client.ts
frontend/src/shared/auth/AuthProvider.tsx
frontend/src/shared/theme/ThemeProvider.tsx
frontend/src/shared/ui/ToastProvider.tsx
frontend/src/index.css
```

`App.tsx` îți arată rutele principale. Dacă vrei să vezi cum ajungi la o pagină, începe de acolo.

## 6. Cum urmărești un request cap-coadă

Exemplu pentru analytics:

```text
frontend/src/features/analytics/api.ts
frontend/src/features/analytics/AnalyticsPage.tsx
frontend/src/features/analytics/AnalyticsView.tsx
backend/.../analytics/web/AnalyticsController.java
backend/.../analytics/application/AnalyticsService.java
backend/.../analytics/infrastructure/repository/AnalyticsQueryRepository.java
```

Exemplu pentru start workout:

```text
frontend/src/features/workouts/StartWorkoutPage.tsx
frontend/src/features/workouts/api.ts
backend/.../session/web/WorkoutSessionController.java
backend/.../session/application/WorkoutSessionService.java
backend/.../session/domain/model/*
```

Exemplu pentru marketplace use:

```text
frontend/src/features/marketplace/MarketplacePage.tsx
frontend/src/features/marketplace/api.ts
backend/.../marketplace/web/MarketplaceController.java
backend/.../marketplace/application/TemplateCopyService.java
backend/.../template/application/TemplateService.java
```

Exemplu pentru OpenSearch:

```text
frontend/src/features/search/api.ts
frontend/src/features/search/SearchFacets.tsx
backend/.../search/web/SearchController.java
backend/.../search/application/TemplateSearchService.java
backend/.../search/application/WorkoutSearchService.java
backend/.../search/infrastructure/*
```

## 7. Documentație tehnică deja existentă

Documentele utile din `docs`:

| Document | Pentru ce îl folosești |
|---|---|
| `ARCHITECTURE.md` | vedere de ansamblu |
| `DATABASE_SCHEMA.md` | baza de date |
| `SECURITY_MODEL.md` | securitate |
| `SEARCH_FEATURE.md` | OpenSearch complet |
| `SEARCH_BENCHMARK.md` | benchmark OpenSearch vs PostgreSQL |
| `ROUTINE_ANALYZER.md` | analizator |
| `COACH_MODE.md` | coaching |
| `API_CONTRACT.md` | endpoint-uri |
| `THESIS_HIGHLIGHTS.md` | idei bune pentru prezentare |
| `THESIS_NOTES.md` | notițe ample despre implementare |

## 8. Ce fișiere să deschizi dacă ești întrebat la prezentare

| Întrebare | Deschide |
|---|---|
| Cum funcționează JWT/refresh token? | `AuthService.java`, `JwtService.java`, `RefreshCookieFactory.java` |
| Cum previi IDOR? | `TemplateAccess.java`, `WorkoutSessionAccess.java`, `CoachAccess.java` |
| Cum funcționează snapshot-urile? | `WorkoutSessionService.java`, `SessionExercise.java`, `WorkoutSession.java`, migrarea V1 |
| Cum se calculează statisticile? | `AnalyticsQueryRepository.java`, `AnalyticsService.java` |
| Cum funcționează OpenSearch? | `TemplateSearchService.java`, `WorkoutSearchService.java`, `SearchIndexEventListener.java` |
| Cum se reconstruiește indexul? | `SearchRebuildService.java`, `OpenSearchAdmin.java` |
| Cum funcționează marketplace copy? | `TemplateCopyService.java` |
| Cum funcționează coach-client? | `CoachAccess.java`, `CoachViewService.java`, `RelationshipService.java` |
| Unde sunt regulile analizatorului? | `TemplateAnalyzerService.java` |
| Unde sunt endpoint-urile? | pachetele `web` din fiecare feature |

## 9. Comenzi utile

Build backend:

```powershell
cd backend
mvn test
```

Build frontend:

```powershell
cd frontend
npm run build
```

Căutare rapidă în cod:

```powershell
rg "WorkoutSessionService"
rg "TemplateIndexEvent"
rg "refresh token"
rg "ownerUserId"
```

## 10. Ideea de ansamblu

Proiectul nu este doar un CRUD. Arhitectura este construită în jurul câtorva idei:

- PostgreSQL este sursa de adevăr.
- Flyway controlează schema.
- Backend-ul nu are încredere în `userId` primit de la frontend.
- Sesiunile folosesc snapshot-uri pentru istoric corect.
- JWT + refresh token + BCrypt + RBAC/ReBAC susțin securitatea.
- OpenSearch este un read model derivat pentru search avansat.
- React-ul este organizat pe feature-uri, cu API client comun și componente reutilizabile.

