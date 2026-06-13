const state = {
    route: getRoute(),
    theme: localStorage.getItem("workout.theme") || "dark",
    credentials: loadCredentials(),
    user: null,
    loading: false,
    error: null,
    toast: null,
    authMode: "login",
    catalogTab: "gyms",
    selectedTemplateId: null,
    selectedHistorySessionId: null,
    currentSession: null,
    historySession: null,
    data: {
        gyms: [],
        equipment: [],
        exercises: [],
        routines: [],
        templates: [],
        sessions: []
    }
};

const navItems = [
    { route: "dashboard", label: "Dashboard", icon: "D" },
    { route: "gyms-equipment", label: "Gyms & Equipment", icon: "G" },
    { route: "exercises", label: "Exercises", icon: "E" },
    { route: "routines", label: "Routines", icon: "R" },
    { route: "templates", label: "Templates", icon: "T" },
    { route: "start-workout", label: "Start Workout", icon: "S" },
    { route: "history", label: "History", icon: "H" }
];

document.documentElement.dataset.theme = state.theme;
window.addEventListener("hashchange", handleRouteChange);
document.addEventListener("DOMContentLoaded", initializeApp);
document.addEventListener("click", handleDocumentClick);
document.addEventListener("submit", handleDocumentSubmit);
document.addEventListener("change", handleDocumentChange);

async function initializeApp() {
    render();

    if (!state.credentials) {
        navigateTo("auth");
        return;
    }

    await loadCurrentUser();
}

async function loadCurrentUser() {
    startLoading();

    try {
        state.user = await api("/api/auth/me");
        await loadData();

        if (state.route.name === "auth") {
            navigateTo("dashboard");
        }
    } catch (error) {
        clearCredentials();
        state.user = null;
        state.error = error.message;
        navigateTo("auth");
    } finally {
        stopLoading();
    }
}

async function loadData() {
    const [gyms, equipment, exercises, routines, templates, sessions] = await Promise.all([
        api("/api/gyms"),
        api("/api/equipment"),
        api("/api/exercises"),
        api("/api/routines"),
        api("/api/workout-templates"),
        api("/api/workout-sessions")
    ]);

    state.data.gyms = gyms;
    state.data.equipment = equipment;
    state.data.exercises = exercises;
    state.data.routines = routines;
    state.data.templates = templates;
    state.data.sessions = sessions;

    if (!state.selectedTemplateId && templates.length > 0) {
        state.selectedTemplateId = templates[0].id;
    }

    if (!state.selectedHistorySessionId && sessions.length > 0) {
        state.selectedHistorySessionId = sessions[0].id;
    }

    await loadRouteDetails();
}

async function loadRouteDetails() {
    if (state.route.name === "templates" && state.selectedTemplateId) {
        state.selectedTemplate = await api(`/api/workout-templates/${state.selectedTemplateId}`);
    }

    if (state.route.name === "live-workout" && state.route.id) {
        state.currentSession = await api(`/api/workout-sessions/${state.route.id}`);
    }

    if (state.route.name === "summary" && state.route.id) {
        state.historySession = await api(`/api/workout-sessions/${state.route.id}`);
    }

    if (state.route.name === "history" && state.selectedHistorySessionId) {
        state.historySession = await api(`/api/workout-sessions/${state.selectedHistorySessionId}`);
    }
}

async function api(path, options = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...(options.headers || {})
    };

    if (state.credentials) {
        headers.Authorization = `Basic ${btoa(`${state.credentials.email}:${state.credentials.password}`)}`;
    }

    const response = await fetch(path, {
        ...options,
        headers
    });

    if (!response.ok) {
        let message = "Request failed";

        try {
            const body = await response.json();
            message = body.message || message;
        } catch (error) {
            message = response.statusText || message;
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return null;
    }

    return response.json();
}

function render() {
    const app = document.querySelector("#app");

    if (!state.user || state.route.name === "auth") {
        app.innerHTML = renderAuthPage();
        renderToast();
        return;
    }

    app.innerHTML = `
        <div class="app-shell">
            ${renderSidebar()}
            <main class="main">
                ${renderTopbar()}
                ${renderMainContent()}
            </main>
        </div>
    `;

    renderToast();
}

function renderAuthPage() {
    const isLogin = state.authMode === "login";

    return `
        <main class="auth-shell">
            <section class="auth-panel">
                <div class="auth-art">
                    <div class="brand">
                        <div class="brand-mark">W</div>
                        <div>
                            <div class="brand-title">Workout Thesis</div>
                            <div class="brand-subtitle">Core workout flow</div>
                        </div>
                    </div>
                    <div>
                        <p class="page-kicker">Bachelor thesis</p>
                        <h1>Reusable plans. Real workouts. Historical truth.</h1>
                        <p>Templates can change. Sessions keep copied snapshots, so old workouts remain correct.</p>
                    </div>
                </div>
                <div class="auth-form">
                    <div class="toolbar">
                        <div>
                            <p class="page-kicker">Account</p>
                            <h2>${isLogin ? "Sign in" : "Create account"}</h2>
                        </div>
                        <div class="segmented">
                            <button class="segment ${isLogin ? "active" : ""}" data-action="auth-mode" data-mode="login">Login</button>
                            <button class="segment ${!isLogin ? "active" : ""}" data-action="auth-mode" data-mode="register">Register</button>
                        </div>
                    </div>
                    ${state.error ? `<div class="error-box">${escapeHtml(state.error)}</div>` : ""}
                    <form class="form section" id="${isLogin ? "loginForm" : "registerForm"}">
                        ${isLogin ? "" : `
                            <div class="field">
                                <label for="name">Name</label>
                                <input id="name" name="name" autocomplete="name" required maxlength="120">
                            </div>
                        `}
                        <div class="field">
                            <label for="email">Email</label>
                            <input id="email" name="email" type="email" autocomplete="email" required maxlength="180">
                        </div>
                        <div class="field">
                            <label for="password">Password</label>
                            <input id="password" name="password" type="password" autocomplete="${isLogin ? "current-password" : "new-password"}" required minlength="8" maxlength="120">
                        </div>
                        <button class="button primary" type="submit">${isLogin ? "Login" : "Register"}</button>
                    </form>
                </div>
            </section>
        </main>
    `;
}

function renderSidebar() {
    return `
        <aside class="sidebar">
            <div class="brand">
                <div class="brand-mark">W</div>
                <div>
                    <div class="brand-title">Workout Thesis</div>
                    <div class="brand-subtitle">Snapshot-aware tracker</div>
                </div>
            </div>
            <nav class="nav">
                ${navItems.map(item => `
                    <a class="nav-link ${state.route.name === item.route ? "active" : ""}" href="#/${item.route}">
                        <span class="nav-icon">${item.icon}</span>
                        <span class="nav-label">${item.label}</span>
                    </a>
                `).join("")}
            </nav>
            <div class="sidebar-bottom">
                <button class="button" data-action="toggle-theme">${state.theme === "dark" ? "Light mode" : "Dark mode"}</button>
                <div class="profile-card">
                    <div class="profile-name">${escapeHtml(state.user.name)}</div>
                    <div class="profile-email">${escapeHtml(state.user.email)}</div>
                    <button class="button section" data-action="logout">Logout</button>
                </div>
            </div>
        </aside>
    `;
}

function renderTopbar() {
    const copy = getPageCopy();

    return `
        <header class="topbar">
            <div>
                <p class="page-kicker">${copy.kicker}</p>
                <h1>${copy.title}</h1>
                <p class="lead">${copy.lead}</p>
            </div>
            <div class="badge-row">
                <button class="button" data-action="refresh">Refresh</button>
                ${getActiveSessionButton()}
            </div>
        </header>
    `;
}

function renderMainContent() {
    if (state.loading) {
        return `<div class="loading">Loading workout data</div>`;
    }

    if (state.error) {
        return `<div class="error-box">${escapeHtml(state.error)}</div>`;
    }

    if (state.route.name === "gyms-equipment") {
        return renderGymsEquipmentPage();
    }

    if (state.route.name === "exercises") {
        return renderExercisesPage();
    }

    if (state.route.name === "routines") {
        return renderRoutinesPage();
    }

    if (state.route.name === "templates") {
        return renderTemplatesPage();
    }

    if (state.route.name === "start-workout") {
        return renderStartWorkoutPage();
    }

    if (state.route.name === "live-workout") {
        return renderLiveWorkoutPage();
    }

    if (state.route.name === "summary") {
        return renderSummaryPage();
    }

    if (state.route.name === "history") {
        return renderHistoryPage();
    }

    return renderDashboardPage();
}

function renderDashboardPage() {
    const sessions = state.data.sessions;
    const activeSessions = sessions.filter(session => session.status === "IN_PROGRESS");
    const completedSessions = sessions.filter(session => session.status === "COMPLETED");
    const totalSets = state.historySession?.exercises?.reduce(
            (sum, exercise) => sum + exercise.sets.length,
            0
    ) || 0;

    return `
        <section class="grid three">
            ${renderMetric("Templates", state.data.templates.length, "Reusable plans")}
            ${renderMetric("Real workouts", sessions.length, "Sessions started")}
            ${renderMetric("Active", activeSessions.length, "In progress")}
        </section>
        <section class="section grid two">
            <div class="card snapshot-note">
                <h2>Historical truth</h2>
                <p class="lead">A workout template is a reusable plan. A workout session is a real workout. Session snapshots preserve the names, equipment, order, and planned targets that existed when the session started.</p>
            </div>
            <div class="card">
                <h2>Latest completed summary</h2>
                ${completedSessions.length ? `
                    <div class="item">
                        <div class="item-title">${escapeHtml(completedSessions[0].templateName)}</div>
                        <div class="item-meta">${formatDate(completedSessions[0].completedAt)} - ${totalSets} sets visible in selected detail</div>
                    </div>
                ` : `<div class="empty">No completed workouts yet</div>`}
            </div>
        </section>
        <section class="section grid two">
            <div class="card">
                <div class="toolbar">
                    <h2>Recent sessions</h2>
                    <a class="button" href="#/history">History</a>
                </div>
                ${renderSessionList(sessions.slice(0, 5))}
            </div>
            <div class="card">
                <div class="toolbar">
                    <h2>Templates</h2>
                    <a class="button" href="#/templates">Builder</a>
                </div>
                ${renderTemplateList(state.data.templates.slice(0, 5))}
            </div>
        </section>
    `;
}

function renderGymsEquipmentPage() {
    return `
        <section class="grid two">
            <div class="card">
                <h2>Gyms</h2>
                <form class="form" id="gymForm">
                    <div class="field">
                        <label for="gymName">Name</label>
                        <input id="gymName" name="name" required maxlength="120">
                    </div>
                    <div class="field">
                        <label for="gymAddress">Address</label>
                        <input id="gymAddress" name="address" maxlength="255">
                    </div>
                    <button class="button primary" type="submit">Save gym</button>
                </form>
                <div class="section">${renderGymList()}</div>
            </div>
            <div class="card">
                <h2>Equipment</h2>
                <form class="form" id="equipmentForm">
                    <div class="field">
                        <label for="equipmentGymId">Gym</label>
                        ${renderSelect("equipmentGymId", "gymId", state.data.gyms, "No gym")}
                    </div>
                    <div class="field">
                        <label for="equipmentName">Name</label>
                        <input id="equipmentName" name="name" required maxlength="120">
                    </div>
                    <button class="button primary" type="submit">Save equipment</button>
                </form>
                <div class="section">${renderEquipmentList()}</div>
            </div>
        </section>
    `;
}

function renderExercisesPage() {
    return `
        <section class="grid sidebar-main">
            <div class="card">
                <h2>New exercise</h2>
                <form class="form" id="exerciseForm">
                    <div class="field">
                        <label for="exerciseEquipmentId">Equipment</label>
                        ${renderSelect("exerciseEquipmentId", "equipmentId", state.data.equipment, "No equipment")}
                    </div>
                    <div class="field">
                        <label for="exerciseName">Name</label>
                        <input id="exerciseName" name="name" required maxlength="120">
                    </div>
                    <div class="field">
                        <label for="exerciseMuscleGroup">Muscle group</label>
                        <input id="exerciseMuscleGroup" name="muscleGroup" maxlength="120">
                    </div>
                    <div class="field">
                        <label for="exerciseInstructions">Instructions</label>
                        <textarea id="exerciseInstructions" name="instructions" maxlength="1000"></textarea>
                    </div>
                    <button class="button primary" type="submit">Save exercise</button>
                </form>
            </div>
            <div class="card">
                <h2>Exercise library</h2>
                ${renderExerciseList()}
            </div>
        </section>
    `;
}

function renderRoutinesPage() {
    return `
        <section class="grid sidebar-main">
            <div class="card">
                <h2>New routine</h2>
                <form class="form" id="routineForm">
                    <div class="field">
                        <label for="routineName">Name</label>
                        <input id="routineName" name="name" required maxlength="120">
                    </div>
                    <div class="field">
                        <label for="routineDescription">Description</label>
                        <textarea id="routineDescription" name="description" maxlength="500"></textarea>
                    </div>
                    <button class="button primary" type="submit">Save routine</button>
                </form>
            </div>
            <div class="card">
                <h2>Routine library</h2>
                ${renderRoutineList()}
            </div>
        </section>
    `;
}

function renderTemplatesPage() {
    return `
        <section class="grid sidebar-main">
            <div class="card">
                <h2>Create reusable plan</h2>
                <p class="lead">Templates are plans. They are copied into session snapshots when a real workout starts.</p>
                <form class="form section" id="templateForm">
                    <div class="field">
                        <label for="templateGymId">Gym</label>
                        ${renderSelect("templateGymId", "gymId", state.data.gyms, "No gym")}
                    </div>
                    <div class="field">
                        <label for="templateName">Name</label>
                        <input id="templateName" name="name" required maxlength="120">
                    </div>
                    <div class="field">
                        <label for="templateDescription">Description</label>
                        <textarea id="templateDescription" name="description" maxlength="500"></textarea>
                    </div>
                    <button class="button primary" type="submit">Save template</button>
                </form>
                <div class="section">
                    <h3>Existing templates</h3>
                    ${renderTemplatePickerList()}
                </div>
            </div>
            <div class="card">
                ${renderTemplateBuilder()}
            </div>
        </section>
    `;
}

function renderStartWorkoutPage() {
    return `
        <section class="grid two">
            <div class="card">
                <h2>Start a real workout</h2>
                <p class="lead">Choose a template. The backend copies template routines and exercises into session snapshot tables.</p>
                <form class="form section" id="startWorkoutForm">
                    <div class="field">
                        <label for="workoutTemplateId">Workout template</label>
                        ${renderSelect("workoutTemplateId", "workoutTemplateId", state.data.templates, "Select a template")}
                    </div>
                    <button class="button primary" type="submit">Start workout</button>
                </form>
            </div>
            <div class="card snapshot-note">
                <h2>What happens on start</h2>
                <div class="list">
                    ${renderPlainItem("Template", "Reusable plan; editable later.")}
                    ${renderPlainItem("Session", "A real workout instance; status moves from in progress to completed.")}
                    ${renderPlainItem("Snapshots", "Copied exercise, routine, equipment, and plan data. This is the historical truth.")}
                </div>
            </div>
        </section>
    `;
}

function renderLiveWorkoutPage() {
    const session = state.currentSession;

    if (!session) {
        return `<div class="empty">No live session selected</div>`;
    }

    if (session.status !== "IN_PROGRESS") {
        return renderSummaryContent(session);
    }

    return `
        <section class="card snapshot-note">
            <div class="live-header">
                <div>
                    <div class="badge-row">
                        <span class="badge">Session</span>
                        <span class="badge blue">Snapshot data</span>
                        <span class="badge amber">${escapeHtml(session.status)}</span>
                    </div>
                    <h2 class="section">${escapeHtml(session.templateName)}</h2>
                    <p class="lead">${escapeHtml(session.gymName || "No gym")} - started ${formatDate(session.startedAt)}</p>
                </div>
                <button class="button primary" data-action="finish-workout" data-session-id="${session.id}">Finish workout</button>
            </div>
        </section>
        <section class="section grid two">
            <div class="card">
                <h2>START routines / END routines</h2>
                ${renderLiveRoutineList(session)}
            </div>
            <div class="card">
                <h2>Add extra live exercise</h2>
                ${renderAddLiveExerciseForm(session)}
            </div>
        </section>
        <section class="section card">
            <h2>Session exercises copied from template</h2>
            ${renderLiveExerciseList(session)}
        </section>
    `;
}

function renderSummaryPage() {
    if (!state.historySession) {
        return `<div class="empty">No summary selected</div>`;
    }

    return renderSummaryContent(state.historySession);
}

function renderSummaryContent(session) {
    const totalSets = getTotalSets(session);
    const totalExercises = session.exercises.length;
    const totalRoutines = session.routines.length;

    return `
        <section class="grid three">
            ${renderMetric("Sets", totalSets, "Logged work")}
            ${renderMetric("Exercises", totalExercises, "Snapshot exercises")}
            ${renderMetric("Routines", totalRoutines, "Snapshot routines")}
        </section>
        <section class="section card snapshot-note">
            <h2>Cooldown summary</h2>
            <p class="lead">${escapeHtml(session.templateName)} - ${escapeHtml(session.status)} - ${formatDate(session.startedAt)}${session.completedAt ? ` to ${formatDate(session.completedAt)}` : ""}</p>
            <p class="lead section">This summary reads session snapshots, not live template data. It is safe from later template, exercise, routine, and equipment edits.</p>
        </section>
        <section class="section card">
            <h2>Workout details</h2>
            ${renderReadOnlySession(session)}
        </section>
    `;
}

function renderHistoryPage() {
    return `
        <section class="grid sidebar-main">
            <div class="card">
                <h2>Workout history</h2>
                ${renderHistoryList()}
            </div>
            <div class="card">
                ${state.historySession ? renderSummaryContent(state.historySession) : `<div class="empty">Select a workout session</div>`}
            </div>
        </section>
    `;
}

function renderMetric(label, value, helper) {
    return `
        <div class="card metric">
            <div class="metric-value">${value}</div>
            <div>
                <div class="metric-label">${escapeHtml(label)}</div>
                <div class="item-meta">${escapeHtml(helper)}</div>
            </div>
        </div>
    `;
}

function renderGymList() {
    if (!state.data.gyms.length) {
        return `<div class="empty">No gyms yet</div>`;
    }

    return `<div class="list">${state.data.gyms.map(gym => renderPlainItem(gym.name, gym.address || "No address")).join("")}</div>`;
}

function renderEquipmentList() {
    if (!state.data.equipment.length) {
        return `<div class="empty">No equipment yet</div>`;
    }

    return `<div class="list">${state.data.equipment.map(equipment => renderPlainItem(equipment.name, equipment.gymName || "No gym", "blue")).join("")}</div>`;
}

function renderExerciseList() {
    if (!state.data.exercises.length) {
        return `<div class="empty">No exercises yet</div>`;
    }

    return `
        <div class="list">
            ${state.data.exercises.map(exercise => renderPlainItem(
                    exercise.name,
                    [exercise.muscleGroup, exercise.equipmentName].filter(Boolean).join(" - ") || "No details",
                    "blue"
            )).join("")}
        </div>
    `;
}

function renderRoutineList() {
    if (!state.data.routines.length) {
        return `<div class="empty">No routines yet</div>`;
    }

    return `<div class="list">${state.data.routines.map(routine => renderPlainItem(routine.name, routine.description || "No description", "amber")).join("")}</div>`;
}

function renderTemplateList(templates) {
    if (!templates.length) {
        return `<div class="empty">No templates yet</div>`;
    }

    return `<div class="list">${templates.map(template => renderPlainItem(template.name, template.gymName || template.description || "No gym", "amber")).join("")}</div>`;
}

function renderTemplatePickerList() {
    if (!state.data.templates.length) {
        return `<div class="empty">No templates yet</div>`;
    }

    return `
        <div class="list">
            ${state.data.templates.map(template => `
                <button class="item" data-action="select-template" data-template-id="${template.id}">
                    <div class="item-row">
                        <div>
                            <div class="item-title">${escapeHtml(template.name)}</div>
                            <div class="item-meta">${escapeHtml(template.gymName || template.description || "No gym")}</div>
                        </div>
                        <span class="badge ${state.selectedTemplateId === template.id ? "" : "amber"}">${state.selectedTemplateId === template.id ? "Open" : "Plan"}</span>
                    </div>
                </button>
            `).join("")}
        </div>
    `;
}

function renderTemplateBuilder() {
    const template = state.selectedTemplate;

    if (!template) {
        return `<div class="empty">Select or create a template to build a reusable plan</div>`;
    }

    return `
        <div class="toolbar">
            <div>
                <h2>${escapeHtml(template.name)}</h2>
                <p class="lead">Reusable plan - ${escapeHtml(template.gymName || "No gym")}</p>
            </div>
            <span class="badge amber">Template</span>
        </div>
        <div class="grid two">
            <form class="form" id="templateRoutineForm">
                <h3>Add routine to template</h3>
                <div class="field">
                    <label for="templateRoutineId">Routine</label>
                    ${renderSelect("templateRoutineId", "routineId", state.data.routines, "Select routine")}
                </div>
                <div class="form-grid">
                    <div class="field">
                        <label for="routineSortOrder">Order</label>
                        <input id="routineSortOrder" name="sortOrder" type="number" min="1" value="${template.routines.length + 1}" required>
                    </div>
                    <div class="field">
                        <label for="templateRoutineNotes">Notes</label>
                        <input id="templateRoutineNotes" name="notes" maxlength="500">
                    </div>
                </div>
                <button class="button primary" type="submit">Add routine</button>
            </form>
            <form class="form" id="templateExerciseForm">
                <h3>Add exercise to template</h3>
                <div class="form-grid">
                    <div class="field">
                        <label for="templateExerciseId">Exercise</label>
                        ${renderSelect("templateExerciseId", "exerciseId", state.data.exercises, "Select exercise")}
                    </div>
                    <div class="field">
                        <label for="templateRoutineLinkId">Routine slot</label>
                        ${renderTemplateRoutineSelect(template)}
                    </div>
                    <div class="field">
                        <label for="templateExerciseOrder">Order</label>
                        <input id="templateExerciseOrder" name="sortOrder" type="number" min="1" value="${template.exercises.length + 1}" required>
                    </div>
                    <div class="field">
                        <label for="plannedSets">Sets</label>
                        <input id="plannedSets" name="plannedSets" type="number" min="1">
                    </div>
                    <div class="field">
                        <label for="plannedReps">Reps</label>
                        <input id="plannedReps" name="plannedReps" type="number" min="1">
                    </div>
                    <div class="field">
                        <label for="plannedWeight">Weight</label>
                        <input id="plannedWeight" name="plannedWeight" type="number" min="0" step="0.01">
                    </div>
                </div>
                <div class="field">
                    <label for="templateExerciseNotes">Notes</label>
                    <input id="templateExerciseNotes" name="notes" maxlength="500">
                </div>
                <button class="button primary" type="submit">Add exercise</button>
            </form>
        </div>
        <div class="section grid two">
            <div>
                <h3>Template routines</h3>
                ${renderTemplateRoutines(template)}
            </div>
            <div>
                <h3>Template exercises</h3>
                ${renderTemplateExercises(template)}
            </div>
        </div>
    `;
}

function renderTemplateRoutineSelect(template) {
    return `
        <select id="templateRoutineLinkId" name="templateRoutineId">
            <option value="">No routine</option>
            ${template.routines.map(routine => `
                <option value="${routine.id}">${escapeHtml(routine.routineName)}</option>
            `).join("")}
        </select>
    `;
}

function renderTemplateRoutines(template) {
    if (!template.routines.length) {
        return `<div class="empty">No routines in this template</div>`;
    }

    return `
        <div class="list">
            ${template.routines.map(routine => renderPlainItem(
                    routine.routineName,
                    `Order ${routine.sortOrder}${routine.notes ? ` - ${routine.notes}` : ""}`,
                    "amber"
            )).join("")}
        </div>
    `;
}

function renderTemplateExercises(template) {
    if (!template.exercises.length) {
        return `<div class="empty">No exercises in this template</div>`;
    }

    return `
        <div class="list">
            ${template.exercises.map(exercise => renderPlainItem(
                    exercise.exerciseName,
                    getTemplateExerciseMeta(exercise),
                    "blue"
            )).join("")}
        </div>
    `;
}

function renderLiveRoutineList(session) {
    if (!session.routines.length) {
        return `<div class="empty">No routine snapshots in this session</div>`;
    }

    return `
        <div class="list">
            ${session.routines.map(routine => `
                <div class="item">
                    <div class="item-row">
                        <div>
                            <div class="item-title">${escapeHtml(routine.routineName)}</div>
                            <div class="item-meta">${escapeHtml(routine.routineDescription || "Routine snapshot")}</div>
                            <div class="badge-row section">
                                <span class="badge blue">Snapshot</span>
                                ${routine.startedAt ? `<span class="badge">Started ${formatDate(routine.startedAt)}</span>` : `<span class="badge amber">Not started</span>`}
                                ${routine.endedAt ? `<span class="badge">Ended ${formatDate(routine.endedAt)}</span>` : ""}
                            </div>
                        </div>
                        <div class="badge-row">
                            <button class="button secondary" data-action="start-routine" data-routine-id="${routine.id}" ${routine.startedAt || routine.endedAt ? "disabled" : ""}>START routine</button>
                            <button class="button secondary" data-action="end-routine" data-routine-id="${routine.id}" ${!routine.startedAt || routine.endedAt ? "disabled" : ""}>END routine</button>
                        </div>
                    </div>
                </div>
            `).join("")}
        </div>
    `;
}

function renderAddLiveExerciseForm(session) {
    return `
        <form class="form" id="quickEquipmentForm">
            <h3>Quick add equipment</h3>
            <div class="form-grid">
                <div class="field">
                    <label for="quickEquipmentGymId">Gym</label>
                    ${renderSelect("quickEquipmentGymId", "gymId", state.data.gyms, "No gym")}
                </div>
                <div class="field">
                    <label for="quickEquipmentName">Equipment name</label>
                    <input id="quickEquipmentName" name="name" maxlength="120">
                </div>
            </div>
            <button class="button" type="submit">Quick add equipment</button>
        </form>
        <form class="form section" id="liveExerciseForm">
            <h3>Add exercise to this session</h3>
            <div class="field">
                <label for="liveExistingExerciseId">Existing exercise</label>
                ${renderSelect("liveExistingExerciseId", "exerciseId", state.data.exercises, "Create from fields below")}
            </div>
            <div class="form-grid">
                <div class="field">
                    <label for="liveEquipmentId">Equipment selection</label>
                    ${renderSelect("liveEquipmentId", "equipmentId", state.data.equipment, "No equipment")}
                </div>
                <div class="field">
                    <label for="liveExerciseName">New exercise name</label>
                    <input id="liveExerciseName" name="name" maxlength="120">
                </div>
                <div class="field">
                    <label for="liveMuscleGroup">Muscle group</label>
                    <input id="liveMuscleGroup" name="muscleGroup" maxlength="120">
                </div>
                <div class="field">
                    <label for="liveRoutineSnapshotId">Routine snapshot</label>
                    ${renderSessionRoutineSelect(session)}
                </div>
                <div class="field">
                    <label for="livePlannedSets">Planned sets</label>
                    <input id="livePlannedSets" name="plannedSets" type="number" min="1">
                </div>
                <div class="field">
                    <label for="livePlannedReps">Planned reps</label>
                    <input id="livePlannedReps" name="plannedReps" type="number" min="1">
                </div>
            </div>
            <div class="field">
                <label for="liveExerciseNotes">Notes</label>
                <input id="liveExerciseNotes" name="notes" maxlength="500">
            </div>
            <button class="button primary" type="submit">Add live exercise</button>
        </form>
    `;
}

function renderSessionRoutineSelect(session) {
    return `
        <select id="liveRoutineSnapshotId" name="sessionRoutineSnapshotId">
            <option value="">No routine</option>
            ${session.routines.map(routine => `
                <option value="${routine.id}">${escapeHtml(routine.routineName)}</option>
            `).join("")}
        </select>
    `;
}

function renderLiveExerciseList(session) {
    if (!session.exercises.length) {
        return `<div class="empty">No exercise snapshots in this session</div>`;
    }

    return `
        <div class="list">
            ${session.exercises.map(exercise => `
                <article class="item">
                    <div class="item-row">
                        <div>
                            <div class="item-title">${escapeHtml(exercise.exerciseName)}</div>
                            <div class="item-meta">${escapeHtml(getSessionExerciseMeta(exercise))}</div>
                            <div class="badge-row section">
                                <span class="badge blue">${exercise.sourceTemplateExerciseId ? "Copied from template" : "Extra live exercise"}</span>
                                <span class="badge">Session snapshot</span>
                            </div>
                        </div>
                        <span class="badge amber">${exercise.sets.length} sets</span>
                    </div>
                    <div class="section">${renderWorkoutSets(exercise.sets)}</div>
                    <form class="exercise-log section setForm" data-exercise-id="${exercise.id}">
                        <div class="field">
                            <label>Set</label>
                            <input name="setNumber" type="number" min="1" value="${exercise.sets.length + 1}" required>
                        </div>
                        <div class="field">
                            <label>Reps</label>
                            <input name="reps" type="number" min="1">
                        </div>
                        <div class="field">
                            <label>Weight</label>
                            <input name="weight" type="number" min="0" step="0.01">
                        </div>
                        <div class="field">
                            <label>Notes</label>
                            <input name="notes" maxlength="500">
                        </div>
                        <button class="button primary" type="submit">Log set</button>
                    </form>
                </article>
            `).join("")}
        </div>
    `;
}

function renderWorkoutSets(sets) {
    if (!sets.length) {
        return `<div class="empty">No sets logged yet</div>`;
    }

    return `
        <div class="list">
            ${sets.map(set => `
                <div class="set-row">
                    <strong>Set ${set.setNumber}</strong>
                    <span class="item-meta">${escapeHtml(getSetMeta(set))}</span>
                    <button class="button danger" data-action="delete-set" data-set-id="${set.id}">Delete set</button>
                </div>
            `).join("")}
        </div>
    `;
}

function renderReadOnlySession(session) {
    if (!session.exercises.length) {
        return `<div class="empty">No snapshot exercises</div>`;
    }

    return `
        <div class="list">
            ${session.exercises.map(exercise => `
                <div class="item">
                    <div class="item-row">
                        <div>
                            <div class="item-title">${escapeHtml(exercise.exerciseName)}</div>
                            <div class="item-meta">${escapeHtml(getSessionExerciseMeta(exercise))}</div>
                        </div>
                        <span class="badge blue">${exercise.sourceTemplateExerciseId ? "Template snapshot" : "Live snapshot"}</span>
                    </div>
                    <div class="section">${renderReadOnlySets(exercise.sets)}</div>
                </div>
            `).join("")}
        </div>
    `;
}

function renderReadOnlySets(sets) {
    if (!sets.length) {
        return `<div class="empty">No logged sets</div>`;
    }

    return `<div class="list">${sets.map(set => renderPlainItem(`Set ${set.setNumber}`, getSetMeta(set))).join("")}</div>`;
}

function renderHistoryList() {
    if (!state.data.sessions.length) {
        return `<div class="empty">No workout sessions yet</div>`;
    }

    return `
        <div class="list">
            ${state.data.sessions.map(session => `
                <button class="item" data-action="select-history-session" data-session-id="${session.id}">
                    <div class="item-row">
                        <div>
                            <div class="item-title">${escapeHtml(session.templateName)}</div>
                            <div class="item-meta">${escapeHtml(session.status)} - ${formatDate(session.startedAt)}</div>
                        </div>
                        <span class="badge ${session.status === "IN_PROGRESS" ? "amber" : "blue"}">${session.status === "IN_PROGRESS" ? "Live" : "Summary"}</span>
                    </div>
                </button>
            `).join("")}
        </div>
    `;
}

function renderSessionList(sessions) {
    if (!sessions.length) {
        return `<div class="empty">No sessions yet</div>`;
    }

    return `
        <div class="list">
            ${sessions.map(session => `
                <div class="item">
                    <div class="item-row">
                        <div>
                            <div class="item-title">${escapeHtml(session.templateName)}</div>
                            <div class="item-meta">${escapeHtml(session.status)} - ${formatDate(session.startedAt)}</div>
                        </div>
                        <a class="button" href="#/${session.status === "IN_PROGRESS" ? "live-workout" : "summary"}/${session.id}">
                            ${session.status === "IN_PROGRESS" ? "Resume" : "Summary"}
                        </a>
                    </div>
                </div>
            `).join("")}
        </div>
    `;
}

function renderPlainItem(title, meta, badgeClass = "") {
    return `
        <div class="item">
            <div class="item-row">
                <div>
                    <div class="item-title">${escapeHtml(title || "Untitled")}</div>
                    <div class="item-meta">${escapeHtml(meta || "")}</div>
                </div>
                <span class="badge ${badgeClass}">${escapeHtml((title || "I").slice(0, 1).toUpperCase())}</span>
            </div>
        </div>
    `;
}

function renderSelect(id, name, items, emptyLabel, selectedValue = "", labelField = "name") {
    return `
        <select id="${id}" name="${name}">
            <option value="">${escapeHtml(emptyLabel)}</option>
            ${items.map(item => `
                <option value="${item.id}" ${String(selectedValue || "") === String(item.id) ? "selected" : ""}>
                    ${escapeHtml(item[labelField] || item.name || "Untitled")}
                </option>
            `).join("")}
        </select>
    `;
}

async function handleDocumentClick(event) {
    const target = event.target.closest("[data-action]");

    if (!target) {
        return;
    }

    const action = target.dataset.action;

    if (action === "auth-mode") {
        state.authMode = target.dataset.mode;
        state.error = null;
        render();
        return;
    }

    if (action === "toggle-theme") {
        toggleTheme();
        return;
    }

    if (action === "logout") {
        logout();
        return;
    }

    if (action === "refresh") {
        await withLoading(async () => loadData(), "Refreshed");
        return;
    }

    if (action === "select-template") {
        await selectTemplate(Number(target.dataset.templateId));
        return;
    }

    if (action === "start-routine") {
        await updateRoutineState(target.dataset.routineId, "start");
        return;
    }

    if (action === "end-routine") {
        await updateRoutineState(target.dataset.routineId, "end");
        return;
    }

    if (action === "delete-set") {
        await deleteWorkoutSet(Number(target.dataset.setId));
        return;
    }

    if (action === "finish-workout") {
        await finishWorkout(Number(target.dataset.sessionId));
        return;
    }

    if (action === "select-history-session") {
        await selectHistorySession(Number(target.dataset.sessionId));
    }
}

async function handleDocumentSubmit(event) {
    event.preventDefault();

    const id = event.target.id;

    if (id === "loginForm") {
        await login(event.target);
        return;
    }

    if (id === "registerForm") {
        await register(event.target);
        return;
    }

    if (id === "gymForm") {
        await createResource("/api/gyms", getFormData(event.target), "Gym saved");
        return;
    }

    if (id === "equipmentForm") {
        await createResource("/api/equipment", normalizePayload(getFormData(event.target)), "Equipment saved");
        return;
    }

    if (id === "exerciseForm") {
        await createResource("/api/exercises", normalizePayload(getFormData(event.target)), "Exercise saved");
        return;
    }

    if (id === "routineForm") {
        await createResource("/api/routines", getFormData(event.target), "Routine saved");
        return;
    }

    if (id === "templateForm") {
        await createTemplate(event.target);
        return;
    }

    if (id === "templateRoutineForm") {
        await addTemplateRoutine(event.target);
        return;
    }

    if (id === "templateExerciseForm") {
        await addTemplateExercise(event.target);
        return;
    }

    if (id === "startWorkoutForm") {
        await startWorkout(event.target);
        return;
    }

    if (id === "quickEquipmentForm") {
        await quickAddEquipment(event.target);
        return;
    }

    if (id === "liveExerciseForm") {
        await addLiveExercise(event.target);
        return;
    }

    if (event.target.classList.contains("setForm")) {
        await logSet(event.target);
    }
}

async function handleDocumentChange(event) {
    if (event.target.id === "selectedTemplateId") {
        await selectTemplate(Number(event.target.value));
    }
}

async function login(form) {
    const payload = getFormData(form);

    await withLoading(async () => {
        await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        saveCredentials(payload.email, payload.password);
        state.user = await api("/api/auth/me");
        await loadData();
        navigateTo("dashboard");
    }, "Logged in");
}

async function register(form) {
    const payload = getFormData(form);

    await withLoading(async () => {
        await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        saveCredentials(payload.email, payload.password);
        state.user = await api("/api/auth/me");
        await loadData();
        navigateTo("dashboard");
    }, "Account created");
}

async function createResource(path, payload, message) {
    await withLoading(async () => {
        await api(path, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        await loadData();
    }, message);
}

async function createTemplate(form) {
    await withLoading(async () => {
        const template = await api("/api/workout-templates", {
            method: "POST",
            body: JSON.stringify(normalizePayload(getFormData(form)))
        });

        state.selectedTemplateId = template.id;
        await loadData();
    }, "Template saved");
}

async function addTemplateRoutine(form) {
    if (!state.selectedTemplateId) {
        return;
    }

    await withLoading(async () => {
        await api(`/api/workout-templates/${state.selectedTemplateId}/routines`, {
            method: "POST",
            body: JSON.stringify(normalizePayload(getFormData(form)))
        });
        await loadData();
    }, "Routine added to template");
}

async function addTemplateExercise(form) {
    if (!state.selectedTemplateId) {
        return;
    }

    await withLoading(async () => {
        await api(`/api/workout-templates/${state.selectedTemplateId}/exercises`, {
            method: "POST",
            body: JSON.stringify(normalizePayload(getFormData(form)))
        });
        await loadData();
    }, "Exercise added to template");
}

async function startWorkout(form) {
    const payload = normalizePayload(getFormData(form));

    if (!payload.workoutTemplateId) {
        showToast("Select a workout template", true);
        return;
    }

    await withLoading(async () => {
        const session = await api("/api/workout-sessions", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        state.currentSession = session;
        await loadData();
        navigateTo(`live-workout/${session.id}`);
    }, "Workout started");
}

async function quickAddEquipment(form) {
    const payload = normalizePayload(getFormData(form));

    if (!payload.name) {
        showToast("Equipment name is required", true);
        return;
    }

    await withLoading(async () => {
        await api("/api/equipment", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        await loadData();
    }, "Equipment added");
}

async function addLiveExercise(form) {
    const payload = normalizePayload(getFormData(form));
    let exerciseId = payload.exerciseId;

    await withLoading(async () => {
        if (!exerciseId) {
            if (!payload.name) {
                throw new Error("Select an exercise or enter a new exercise name");
            }

            const exercise = await api("/api/exercises", {
                method: "POST",
                body: JSON.stringify({
                    equipmentId: payload.equipmentId,
                    name: payload.name,
                    muscleGroup: payload.muscleGroup,
                    instructions: null
                })
            });

            exerciseId = exercise.id;
        }

        const session = await api(`/api/workout-sessions/${state.currentSession.id}/exercises`, {
            method: "POST",
            body: JSON.stringify({
                sessionRoutineSnapshotId: payload.sessionRoutineSnapshotId,
                exerciseId,
                notes: payload.notes,
                sortOrder: null,
                plannedSets: payload.plannedSets,
                plannedReps: payload.plannedReps,
                plannedWeight: null,
                plannedDurationSeconds: null
            })
        });

        state.currentSession = session;
        await loadData();
    }, "Live exercise added");
}

async function logSet(form) {
    const payload = normalizePayload(getFormData(form));
    payload.sessionExerciseSnapshotId = Number(form.dataset.exerciseId);

    await withLoading(async () => {
        await api(`/api/workout-sessions/${state.currentSession.id}/sets`, {
            method: "POST",
            body: JSON.stringify(payload)
        });
        state.currentSession = await api(`/api/workout-sessions/${state.currentSession.id}`);
    }, "Set logged");
}

async function updateRoutineState(routineId, action) {
    await withLoading(async () => {
        state.currentSession = await api(
                `/api/workout-sessions/${state.currentSession.id}/routines/${routineId}/${action}`,
                { method: "POST" }
        );
    }, action === "start" ? "Routine started" : "Routine ended");
}

async function deleteWorkoutSet(setId) {
    await withLoading(async () => {
        state.currentSession = await api(
                `/api/workout-sessions/${state.currentSession.id}/sets/${setId}`,
                { method: "DELETE" }
        );
    }, "Set deleted");
}

async function finishWorkout(sessionId) {
    await withLoading(async () => {
        const session = await api(`/api/workout-sessions/${sessionId}/complete`, {
            method: "POST"
        });
        state.historySession = session;
        await loadData();
        navigateTo(`summary/${session.id}`);
    }, "Workout finished");
}

async function selectTemplate(templateId) {
    if (!templateId) {
        return;
    }

    await withLoading(async () => {
        state.selectedTemplateId = templateId;
        state.selectedTemplate = await api(`/api/workout-templates/${templateId}`);
    });
}

async function selectHistorySession(sessionId) {
    await withLoading(async () => {
        state.selectedHistorySessionId = sessionId;
        state.historySession = await api(`/api/workout-sessions/${sessionId}`);
    });
}

async function withLoading(callback, successMessage) {
    startLoading();

    try {
        await callback();
        state.error = null;

        if (successMessage) {
            showToast(successMessage);
        }
    } catch (error) {
        state.error = error.message;
        showToast(error.message, true);
    } finally {
        stopLoading();
    }
}

function startLoading() {
    state.loading = true;
    render();
}

function stopLoading() {
    state.loading = false;
    render();
}

function handleRouteChange() {
    state.route = getRoute();
    state.error = null;

    if (!state.user && state.route.name !== "auth") {
        navigateTo("auth");
        return;
    }

    withLoading(async () => {
        await loadRouteDetails();
    });
}

function getRoute() {
    const raw = location.hash.replace(/^#\/?/, "") || "dashboard";
    const parts = raw.split("/").filter(Boolean);

    return {
        name: parts[0] || "dashboard",
        id: parts[1] ? Number(parts[1]) : null
    };
}

function navigateTo(route) {
    location.hash = `#/${route}`;
}

function getPageCopy() {
    const copy = {
        dashboard: {
            kicker: "Overview",
            title: "Dashboard",
            lead: "Core workout data, session state, and snapshot integrity at a glance."
        },
        "gyms-equipment": {
            kicker: "Environment",
            title: "Gyms & Equipment",
            lead: "Manage places and equipment used by exercises and workout snapshots."
        },
        exercises: {
            kicker: "Library",
            title: "Exercises",
            lead: "Reusable exercise definitions. Sessions copy these values when workouts start."
        },
        routines: {
            kicker: "Library",
            title: "Routines",
            lead: "Reusable routine definitions for template planning."
        },
        templates: {
            kicker: "Plans",
            title: "Templates",
            lead: "Reusable plans built from routines and exercises."
        },
        "start-workout": {
            kicker: "Session",
            title: "Start Workout",
            lead: "Turn a reusable template into a real workout session."
        },
        "live-workout": {
            kicker: "Live",
            title: "Live Workout",
            lead: "Work from session snapshots, log sets, and finish the workout."
        },
        summary: {
            kicker: "Cooldown",
            title: "Workout Summary",
            lead: "Completed workout detail read from historical snapshots."
        },
        history: {
            kicker: "Archive",
            title: "Workout History",
            lead: "Past sessions and their snapshot-preserved details."
        }
    };

    return copy[state.route.name] || copy.dashboard;
}

function getActiveSessionButton() {
    const activeSession = state.data.sessions.find(session => session.status === "IN_PROGRESS");

    if (!activeSession) {
        return "";
    }

    return `<a class="button primary" href="#/live-workout/${activeSession.id}">Resume live workout</a>`;
}

function getTemplateExerciseMeta(exercise) {
    return [
        exercise.equipmentName,
        exercise.plannedSets ? `${exercise.plannedSets} sets` : null,
        exercise.plannedReps ? `${exercise.plannedReps} reps` : null,
        exercise.plannedWeight ? `${exercise.plannedWeight} kg` : null
    ].filter(Boolean).join(" - ") || "No plan details";
}

function getSessionExerciseMeta(exercise) {
    return [
        exercise.muscleGroup,
        exercise.equipmentName,
        exercise.plannedSets ? `${exercise.plannedSets} planned sets` : null,
        exercise.plannedReps ? `${exercise.plannedReps} planned reps` : null,
        exercise.plannedWeight ? `${exercise.plannedWeight} kg planned` : null
    ].filter(Boolean).join(" - ") || "Snapshot exercise";
}

function getSetMeta(set) {
    return [
        set.reps ? `${set.reps} reps` : null,
        set.weight ? `${set.weight} kg` : null,
        set.durationSeconds ? `${set.durationSeconds}s` : null,
        set.notes
    ].filter(Boolean).join(" - ") || "Logged set";
}

function getTotalSets(session) {
    return session.exercises.reduce((sum, exercise) => sum + exercise.sets.length, 0);
}

function getFormData(form) {
    const formData = new FormData(form);
    const payload = {};

    formData.forEach((value, key) => {
        payload[key] = value === "" ? null : value;
    });

    return payload;
}

function normalizePayload(payload) {
    const normalized = { ...payload };

    Object.keys(normalized).forEach(key => {
        if (normalized[key] === null || normalized[key] === "") {
            normalized[key] = null;
            return;
        }

        if (
            key.endsWith("Id") ||
            key === "sortOrder" ||
            key === "setNumber" ||
            key === "reps" ||
            key === "plannedSets" ||
            key === "plannedReps" ||
            key === "plannedDurationSeconds" ||
            key === "durationSeconds"
        ) {
            normalized[key] = Number(normalized[key]);
        }

        if (key === "weight" || key === "plannedWeight") {
            normalized[key] = Number(normalized[key]);
        }
    });

    return normalized;
}

function saveCredentials(email, password) {
    state.credentials = { email, password };
    localStorage.setItem("workout.credentials", JSON.stringify(state.credentials));
}

function loadCredentials() {
    const stored = localStorage.getItem("workout.credentials");

    if (!stored) {
        return null;
    }

    try {
        return JSON.parse(stored);
    } catch (error) {
        return null;
    }
}

function clearCredentials() {
    state.credentials = null;
    localStorage.removeItem("workout.credentials");
}

function logout() {
    clearCredentials();
    state.user = null;
    state.route = { name: "auth", id: null };
    navigateTo("auth");
    render();
}

function toggleTheme() {
    state.theme = state.theme === "dark" ? "light" : "dark";
    localStorage.setItem("workout.theme", state.theme);
    document.documentElement.dataset.theme = state.theme;
    render();
}

function showToast(message, isError = false) {
    state.toast = { message, isError };

    window.clearTimeout(showToast.timeoutId);
    showToast.timeoutId = window.setTimeout(() => {
        state.toast = null;
        renderToast();
    }, 3200);
}

function renderToast() {
    const oldToast = document.querySelector(".toast");

    if (oldToast) {
        oldToast.remove();
    }

    if (!state.toast) {
        return;
    }

    const toast = document.createElement("div");
    toast.className = `toast ${state.toast.isError ? "error" : ""}`;
    toast.textContent = state.toast.message;
    document.body.appendChild(toast);
}

function formatDate(value) {
    if (!value) {
        return "No date";
    }

    return new Intl.DateTimeFormat("en", {
        month: "short",
        day: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function escapeHtml(value) {
    return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
}
