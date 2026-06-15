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
          <Link to="/gyms" className="button button--block">
            Manage gyms
          </Link>
          <Link to="/templates" className="button button--block">
            Manage templates
          </Link>
          <Link to="/marketplace" className="button button--block button--ghost">
            Browse marketplace
          </Link>
        </section>

        <section className="card">
          <header className="card__header">
            <h2>Train</h2>
          </header>
          <p className="muted">Start a workout from a template day, or resume one in progress.</p>
          <Link to="/workouts/start" className="button button--block">
            Start workout
          </Link>
          <Link to="/workouts/live" className="button button--block button--ghost">
            Resume active workout
          </Link>
        </section>

        <section className="card">
          <header className="card__header">
            <h2>Review</h2>
          </header>
          <p className="muted">Look back at logged workouts and track your progress over time.</p>
          <Link to="/history" className="button button--block">
            Workout history
          </Link>
          <Link to="/analytics" className="button button--block button--ghost">
            Progress &amp; analytics
          </Link>
        </section>

        <section className="card">
          <header className="card__header">
            <h2>Coaching</h2>
            {user?.role === "COACH" && <span className="badge">Coach</span>}
          </header>
          <p className="muted">
            {user?.role === "COACH"
              ? "Manage your clients and review their training, or manage coaches for your own account."
              : "Accept a coach's invite to share your training, or manage your active coaches."}
          </p>
          {user?.role === "COACH" && (
            <Link to="/coach" className="button button--block">
              Coach area
            </Link>
          )}
          <Link to="/coaching" className="button button--block button--ghost">
            Manage invites &amp; coaches
          </Link>
        </section>

        <HealthCard />
      </main>

      <footer className="app__footer muted">Phase 2 — Authentication ready</footer>
    </div>
  );
}
