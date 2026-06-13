# COACH_MODE.md

# Coach Mode

## Purpose

Coach mode solves the subjectivity problem.

The app does not pretend to be the user's coach. Instead, it gives a real human coach structured access to workout history, analytics, comments, and assigned templates.

This is stronger than fake AI recommendations.

---

## Roles

```text
USER
COACH
ADMIN
```

## Admin Flow

Admin can:

- create coach account
- activate/deactivate coach profile
- view coach list
- optionally revoke coach status

## Coach-Client Flow

Option A, recommended:

1. Coach invites user.
2. User accepts.
3. Relationship becomes ACTIVE.
4. User can revoke anytime.

Option B:

1. User requests coach.
2. Coach accepts.
3. Relationship becomes ACTIVE.

Implement Option A first.

---

## Coach Permissions

Coach can:

- view active clients
- view client workout history
- view client analytics
- view client templates if permitted
- comment on client sessions
- assign templates to clients

Coach cannot:

- edit client sessions
- delete client workouts
- change client account data
- see non-client data
- impersonate clients

---

## Coach Dashboard

Pages:

- Coach Dashboard
- Clients List
- Client Profile
- Client Workout History
- Client Analytics
- Session Comments
- Assigned Templates

---

## Data Model

Tables:

- `coach_profiles`
- `coach_client_relationships`
- `coach_session_comments`
- `coach_template_assignments`

Security check:

```text
current_user.role == COACH
AND relationship.coach_user_id == current_user.id
AND relationship.client_user_id == targetUserId
AND relationship.status == ACTIVE
```

---

## Thesis Angle

Coach mode adds:

- RBAC
- relationship-based access control
- scoped data sharing
- collaborative review
- practical product value

This is much better than an app pretending to know the perfect workout for everyone.
