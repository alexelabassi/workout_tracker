import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ApiError } from "../../shared/api/client";
import { fetchAnalyticsOverview } from "./api";
import type { AnalyticsOverview, OneRepMaxSeries } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; data: AnalyticsOverview };

const axisTick = { fill: "var(--muted)", fontSize: 12 };
const tooltipStyle = {
  background: "var(--surface)",
  border: "1px solid var(--border)",
  borderRadius: 8,
  color: "var(--text)",
};
const SERIES_COLORS = [
  "#4f8cff", "#2fbf71", "#ef4d5a", "#f5a623", "#a06bff",
  "#16c2c2", "#ff77a8", "#7bd13f", "#d98cff", "#ffc24b",
];
// How many exercises to plot by default (the rest start hidden to keep the chart readable).
const DEFAULT_VISIBLE_SERIES = 5;

/** Stable color for an exercise, keyed by its position in the full series list (so toggling never shuffles colors). */
function colorFor(exerciseName: string, allSeries: OneRepMaxSeries[]): string {
  const index = allSeries.findIndex((series) => series.exerciseName === exerciseName);
  return SERIES_COLORS[(index < 0 ? 0 : index) % SERIES_COLORS.length];
}

/** Pivots per-exercise 1RM series into one row per date keyed by exercise name (for a multi-line chart). */
function pivotOneRepMax(series: OneRepMaxSeries[]): Record<string, number | string>[] {
  const byDate = new Map<string, Record<string, number | string>>();
  for (const exercise of series) {
    for (const point of exercise.points) {
      const row = byDate.get(point.date) ?? { date: point.date };
      row[exercise.exerciseName] = point.estimatedOneRepMax;
      byDate.set(point.date, row);
    }
  }
  return Array.from(byDate.values()).sort((a, b) => String(a.date).localeCompare(String(b.date)));
}

export function AnalyticsPage() {
  const [state, setState] = useState<LoadState>({ kind: "loading" });
  const [selectedExercises, setSelectedExercises] = useState<string[]>([]);

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const data = await fetchAnalyticsOverview();
      setSelectedExercises(
        data.oneRepMaxOverTime.slice(0, DEFAULT_VISIBLE_SERIES).map((series) => series.exerciseName),
      );
      setState({ kind: "ready", data });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load analytics." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const toggleExercise = (name: string) =>
    setSelectedExercises((current) =>
      current.includes(name) ? current.filter((item) => item !== name) : [...current, name],
    );

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Progress</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
        <Link to="/history" className="button button--ghost">
          View history
        </Link>
      </header>

      <main className="app__main">
        {state.kind === "loading" && (
          <section className="card">
            <p className="muted">Loading…</p>
          </section>
        )}

        {state.kind === "error" && (
          <section className="card">
            <div className="status status--down">
              <span className="dot" />
              <div>
                <strong>Couldn’t load analytics</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          </section>
        )}

        {state.kind === "ready" && state.data.totalWorkouts === 0 && (
          <section className="card">
            <p className="muted">No finished workouts yet. Complete a session to see your progress.</p>
            <Link to="/workouts/start" className="button button--block">
              Start a workout
            </Link>
          </section>
        )}

        {state.kind === "ready" && state.data.totalWorkouts > 0 && (
          <>
            <section className="card">
              <header className="card__header">
                <h2>Overview</h2>
              </header>
              <dl className="details">
                <div>
                  <dt className="muted">Finished workouts</dt>
                  <dd>{state.data.totalWorkouts}</dd>
                </div>
                <div>
                  <dt className="muted">Total volume</dt>
                  <dd>{state.data.totalVolume.toLocaleString()} kg</dd>
                </div>
              </dl>
            </section>

            <section className="card">
              <header className="card__header">
                <h2>Volume over time</h2>
              </header>
              <div className="chart">
                <ResponsiveContainer width="100%" height={240}>
                  <LineChart data={state.data.volumeOverTime}>
                    <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                    <XAxis dataKey="date" tick={axisTick} stroke="var(--border)" />
                    <YAxis tick={axisTick} stroke="var(--border)" />
                    <Tooltip contentStyle={tooltipStyle} />
                    <Line type="monotone" dataKey="volume" stroke="var(--accent)" strokeWidth={2} dot={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </section>

            {state.data.oneRepMaxOverTime.length > 0 && (
              <section className="card">
                <header className="card__header">
                  <h2>Estimated 1RM over time</h2>
                </header>
                <p className="muted">Best estimated 1RM (Epley) per session. Toggle exercises to compare.</p>

                <div className="legend-toggles">
                  {state.data.oneRepMaxOverTime.map((series) => {
                    const on = selectedExercises.includes(series.exerciseName);
                    const color = colorFor(series.exerciseName, state.data.oneRepMaxOverTime);
                    return (
                      <button
                        key={series.exerciseName}
                        type="button"
                        className={on ? "legend-toggle legend-toggle--on" : "legend-toggle"}
                        style={on ? { borderColor: color, color } : undefined}
                        onClick={() => toggleExercise(series.exerciseName)}
                      >
                        <span className="legend-dot" style={{ background: on ? color : "var(--muted)" }} />
                        {series.exerciseName}
                      </button>
                    );
                  })}
                </div>

                {selectedExercises.length === 0 ? (
                  <p className="muted">Select one or more exercises above to plot their progression.</p>
                ) : (
                  <div className="chart">
                    <ResponsiveContainer width="100%" height={280}>
                      <LineChart data={pivotOneRepMax(state.data.oneRepMaxOverTime)}>
                        <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                        <XAxis dataKey="date" tick={axisTick} stroke="var(--border)" />
                        <YAxis tick={axisTick} stroke="var(--border)" unit=" kg" width={56} />
                        <Tooltip contentStyle={tooltipStyle} />
                        {state.data.oneRepMaxOverTime
                          .filter((series) => selectedExercises.includes(series.exerciseName))
                          .map((series) => (
                            <Line
                              key={series.exerciseName}
                              type="monotone"
                              dataKey={series.exerciseName}
                              stroke={colorFor(series.exerciseName, state.data.oneRepMaxOverTime)}
                              strokeWidth={2}
                              dot={false}
                              connectNulls
                            />
                          ))}
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </section>
            )}

            <section className="card">
              <header className="card__header">
                <h2>Workouts per week</h2>
              </header>
              <div className="chart">
                <ResponsiveContainer width="100%" height={240}>
                  <BarChart data={state.data.workoutsPerWeek}>
                    <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                    <XAxis dataKey="weekStart" tick={axisTick} stroke="var(--border)" />
                    <YAxis allowDecimals={false} tick={axisTick} stroke="var(--border)" />
                    <Tooltip contentStyle={tooltipStyle} cursor={{ fill: "var(--border)", opacity: 0.3 }} />
                    <Bar dataKey="workouts" fill="var(--accent)" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="card">
              <header className="card__header">
                <h2>Primary muscle-group set distribution</h2>
              </header>
              <p className="muted">Sets per muscle group where it is a primary mover — a training-emphasis proxy, not a stimulus score.</p>
              <div className="chart">
                <ResponsiveContainer width="100%" height={Math.max(240, state.data.primaryMuscleSetDistribution.length * 32)}>
                  <BarChart data={state.data.primaryMuscleSetDistribution} layout="vertical">
                    <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                    <XAxis type="number" allowDecimals={false} tick={axisTick} stroke="var(--border)" />
                    <YAxis type="category" dataKey="code" width={100} tick={axisTick} stroke="var(--border)" />
                    <Tooltip contentStyle={tooltipStyle} cursor={{ fill: "var(--border)", opacity: 0.3 }} />
                    <Bar dataKey="setCount" fill="var(--accent)" radius={[0, 4, 4, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </section>

            <section className="card">
              <header className="card__header">
                <h2>Best set per exercise</h2>
              </header>
              <p className="muted">Ranked by estimated 1RM (Epley).</p>
              {state.data.bestSets.length === 0 ? (
                <p className="muted">No weighted sets logged yet.</p>
              ) : (
                <ul className="exercise-list">
                  {state.data.bestSets.map((best) => (
                    <li key={best.exerciseName} className="exercise-row">
                      <div className="exercise-row__main">
                        <div className="exercise-row__title">
                          <strong>{best.exerciseName}</strong>
                          <span className="badge">~{best.estimatedOneRepMax} kg 1RM</span>
                        </div>
                        <p className="muted exercise-row__muscles">
                          {best.weight} kg × {best.reps}
                          {best.performedAt ? ` · ${best.performedAt}` : ""}
                        </p>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
