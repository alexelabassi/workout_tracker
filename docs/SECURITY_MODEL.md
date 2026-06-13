# SECURITY_MODEL.md

# Security Model

This app must be secure enough to look like a real SaaS project.

## Auth

Use:

- email/password registration
- BCrypt password hashing
- JWT access tokens
- refresh tokens stored as hashes
- stateless Spring Security config

Do not use:

- HTTP Basic
- plain text tokens
- H2-only auth examples
- fake users

---

## Roles

```text
USER
COACH
ADMIN
```

### USER

Can manage own resources and view public templates.

### COACH

Can access client resources only through active coach-client relationships.

### ADMIN

Can manage official data and coach accounts.

---

## Authorization Model

Every protected request must satisfy one of:

```text
resource.owner_user_id == current_user.id
resource is public
current_user.role == ADMIN
current_user is active coach of resource owner
```

This prevents IDOR vulnerabilities.

---

## Ownership Rules

User-owned resources:

- custom exercises
- routines
- gyms
- equipment
- private templates
- workout sessions
- sets
- saves/votes
- coach-client acceptance/revocation

Public readable resources:

- official exercises
- public templates
- marketplace stats

Admin-owned/global resources:

- official exercises
- coach account creation
- optional moderation

---

## Coach Access Rules

A coach can read client data only when:

```text
coach_client_relationships.status = ACTIVE
```

Coach can view:

- client workout sessions
- client session exercises
- client sets
- client analytics
- client templates if allowed
- assigned template status

Coach can create:

- session comments
- template assignments

Coach cannot:

- edit client workout history
- delete client sessions
- change client account settings
- impersonate client
- see users who are not clients

---

## Marketplace Security

Public templates:

- readable by all authenticated users
- copyable by authenticated users
- editable only by owner
- admin can moderate later

When copied:

```text
public template -> private owned copy
```

Do not make private user templates depend forever on public templates.

---

## API Security Checklist

For every controller method:

1. Resolve authenticated user.
2. Load resource.
3. Check ownership/public/coach/admin permission.
4. Use DTOs, not raw entity exposure.
5. Validate input.
6. Return safe response.

---

## Dangerous Bugs To Avoid

### IDOR

Bad:

```text
GET /api/workouts/{sessionId}
```

returning any session by ID.

Good:

```text
Load session and verify user owns it or coach has active relationship.
```

### Coach Overreach

Bad:

```text
role == COACH can view all workouts
```

Good:

```text
role == COACH AND active relationship with specific client
```

### Public Template Mutation

Bad:

```text
any user can edit copied public template source
```

Good:

```text
Use Template creates private copy.
```

### Raw JWT Trust

Bad:

```text
trust user_id from request body
```

Good:

```text
derive current user from token.
```
