import { useState } from "react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { ApiError } from "../../shared/api/client";
import { fetchAnalysis } from "./api";
import type { Severity, TemplateAnalysis } from "./types";

const SEVERITY_COLORS: Record<Severity, string> = {
  CRITICAL: "#b3261e",
  HIGH: "#ef4d5a",
  MEDIUM: "#f5a623",
  LOW: "#4f8cff",
  INFO: "#8893a5",
};

const CATEGORY_LABELS: Record<string, string> = {
  WELL_STRUCTURED: "Well structured",
  DECENT_STRUCTURE: "Decent structure",
  NEEDS_REVIEW: "Needs review",
};

interface PanelProps {
  templateId: string;
}

type State =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; analysis: TemplateAnalysis };

export function TemplateAnalysisPanel({ templateId }: PanelProps) {
  const [state, setState] = useState<State>({ kind: "idle" });

  const run = async () => {
    setState({ kind: "loading" });
    try {
      const analysis = await fetchAnalysis(templateId);
      setState({ kind: "ready", analysis });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not analyze the template." });
    }
  };

  return (
    <section className="card">
      <header className="card__header">
        <h2>Structure analysis</h2>
        {state.kind !== "loading" && (
          <button type="button" className="button button--ghost" onClick={() => void run()}>
            {state.kind === "ready" ? "Re-analyze" : "Analyze structure"}
          </button>
        )}
      </header>

      {state.kind === "idle" && (
        <p className="muted">
          Evidence-informed structural analysis using general resistance-training heuristics.
        </p>
      )}
      {state.kind === "loading" && <p className="muted">Analyzing…</p>}
      {state.kind === "error" && <p className="form-error">{state.message}</p>}

      {state.kind === "ready" && <AnalysisBody analysis={state.analysis} />}
    </section>
  );
}

function AnalysisBody({ analysis }: { analysis: TemplateAnalysis }) {
  const volumeData = analysis.muscleGroupVolumes.filter((v) => v.weeklyWeightedSets > 0 || v.volumeDataIncomplete);
  const ratio = (value: number | null) => (value == null ? "—" : value.toFixed(2));

  return (
    <div>
      <div className="analysis-head">
        <div className="analysis-score">{analysis.overallScore}</div>
        <div>
          <strong>{CATEGORY_LABELS[analysis.category] ?? analysis.category}</strong>
          <p className="muted">Template Structure Score (out of 100)</p>
        </div>
      </div>
      <p>{analysis.summary}</p>

      {analysis.warnings.length > 0 && (
        <ul className="exercise-list">
          {analysis.warnings.map((warning, index) => (
            <li key={`${warning.code}-${index}`} className="exercise-row">
              <div className="exercise-row__main">
                <div className="exercise-row__title">
                  <span className="sev-chip" style={{ background: SEVERITY_COLORS[warning.severity] }}>
                    {warning.severity}
                  </span>
                  <strong>{warning.title}</strong>
                </div>
                <p className="muted exercise-row__muscles">{warning.explanation}</p>
                {warning.suggestedFix && <p className="muted">Fix: {warning.suggestedFix}</p>}
              </div>
            </li>
          ))}
        </ul>
      )}

      <h3 className="builder-subhead">Weekly volume (weighted sets)</h3>
      {volumeData.length > 0 ? (
        <div className="chart">
          <ResponsiveContainer width="100%" height={Math.max(200, volumeData.length * 30)}>
            <BarChart data={volumeData} layout="vertical">
              <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
              <XAxis type="number" allowDecimals tick={{ fill: "var(--muted)", fontSize: 12 }} stroke="var(--border)" />
              <YAxis type="category" dataKey="muscleGroup" width={90}
                tick={{ fill: "var(--muted)", fontSize: 12 }} stroke="var(--border)" />
              <Tooltip contentStyle={{ background: "var(--surface)", border: "1px solid var(--border)", borderRadius: 8, color: "var(--text)" }} />
              <Bar dataKey="weeklyWeightedSets" fill="var(--accent)" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      ) : (
        <p className="muted">No counted volume (check that exercises have planned set counts).</p>
      )}

      <h3 className="builder-subhead">Frequency &amp; balance</h3>
      <p className="muted">
        Pull/Push {ratio(analysis.balanceRatios.pullToPush)} · Posterior/Quads{" "}
        {ratio(analysis.balanceRatios.posteriorToQuads)} · Lower/Upper {ratio(analysis.balanceRatios.lowerToUpper)}
      </p>
      <p className="muted">
        Days/week per muscle:{" "}
        {analysis.frequencyByMuscleGroup
          .filter((f) => f.daysPerWeek > 0)
          .map((f) => `${f.muscleGroup} ${f.daysPerWeek}`)
          .join(" · ") || "—"}
      </p>

      {analysis.strengths.length > 0 && (
        <>
          <h3 className="builder-subhead">Strengths</h3>
          <ul className="muted">
            {analysis.strengths.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </>
      )}

      {analysis.suggestions.length > 0 && (
        <>
          <h3 className="builder-subhead">Suggestions</h3>
          <ul className="muted">
            {analysis.suggestions.map((s) => (
              <li key={s}>{s}</li>
            ))}
          </ul>
        </>
      )}

      <details className="analysis-fine-print">
        <summary>Limitations &amp; disclaimer</summary>
        <ul className="muted">
          {analysis.limitations.map((l) => (
            <li key={l}>{l}</li>
          ))}
        </ul>
        <p className="muted">{analysis.disclaimer}</p>
      </details>
    </div>
  );
}
