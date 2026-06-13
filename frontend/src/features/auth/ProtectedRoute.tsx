import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../shared/auth/AuthProvider";

export function ProtectedRoute() {
  const { status } = useAuth();

  if (status === "loading") {
    return (
      <div className="app">
        <p className="muted">Loading…</p>
      </div>
    );
  }

  if (status === "unauthenticated") {
    return <Navigate to="/login" replace />;
  }

  return <Outlet />;
}
