import { HealthCard } from "./features/health/HealthCard";
import { useTheme } from "./shared/theme/ThemeProvider";

export default function App() {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Workout Platform</h1>
          <p className="muted">
            Evidence-Informed Workout Template Discovery and Training History
          </p>
        </div>
        <button type="button" className="button" onClick={toggleTheme}>
          {theme === "dark" ? "Light mode" : "Dark mode"}
        </button>
      </header>

      <main className="app__main">
        <HealthCard />
      </main>

      <footer className="app__footer muted">Phase 1 — Infrastructure ready</footer>
    </div>
  );
}
