import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError } from "../../shared/api/client";
import { useToast } from "../../shared/ui/ToastProvider";
import { acceptInvite, fetchCoaches, fetchInvites, rejectInvite, revokeCoach } from "./api";
import type { CoachRelationship } from "./types";

type LoadState =
  | { kind: "loading" }
  | { kind: "error"; message: string }
  | { kind: "ready"; invites: CoachRelationship[]; coaches: CoachRelationship[] };

/**
 * Client-side coaching management: pending invites (accept/reject) and active coaches (revoke).
 * Revoking a coach immediately removes their read access to this user's data.
 */
export function CoachingPage() {
  const { showToast } = useToast();
  const [state, setState] = useState<LoadState>({ kind: "loading" });

  const load = useCallback(async () => {
    setState({ kind: "loading" });
    try {
      const [invites, coaches] = await Promise.all([fetchInvites(), fetchCoaches()]);
      setState({ kind: "ready", invites, coaches });
    } catch (err) {
      setState({ kind: "error", message: err instanceof ApiError ? err.message : "Could not load coaching." });
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const act = async (action: () => Promise<void>, success: string) => {
    try {
      await action();
      showToast(success);
      void load();
    } catch (err) {
      showToast(err instanceof ApiError ? err.message : "Action failed.", "error");
    }
  };

  return (
    <div className="app">
      <header className="app__header">
        <div>
          <h1>Coaching</h1>
          <p className="muted">
            <Link to="/">← Back to dashboard</Link>
          </p>
        </div>
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
                <strong>Couldn’t load coaching</strong>
                <p className="muted">{state.message}</p>
                <button type="button" className="button button--ghost" onClick={() => void load()}>
                  Retry
                </button>
              </div>
            </div>
          </section>
        )}

        {state.kind === "ready" && (
          <>
            <section className="card">
              <header className="card__header">
                <h2>Pending invites</h2>
              </header>
              {state.invites.length === 0 ? (
                <p className="muted">No pending invites.</p>
              ) : (
                <ul className="exercise-list">
                  {state.invites.map((invite) => (
                    <li key={invite.relationshipId} className="exercise-row">
                      <div className="exercise-row__main">
                        <div className="exercise-row__title">
                          <strong>{invite.coachDisplayName ?? invite.coachEmail}</strong>
                        </div>
                        <p className="muted exercise-row__muscles">
                          {invite.coachEmail} wants to coach you
                        </p>
                      </div>
                      <div className="app__actions">
                        <button type="button" className="button"
                          onClick={() => void act(() => acceptInvite(invite.relationshipId), "Invite accepted.")}>
                          Accept
                        </button>
                        <button type="button" className="button button--ghost"
                          onClick={() => void act(() => rejectInvite(invite.relationshipId), "Invite rejected.")}>
                          Reject
                        </button>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>

            <section className="card">
              <header className="card__header">
                <h2>Your coaches</h2>
              </header>
              <p className="muted">A coach can view your workout history and progress. You can revoke access anytime.</p>
              {state.coaches.length === 0 ? (
                <p className="muted">You have no active coaches.</p>
              ) : (
                <ul className="exercise-list">
                  {state.coaches.map((coach) => (
                    <li key={coach.relationshipId} className="exercise-row">
                      <div className="exercise-row__main">
                        <div className="exercise-row__title">
                          <strong>{coach.coachDisplayName ?? coach.coachEmail}</strong>
                          <span className="badge badge--muted">active</span>
                        </div>
                        <p className="muted exercise-row__muscles">
                          {coach.coachEmail}
                          {coach.since ? ` · since ${new Date(coach.since).toLocaleDateString()}` : ""}
                        </p>
                      </div>
                      <div className="app__actions">
                        <button type="button" className="button button--ghost"
                          onClick={() => void act(() => revokeCoach(coach.relationshipId), "Coach access revoked.")}>
                          Revoke
                        </button>
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
