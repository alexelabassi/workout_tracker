import { Navigate, Route, Routes } from "react-router-dom";
import { LoginPage } from "./features/auth/LoginPage";
import { RegisterPage } from "./features/auth/RegisterPage";
import { ProtectedRoute } from "./features/auth/ProtectedRoute";
import { DashboardPage } from "./features/dashboard/DashboardPage";
import { ExercisesPage } from "./features/exercises/ExercisesPage";
import { RoutinesPage } from "./features/routines/RoutinesPage";
import { GymsPage } from "./features/gyms/GymsPage";
import { GymEquipmentPage } from "./features/gyms/GymEquipmentPage";
import { TemplatesPage } from "./features/templates/TemplatesPage";
import { TemplateBuilderPage } from "./features/templates/TemplateBuilderPage";
import { StartWorkoutPage } from "./features/workouts/StartWorkoutPage";
import { LiveWorkoutPage } from "./features/workouts/LiveWorkoutPage";
import { WorkoutSummaryPage } from "./features/workouts/WorkoutSummaryPage";
import { HistoryPage } from "./features/history/HistoryPage";
import { AnalyticsPage } from "./features/analytics/AnalyticsPage";
import { MarketplacePage } from "./features/marketplace/MarketplacePage";
import { MarketplaceDetailPage } from "./features/marketplace/MarketplaceDetailPage";
import { CoachingPage } from "./features/coaching/CoachingPage";
import { CoachClientsPage } from "./features/coaching/CoachClientsPage";
import { CoachClientDetailPage } from "./features/coaching/CoachClientDetailPage";
import { CoachClientSessionPage } from "./features/coaching/CoachClientSessionPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/exercises" element={<ExercisesPage />} />
        <Route path="/routines" element={<RoutinesPage />} />
        <Route path="/gyms" element={<GymsPage />} />
        <Route path="/gyms/:gymId" element={<GymEquipmentPage />} />
        <Route path="/templates" element={<TemplatesPage />} />
        <Route path="/templates/:templateId" element={<TemplateBuilderPage />} />
        <Route path="/workouts/start" element={<StartWorkoutPage />} />
        <Route path="/workouts/live" element={<LiveWorkoutPage />} />
        <Route path="/workouts/:sessionId" element={<WorkoutSummaryPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/analytics" element={<AnalyticsPage />} />
        <Route path="/marketplace" element={<MarketplacePage />} />
        <Route path="/marketplace/:templateId" element={<MarketplaceDetailPage />} />
        <Route path="/coaching" element={<CoachingPage />} />
        <Route path="/coach" element={<CoachClientsPage />} />
        <Route path="/coach/clients/:clientId" element={<CoachClientDetailPage />} />
        <Route path="/coach/clients/:clientId/sessions/:sessionId" element={<CoachClientSessionPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
