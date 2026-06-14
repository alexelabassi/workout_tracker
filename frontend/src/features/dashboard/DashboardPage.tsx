import { Link } from "react-router-dom";
import { HealthCard } from "../health/HealthCard";
import { useAuth } from "../../shared/auth/AuthProvider";
import { useTheme } from "../../shared/theme/ThemeProvider";

export function DashboardPage() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  const greetingName = user?.displayName?.trim() || user?.email || "athlete";

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Workout Platform</h1>
          <p className="muted">Signed in as {greetingName}</p>
        </div>
        <div className="app__actions">
          <button type="button" className="button button--ghost" onClick={toggleTheme}>
            {theme === "dark" ? "Light mode" : "Dark mode"}
          </button>
          <button type="button" className="button" onClick={() => void logout()}>
            Log out
          </button>
        </div>
      </header>

      <main className="app__main">
        <section className="card">
          <header className="card__header">
            <h2>Your account</h2>
            {user && <span className="badge">{user.role}</span>}
          </header>
          {user && (
            <dl className="details">
              <div>
                <dt className="muted">Email</dt>
                <dd>{user.email}</dd>
              </div>
              <div>
                <dt className="muted">Display name</dt>
                <dd>{user.displayName ?? "—"}</dd>
              </div>
            </dl>
          )}
        </section>

        <section className="card">
          <header className="card__header">
            <h2>Planning data</h2>
          </header>
          <p className="muted">Build your exercise library from the official catalog or your own movements.</p>
          <Link to="/exercises" className="button button--block">
            Manage exercises
          </Link>
          <Link to="/routines" className="button button--block">
            Manage routines
          </Link>
        </section>

        <HealthCard />
      </main>

      <footer className="app__footer muted">Phase 2 — Authentication ready</footer>
    </div>
  );
}
