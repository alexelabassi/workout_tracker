import { useState } from "react";
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
import type { AnalyticsOverview, OneRepMaxSeries } from "./types";

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
const DEFAULT_VISIBLE_SERIES = 5;

function colorFor(exerciseName: string, allSeries: OneRepMaxSeries[]): string {
  const index = allSeries.findIndex((series) => series.exerciseName === exerciseName);
  return SERIES_COLORS[(index < 0 ? 0 : index) % SERIES_COLORS.length];
}

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

/**
 * Presentational analytics dashboard (overview + charts), driven purely by the supplied data so it
 * can be reused for the signed-in user's own analytics and for a coach viewing a client's analytics.
 * The {@code emptyAction} slot lets each caller show a contextual call-to-action when there is no data.
 */
export function AnalyticsView({
  data,
  emptyAction,
}: {
  data: AnalyticsOverview;
  emptyAction?: React.ReactNode;
}) {
  const [selectedExercises, setSelectedExercises] = useState<string[]>(() =>
    data.oneRepMaxOverTime.slice(0, DEFAULT_VISIBLE_SERIES).map((series) => series.exerciseName),
  );

  const toggleExercise = (name: string) =>
    setSelectedExercises((current) =>
      current.includes(name) ? current.filter((item) => item !== name) : [...current, name],
    );

  if (data.totalWorkouts === 0) {
    return (
      <section className="card">
        <p className="muted">No finished workouts yet.</p>
        {emptyAction}
      </section>
    );
  }

  return (
    <>
      <section className="card">
        <header className="card__header">
          <h2>Overview</h2>
        </header>
        <dl className="details">
          <div>
            <dt className="muted">Finished workouts</dt>
            <dd>{data.totalWorkouts}</dd>
          </div>
          <div>
            <dt className="muted">Total volume</dt>
            <dd>{data.totalVolume.toLocaleString()} kg</dd>
          </div>
        </dl>
      </section>

      <section className="card">
        <header className="card__header">
          <h2>Volume over time</h2>
        </header>
        <div className="chart">
          <ResponsiveContainer width="100%" height={240}>
            <LineChart data={data.volumeOverTime}>
              <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={axisTick} stroke="var(--border)" />
              <YAxis tick={axisTick} stroke="var(--border)" />
              <Tooltip contentStyle={tooltipStyle} />
              <Line type="monotone" dataKey="volume" stroke="var(--accent)" strokeWidth={2} dot={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </section>

      {data.oneRepMaxOverTime.length > 0 && (
        <section className="card">
          <header className="card__header">
            <h2>Estimated 1RM over time</h2>
          </header>
          <p className="muted">Best estimated 1RM (Epley) per session. Toggle exercises to compare.</p>

          <div className="legend-toggles">
            {data.oneRepMaxOverTime.map((series) => {
              const on = selectedExercises.includes(series.exerciseName);
              const color = colorFor(series.exerciseName, data.oneRepMaxOverTime);
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
                <LineChart data={pivotOneRepMax(data.oneRepMaxOverTime)}>
                  <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                  <XAxis dataKey="date" tick={axisTick} stroke="var(--border)" />
                  <YAxis tick={axisTick} stroke="var(--border)" unit=" kg" width={56} />
                  <Tooltip contentStyle={tooltipStyle} />
                  {data.oneRepMaxOverTime
                    .filter((series) => selectedExercises.includes(series.exerciseName))
                    .map((series) => (
                      <Line
                        key={series.exerciseName}
                        type="monotone"
                        dataKey={series.exerciseName}
                        stroke={colorFor(series.exerciseName, data.oneRepMaxOverTime)}
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
            <BarChart data={data.workoutsPerWeek}>
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
          <ResponsiveContainer width="100%" height={Math.max(240, data.primaryMuscleSetDistribution.length * 32)}>
            <BarChart data={data.primaryMuscleSetDistribution} layout="vertical">
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
        {data.bestSets.length === 0 ? (
          <p className="muted">No weighted sets logged yet.</p>
        ) : (
          <ul className="exercise-list">
            {data.bestSets.map((best) => (
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
  );
}
