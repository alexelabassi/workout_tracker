export type RelationshipStatus = "PENDING" | "ACTIVE" | "REVOKED" | "REJECTED";

/** A coach's view of one active client. */
export interface ClientSummary {
  clientId: string;
  displayName: string | null;
  email: string;
  activeSince: string | null;
}

/** A client's view of a relationship — a pending invite (PENDING) or an active coach (ACTIVE). */
export interface CoachRelationship {
  relationshipId: string;
  coachUserId: string;
  coachDisplayName: string | null;
  coachEmail: string;
  status: RelationshipStatus;
  since: string | null;
}
