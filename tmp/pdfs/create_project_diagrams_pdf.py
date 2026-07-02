from __future__ import annotations

import math
import re
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.units import cm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas


ROOT = Path(r"C:\Users\Alexandru\Desktop\Licenta")
OUT = ROOT / "output" / "pdf" / "project_diagrams_study_guide.pdf"
V1 = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V1__full_initial_schema.sql"
V2 = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V2__seed_official_exercises.sql"
V3 = ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V3__one_active_workout_per_user.sql"
JAVA_ROOT = ROOT / "backend" / "src" / "main" / "java" / "com" / "thesis" / "workout"

PAGE_SIZE = landscape(A4)
W, H = PAGE_SIZE
M = 28

FONT = "TimesNewRoman"
FONT_BOLD = "TimesNewRoman-Bold"
FONT_ITALIC = "TimesNewRoman-Italic"
FONT_MONO = "Consolas"

NAVY = colors.HexColor("#1f2a44")
BLUE = colors.HexColor("#2f6f9f")
BLUE_FILL = colors.HexColor("#e8f2f8")
GREEN = colors.HexColor("#3c7a57")
GREEN_FILL = colors.HexColor("#e8f5ec")
AMBER = colors.HexColor("#9a6b19")
AMBER_FILL = colors.HexColor("#fff4df")
PURPLE = colors.HexColor("#74539b")
PURPLE_FILL = colors.HexColor("#f1ebf7")
RED = colors.HexColor("#b23a48")
RED_FILL = colors.HexColor("#fae9ec")
GRAY = colors.HexColor("#697586")
LIGHT = colors.HexColor("#f5f7fa")
LINE = colors.HexColor("#415065")


EXPECTED_TABLES = {
    "app_users", "refresh_tokens", "coach_profiles", "coach_client_relationships",
    "muscle_groups", "exercises", "exercise_muscle_groups", "routines", "gyms", "equipment",
    "workout_templates", "template_stats", "template_days", "template_day_exercises",
    "template_day_exercise_muscle_groups", "template_day_routines", "workout_sessions",
    "session_exercises", "session_exercise_muscle_groups", "session_routines", "workout_sets",
    "coach_session_comments", "template_votes", "template_saves", "template_use_events",
    "coach_template_assignments", "template_analysis_results", "outbox_events",
}

EXPECTED_ENDPOINTS = {
    "GET /api/health",
    "POST /api/auth/register",
    "POST /api/auth/login",
    "POST /api/auth/refresh",
    "POST /api/auth/logout",
    "GET /api/auth/me",
    "GET /api/muscle-groups",
    "GET /api/exercises",
    "GET /api/exercises/official",
    "GET /api/exercises/custom",
    "POST /api/exercises/custom",
    "PUT /api/exercises/custom/{exerciseId}",
    "DELETE /api/exercises/custom/{exerciseId}",
    "GET /api/routines",
    "POST /api/routines",
    "PUT /api/routines/{routineId}",
    "DELETE /api/routines/{routineId}",
    "GET /api/gyms",
    "GET /api/gyms/{gymId}",
    "POST /api/gyms",
    "PUT /api/gyms/{gymId}",
    "DELETE /api/gyms/{gymId}",
    "GET /api/gyms/{gymId}/equipment",
    "POST /api/gyms/{gymId}/equipment",
    "PUT /api/equipment/{equipmentId}",
    "DELETE /api/equipment/{equipmentId}",
    "GET /api/templates",
    "GET /api/templates/{templateId}",
    "POST /api/templates",
    "PUT /api/templates/{templateId}",
    "DELETE /api/templates/{templateId}",
    "POST /api/templates/{templateId}/publish",
    "POST /api/templates/{templateId}/unpublish",
    "GET /api/templates/{templateId}/analysis",
    "POST /api/templates/{templateId}/days",
    "PUT /api/template-days/{templateDayId}",
    "DELETE /api/template-days/{templateDayId}",
    "POST /api/template-days/{templateDayId}/exercises",
    "PUT /api/template-day-exercises/{templateDayExerciseId}",
    "DELETE /api/template-day-exercises/{templateDayExerciseId}",
    "POST /api/template-days/{templateDayId}/routines",
    "DELETE /api/template-day-routines/{templateDayRoutineId}",
    "POST /api/workouts/start",
    "GET /api/workouts/active",
    "GET /api/workouts/{sessionId}",
    "POST /api/workouts/{sessionId}/finish",
    "POST /api/workouts/{sessionId}/cancel",
    "POST /api/workouts/{sessionId}/extra-exercises",
    "PUT /api/workouts/{sessionId}/notes",
    "POST /api/session-exercises/{sessionExerciseId}/sets",
    "PUT /api/sets/{setId}",
    "DELETE /api/sets/{setId}",
    "GET /api/history",
    "GET /api/analytics/overview",
    "GET /api/marketplace/templates",
    "GET /api/marketplace/templates/{templateId}",
    "POST /api/marketplace/templates/{templateId}/vote",
    "DELETE /api/marketplace/templates/{templateId}/vote",
    "POST /api/marketplace/templates/{templateId}/save",
    "DELETE /api/marketplace/templates/{templateId}/save",
    "POST /api/marketplace/templates/{templateId}/use",
    "GET /api/search/templates",
    "GET /api/search/workouts",
    "POST /api/admin/search/reindex",
    "GET /api/coach/clients",
    "POST /api/coach/clients/invite",
    "DELETE /api/coach/clients/{clientId}",
    "GET /api/coach/clients/{clientId}/history",
    "GET /api/coach/clients/{clientId}/analytics",
    "GET /api/coach/clients/{clientId}/sessions/{sessionId}",
    "GET /api/coach/clients/{clientId}/search/workouts",
    "GET /api/coaching/invites",
    "POST /api/coaching/invites/{relationshipId}/accept",
    "POST /api/coaching/invites/{relationshipId}/reject",
    "GET /api/coaching/coaches",
    "DELETE /api/coaching/coaches/{relationshipId}",
}


def register_fonts() -> None:
    global FONT_MONO
    pdfmetrics.registerFont(TTFont(FONT, r"C:\Windows\Fonts\times.ttf"))
    pdfmetrics.registerFont(TTFont(FONT_BOLD, r"C:\Windows\Fonts\timesbd.ttf"))
    pdfmetrics.registerFont(TTFont(FONT_ITALIC, r"C:\Windows\Fonts\timesi.ttf"))
    consolas = Path(r"C:\Windows\Fonts\consola.ttf")
    if consolas.exists():
        pdfmetrics.registerFont(TTFont(FONT_MONO, str(consolas)))
    else:
        FONT_MONO = "Courier"


def parse_tables() -> set[str]:
    return set(re.findall(r"CREATE TABLE\s+([a-zA-Z_][a-zA-Z0-9_]*)\s*\(", V1.read_text(encoding="utf-8")))


def parse_endpoints() -> set[str]:
    endpoints: set[str] = set()
    method_map = {
        "GetMapping": "GET",
        "PostMapping": "POST",
        "PutMapping": "PUT",
        "DeleteMapping": "DELETE",
        "PatchMapping": "PATCH",
    }
    for path in JAVA_ROOT.rglob("*Controller.java"):
        text = path.read_text(encoding="utf-8")
        class_base = ""
        req = re.search(r"@RequestMapping\(\s*\"([^\"]*)\"", text)
        if req:
            class_base = req.group(1)
        for ann, method in method_map.items():
            for match in re.finditer(rf"@{ann}(?:\(\s*\"([^\"]*)\"\s*\))?", text):
                suffix = match.group(1) or ""
                route = (class_base.rstrip("/") + "/" + suffix.lstrip("/")).rstrip("/")
                if not route:
                    route = "/"
                endpoints.add(f"{method} {route}")
    return endpoints


def validate_sources() -> tuple[int, int]:
    tables = parse_tables()
    missing_tables = sorted(EXPECTED_TABLES - tables)
    extra_tables = sorted(tables - EXPECTED_TABLES)
    if missing_tables or extra_tables:
        raise RuntimeError(f"DB table mismatch. Missing={missing_tables}; extra={extra_tables}")

    endpoints = parse_endpoints()
    missing_endpoints = sorted(EXPECTED_ENDPOINTS - endpoints)
    if missing_endpoints:
        raise RuntimeError(f"Endpoint mismatch. Missing={missing_endpoints}")

    v2_text = V2.read_text(encoding="utf-8")
    exercises_insert = re.search(
        r"INSERT INTO exercises\s*\([^)]*\)\s*VALUES\s*(.*?)\s*ON CONFLICT",
        v2_text,
        flags=re.DOTALL,
    )
    if not exercises_insert:
        raise RuntimeError("Could not locate V2 official exercise insert")
    official_count = len(re.findall(r"\('00000000-0000-0000-0000-0000000001\d\d'", exercises_insert.group(1)))
    if official_count != 14:
        raise RuntimeError(f"Expected 14 official seeded exercises, found {official_count}")
    if "ux_workout_sessions_one_active_per_user" not in V3.read_text(encoding="utf-8"):
        raise RuntimeError("V3 one-active-workout unique index was not found")
    return len(tables), len(endpoints)


def width(text: str, font: str, size: float) -> float:
    return pdfmetrics.stringWidth(text, font, size)


def wrap(text: str, max_w: float, font: str = FONT, size: float = 8) -> list[str]:
    out: list[str] = []
    for raw in str(text).split("\n"):
        words = raw.split()
        if not words:
            out.append("")
            continue
        line = words[0]
        for word in words[1:]:
            candidate = f"{line} {word}"
            if width(candidate, font, size) <= max_w:
                line = candidate
            else:
                out.append(line)
                line = word
        out.append(line)
    return out


def header(c: canvas.Canvas, title: str, page: int) -> None:
    c.setFillColor(NAVY)
    c.setFont(FONT_BOLD, 12)
    c.drawString(M, H - 22, title)
    c.setFont(FONT, 8)
    c.setFillColor(GRAY)
    c.drawRightString(W - M, H - 20, f"Pagina {page}")
    c.setStrokeColor(colors.HexColor("#d6dce5"))
    c.line(M, H - 30, W - M, H - 30)


def footer(c: canvas.Canvas, note: str = "Ghid vizual derivat din codul proiectului") -> None:
    c.setStrokeColor(colors.HexColor("#d6dce5"))
    c.line(M, 24, W - M, 24)
    c.setFillColor(GRAY)
    c.setFont(FONT, 7)
    c.drawString(M, 12, note)


def bullet_list(c: canvas.Canvas, x: float, y: float, items: list[str], max_w: float,
                size: float = 8.3, leading: float = 10.5, color=colors.black) -> float:
    c.setFillColor(color)
    for item in items:
        lines = wrap(item, max_w - 10, FONT, size)
        c.setFont(FONT, size)
        c.drawString(x, y, "-")
        for i, line in enumerate(lines):
            c.drawString(x + 10, y - i * leading, line)
        y -= max(1, len(lines)) * leading + 1.5
    return y


def note_box(c: canvas.Canvas, x: float, y: float, w: float, h: float, title: str,
             body: list[str], fill=LIGHT, stroke=colors.HexColor("#cbd5e1")) -> None:
    c.setFillColor(fill)
    c.setStrokeColor(stroke)
    c.roundRect(x, y, w, h, 6, stroke=1, fill=1)
    c.setFillColor(NAVY)
    c.setFont(FONT_BOLD, 9.5)
    c.drawString(x + 8, y + h - 15, title)
    cy = y + h - 29
    c.setFillColor(colors.black)
    for item in body:
        cy = bullet_list(c, x + 8, cy, [item], w - 16, 7.5, 9.2)


def table_box(c: canvas.Canvas, x: float, y: float, w: float, h: float, title: str,
              rows: list[str], fill=BLUE_FILL, stroke=BLUE, small: bool = False) -> dict:
    c.setFillColor(fill)
    c.setStrokeColor(stroke)
    c.roundRect(x, y, w, h, 5, stroke=1, fill=1)
    c.setFillColor(stroke)
    c.setFont(FONT_BOLD, 8.7 if not small else 7.8)
    c.drawString(x + 6, y + h - 12, title)
    c.setFillColor(colors.black)
    c.setFont(FONT, 6.8 if not small else 6.0)
    cy = y + h - 23
    for row in rows:
        for line in wrap(row, w - 12, FONT, 6.8 if not small else 6.0):
            if cy < y + 5:
                c.setFillColor(GRAY)
                c.drawString(x + 6, cy, "...")
                return {"x": x, "y": y, "w": w, "h": h}
            c.drawString(x + 6, cy, line)
            cy -= 7.2 if not small else 6.5
    return {"x": x, "y": y, "w": w, "h": h}


def center(box: dict) -> tuple[float, float]:
    return box["x"] + box["w"] / 2, box["y"] + box["h"] / 2


def side(box: dict, where: str) -> tuple[float, float]:
    x, y, w, h = box["x"], box["y"], box["w"], box["h"]
    return {
        "left": (x, y + h / 2),
        "right": (x + w, y + h / 2),
        "top": (x + w / 2, y + h),
        "bottom": (x + w / 2, y),
    }[where]


def arrow(c: canvas.Canvas, start: tuple[float, float], end: tuple[float, float],
          label: str = "", color=LINE, dash: bool = False) -> None:
    x1, y1 = start
    x2, y2 = end
    c.setStrokeColor(color)
    c.setFillColor(color)
    c.setLineWidth(0.8)
    if dash:
        c.setDash(3, 2)
    c.line(x1, y1, x2, y2)
    if dash:
        c.setDash()
    angle = math.atan2(y2 - y1, x2 - x1)
    size = 5
    p1 = (x2 - size * math.cos(angle - math.pi / 6), y2 - size * math.sin(angle - math.pi / 6))
    p2 = (x2 - size * math.cos(angle + math.pi / 6), y2 - size * math.sin(angle + math.pi / 6))
    c.line(x2, y2, p1[0], p1[1])
    c.line(x2, y2, p2[0], p2[1])
    if label:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        c.setFont(FONT, 6.4)
        c.setFillColor(GRAY)
        c.drawCentredString(mx, my + 3, label)


def flow_box(c: canvas.Canvas, x: float, y: float, w: float, h: float, title: str,
             body: list[str], fill=LIGHT, stroke=LINE) -> dict:
    return table_box(c, x, y, w, h, title, body, fill=fill, stroke=stroke)


def title_page(c: canvas.Canvas, table_count: int, endpoint_count: int) -> None:
    c.setFillColor(NAVY)
    c.setFont(FONT_BOLD, 27)
    c.drawString(58, 430, "Ghid vizual pentru licență")
    c.setFont(FONT_BOLD, 15)
    c.drawString(58, 402, "Diagrame corecte după cod pentru platforma de antrenamente")
    c.setFont(FONT, 10.5)
    c.setFillColor(colors.black)
    y = 360
    intro = [
        "Scop: să poți explica rapid arhitectura, schema bazei de date, fluxul de snapshot-uri, securitatea, OpenSearch, marketplace-ul, coaching-ul și analizatorul.",
        f"Validare automată la generare: {table_count} tabele găsite în Flyway V1, 14 exerciții oficiale în V2, indexul unic din V3 și {endpoint_count} endpoint-uri citite din controllere.",
        "Baza de date este împărțită în două diagrame pentru lizibilitate: nucleul aplicației și partea de auth/coaching/marketplace/suport.",
        "Important: OpenSearch este model de citire derivat, PostgreSQL rămâne sursa de adevăr. Redis nu este implementat în proiectul curent.",
    ]
    y = bullet_list(c, 58, y, intro, 720, 10, 13)
    note_box(c, 58, 90, 710, 120, "Surse verificate din proiect", [
        "Migrations: V1__full_initial_schema.sql, V2__seed_official_exercises.sql, V3__one_active_workout_per_user.sql.",
        "Controllere: Auth, Exercises, Routines, Gyms, Templates, Workouts/Sets, History, Analytics, Marketplace, Analyzer, Search, Coach/Coaching.",
        "Servicii citite pentru fluxuri: AuthService, WorkoutSessionService, TemplateCopyService, TemplateInteractionService, TemplatePublishingService, TemplateAnalyzerService, SearchRebuildService, SearchIndexEventListener, CoachAccess.",
    ], fill=colors.HexColor("#eef4ff"), stroke=BLUE)
    footer(c)


def architecture_page(c: canvas.Canvas) -> None:
    header(c, "1. Arhitectura aplicației", 2)
    fe = flow_box(c, 45, 382, 170, 95, "Frontend React + TypeScript", [
        "Vite SPA, dashboard UI",
        "features/*: auth, templates, workouts, history, analytics, marketplace, search, coaching",
        "AuthProvider + api/client"
    ], BLUE_FILL, BLUE)
    api = flow_box(c, 275, 395, 145, 70, "REST API", [
        "Authorization: Bearer access JWT",
        "Refresh cookie HttpOnly",
        "DTO request/response"
    ], LIGHT, LINE)
    sec = flow_box(c, 475, 395, 140, 70, "Security filter", [
        "JwtAuthenticationFilter",
        "stateless sessions",
        "RBAC routes"
    ], PURPLE_FILL, PURPLE)
    be = flow_box(c, 250, 230, 220, 115, "Spring Boot backend", [
        "Controllers -> application services -> repositories",
        "Domain modules: auth, exercise, routine, gym, template, session, history, analytics, marketplace, analyzer, search, coaching",
        "Ownership/ReBAC checks in services"
    ], GREEN_FILL, GREEN)
    pg = flow_box(c, 560, 250, 170, 90, "PostgreSQL 16", [
        "Sursa de adevăr",
        "Flyway only",
        "FK, CHECK, unique/partial indexes",
        "soft delete where needed"
    ], AMBER_FILL, AMBER)
    os = flow_box(c, 560, 115, 170, 92, "OpenSearch 2.13", [
        "model de citire derivat",
        "aliases: templates, workout_sessions",
        "full-text, fuzzy, facets, highlights",
        "rebuild din PostgreSQL"
    ], RED_FILL, RED)
    fly = flow_box(c, 45, 245, 150, 80, "Flyway migrations", [
        "V1: schema completă",
        "V2: seed exerciții oficiale",
        "V3: un workout activ/user"
    ], AMBER_FILL, AMBER)
    tests = flow_box(c, 45, 118, 150, 80, "Testare", [
        "JUnit + Spring Boot",
        "Testcontainers PostgreSQL",
        "OpenSearch real pentru search",
        "fără H2"
    ], LIGHT, LINE)

    arrow(c, side(fe, "right"), side(api, "left"), "HTTP")
    arrow(c, side(api, "right"), side(sec, "left"), "filter")
    arrow(c, side(sec, "bottom"), side(be, "top"), "principal")
    arrow(c, side(be, "right"), side(pg, "left"), "JPA / SQL")
    arrow(c, (470, 260), side(os, "left"), "index/search", RED)
    arrow(c, side(fly, "right"), side(pg, "left"), "schema")
    arrow(c, side(tests, "right"), side(be, "left"), "integration")
    arrow(c, (620, 250), (620, 207), "derived", RED, dash=True)

    note_box(c, 235, 55, 545, 55, "Cum explici în 20 de secunde", [
        "UI-ul React vorbește cu API-ul Spring Boot. PostgreSQL este autoritativ pentru date și integritate. OpenSearch este doar un index derivat pentru căutare avansată, reconstruit din PostgreSQL când e nevoie."
    ], fill=colors.HexColor("#f8fafc"))
    footer(c)


def db_core_page(c: canvas.Canvas) -> None:
    header(c, "2. ER diagram - nucleul aplicației și istoricul", 3)
    b: dict[str, dict] = {}
    b["app_users"] = table_box(c, 35, 500, 128, 42, "app_users", ["id PK", "email unique", "role USER/COACH/ADMIN"], PURPLE_FILL, PURPLE)
    b["exercises"] = table_box(c, 35, 405, 132, 70, "exercises", ["id PK", "owner_user_id FK nullable", "visibility OFFICIAL/CUSTOM", "deleted_at"], BLUE_FILL, BLUE)
    b["exercise_muscle_groups"] = table_box(c, 35, 323, 132, 55, "exercise_muscle_groups", ["PK exercise_id + muscle", "role PRIMARY/SECONDARY"], BLUE_FILL, BLUE)
    b["muscle_groups"] = table_box(c, 35, 258, 132, 42, "muscle_groups", ["code PK", "15 coduri seed în V1"], BLUE_FILL, BLUE)
    b["routines"] = table_box(c, 205, 420, 132, 58, "routines", ["user_id FK", "routine_type START/END", "content, deleted_at"], BLUE_FILL, BLUE)
    b["gyms"] = table_box(c, 205, 340, 132, 52, "gyms", ["user_id FK", "name/location", "deleted_at"], BLUE_FILL, BLUE)
    b["equipment"] = table_box(c, 205, 270, 132, 54, "equipment", ["user_id FK", "gym_id FK", "type, notes, deleted_at"], BLUE_FILL, BLUE)

    b["workout_templates"] = table_box(c, 375, 454, 152, 72, "workout_templates", ["user_id FK", "visibility PRIVATE/PUBLIC", "copied_from_template_id", "aggregated_* arrays"], GREEN_FILL, GREEN)
    b["template_stats"] = table_box(c, 555, 462, 125, 54, "template_stats", ["template_id PK/FK", "votes/saves/uses", "rating/trending"], GREEN_FILL, GREEN)
    b["template_days"] = table_box(c, 375, 370, 152, 56, "template_days", ["template_id FK", "day_number unique/template", "focus, notes"], GREEN_FILL, GREEN)
    b["template_day_exercises"] = table_box(c, 375, 267, 152, 82, "template_day_exercises", ["template_day_id FK", "exercise_id FK nullable", "exercise_*_snapshot", "planned sets/reps/weight/rest", "position unique/day"], GREEN_FILL, GREEN)
    b["template_day_exercise_muscle_groups"] = table_box(c, 350, 190, 178, 54, "template_day_exercise_muscle_groups", ["template_day_exercise_id FK", "muscle_group_code", "role"], GREEN_FILL, GREEN, small=True)
    b["template_day_routines"] = table_box(c, 555, 308, 142, 66, "template_day_routines", ["template_day_id FK", "routine_id FK nullable", "routine_*_snapshot", "position"], GREEN_FILL, GREEN)

    b["workout_sessions"] = table_box(c, 690, 440, 125, 76, "workout_sessions", ["user_id FK", "template/day/gym FK nullable", "template/gym snapshots", "status IN_PROGRESS/FINISHED/CANCELLED"], AMBER_FILL, AMBER)
    b["session_exercises"] = table_box(c, 690, 326, 125, 82, "session_exercises", ["session_id FK", "original template/exercise FK nullable", "exercise snapshots", "planned_*_snapshot", "is_extra_exercise"], AMBER_FILL, AMBER)
    b["workout_sets"] = table_box(c, 690, 215, 125, 82, "workout_sets", ["session_exercise_id FK", "set_number unique/exercise", "weight/reps/duration/distance/RPE", "equipment snapshot"], AMBER_FILL, AMBER)
    b["session_routines"] = table_box(c, 555, 222, 142, 58, "session_routines", ["session_id FK", "original_routine_id nullable", "routine snapshots", "started/ended"], AMBER_FILL, AMBER)
    b["session_exercise_muscle_groups"] = table_box(c, 530, 138, 170, 52, "session_exercise_muscle_groups", ["session_exercise_id FK", "muscle_group_code_snapshot", "role_snapshot"], AMBER_FILL, AMBER, small=True)

    arrow(c, side(b["app_users"], "bottom"), side(b["exercises"], "top"), "owns")
    arrow(c, (100, 500), side(b["routines"], "top"), "owns")
    arrow(c, (115, 500), side(b["gyms"], "top"), "owns")
    arrow(c, (130, 500), side(b["workout_templates"], "left"), "owns")
    arrow(c, (145, 500), side(b["workout_sessions"], "left"), "owns")
    arrow(c, side(b["exercises"], "bottom"), side(b["exercise_muscle_groups"], "top"), "1:n")
    arrow(c, side(b["exercise_muscle_groups"], "bottom"), side(b["muscle_groups"], "top"), "FK")
    arrow(c, side(b["gyms"], "bottom"), side(b["equipment"], "top"), "1:n")
    arrow(c, side(b["workout_templates"], "right"), side(b["template_stats"], "left"), "1:1")
    arrow(c, side(b["workout_templates"], "bottom"), side(b["template_days"], "top"), "1:n")
    arrow(c, side(b["template_days"], "bottom"), side(b["template_day_exercises"], "top"), "1:n")
    arrow(c, side(b["template_day_exercises"], "bottom"), side(b["template_day_exercise_muscle_groups"], "top"), "1:n")
    arrow(c, side(b["template_days"], "right"), side(b["template_day_routines"], "left"), "1:n")
    arrow(c, side(b["workout_sessions"], "bottom"), side(b["session_exercises"], "top"), "1:n")
    arrow(c, side(b["session_exercises"], "bottom"), side(b["workout_sets"], "top"), "1:n")
    arrow(c, side(b["workout_sessions"], "left"), side(b["session_routines"], "right"), "1:n")
    arrow(c, side(b["session_exercises"], "left"), side(b["session_exercise_muscle_groups"], "right"), "1:n")
    arrow(c, side(b["template_day_exercises"], "right"), side(b["session_exercises"], "left"), "copied to snapshots", AMBER, dash=True)
    arrow(c, side(b["template_day_routines"], "bottom"), side(b["session_routines"], "top"), "copied", AMBER, dash=True)
    arrow(c, side(b["equipment"], "right"), side(b["workout_sets"], "left"), "equipment_id nullable", AMBER, dash=True)

    note_box(c, 35, 56, 365, 66, "Regula critică de integritate", [
        "Seturile nu se leagă direct de template_day_exercises. Lanțul corect este workout_sets -> session_exercises -> workout_sessions.",
        "Istoricul folosește snapshot-uri, nu tabelele editabile de planificare."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    note_box(c, 425, 56, 390, 66, "Constrângeri importante", [
        "Soft delete pe exerciții custom, rutine, săli, echipamente, template-uri.",
        "V3: unique partial index ux_workout_sessions_one_active_per_user WHERE status='IN_PROGRESS'."
    ], fill=colors.HexColor("#f8fafc"))
    footer(c)


def db_support_page(c: canvas.Canvas) -> None:
    header(c, "3. ER diagram - auth, coaching, marketplace și suport", 4)
    b: dict[str, dict] = {}
    b["app_users"] = table_box(c, 350, 498, 138, 43, "app_users", ["root pentru user/coach/admin", "role CHECK"], PURPLE_FILL, PURPLE)
    b["refresh_tokens"] = table_box(c, 45, 438, 140, 62, "refresh_tokens", ["user_id FK", "token_hash unique", "expires/revoked"], PURPLE_FILL, PURPLE)
    b["coach_profiles"] = table_box(c, 45, 355, 140, 57, "coach_profiles", ["coach_user_id PK/FK", "created_by_admin_id", "active"], PURPLE_FILL, PURPLE)
    b["relationships"] = table_box(c, 250, 403, 170, 75, "coach_client_relationships", ["coach_user_id FK", "client_user_id FK", "status PENDING/ACTIVE/REVOKED/REJECTED", "unique coach+client for PENDING/ACTIVE"], PURPLE_FILL, PURPLE, small=True)
    b["comments"] = table_box(c, 480, 403, 150, 65, "coach_session_comments", ["coach_user_id FK", "client_user_id FK", "session_id FK", "deleted_at"], PURPLE_FILL, PURPLE)
    b["assignments"] = table_box(c, 662, 403, 145, 65, "coach_template_assignments", ["coach/client FK", "template_id nullable", "status ASSIGNED/COMPLETED/REVOKED"], PURPLE_FILL, PURPLE, small=True)
    b["templates"] = table_box(c, 345, 305, 150, 58, "workout_templates", ["marketplace source", "PRIVATE/PUBLIC", "copied_from_template_id"], GREEN_FILL, GREEN)
    b["sessions"] = table_box(c, 585, 305, 145, 52, "workout_sessions", ["coach comments point here", "owner still client user"], AMBER_FILL, AMBER)
    b["template_stats"] = table_box(c, 45, 222, 140, 62, "template_stats", ["template_id PK/FK", "up/down/save/use counts", "lock_version"], GREEN_FILL, GREEN)
    b["votes"] = table_box(c, 235, 222, 130, 58, "template_votes", ["PK user_id+template_id", "vote_type UP/DOWN"], GREEN_FILL, GREEN)
    b["saves"] = table_box(c, 410, 222, 130, 58, "template_saves", ["PK user_id+template_id", "idempotent save"], GREEN_FILL, GREEN)
    b["uses"] = table_box(c, 585, 212, 155, 72, "template_use_events", ["user_id FK", "source_template_id nullable", "copied_template_id nullable", "source/copy name snapshots"], GREEN_FILL, GREEN, small=True)
    b["analysis"] = table_box(c, 235, 116, 150, 58, "template_analysis_results", ["template_id FK", "score 0..100", "category", "jsonb positives/warnings/notes"], RED_FILL, RED, small=True)
    b["outbox"] = table_box(c, 470, 116, 150, 58, "outbox_events", ["schema pregătită", "status/attempts/payload", "nu este search listener-ul curent"], LIGHT, LINE, small=True)

    arrow(c, side(b["app_users"], "left"), side(b["refresh_tokens"], "right"), "user")
    arrow(c, (350, 515), side(b["coach_profiles"], "right"), "coach/admin")
    arrow(c, side(b["app_users"], "bottom"), side(b["relationships"], "top"), "coach/client")
    arrow(c, side(b["relationships"], "right"), side(b["comments"], "left"), "ACTIVE enables")
    arrow(c, side(b["relationships"], "right"), side(b["assignments"], "left"), "ACTIVE enables")
    arrow(c, side(b["comments"], "bottom"), side(b["sessions"], "top"), "session_id")
    arrow(c, side(b["assignments"], "bottom"), side(b["templates"], "right"), "template_id")
    arrow(c, side(b["templates"], "left"), side(b["template_stats"], "right"), "1:1")
    arrow(c, side(b["templates"], "bottom"), side(b["votes"], "top"), "1:n")
    arrow(c, side(b["templates"], "bottom"), side(b["saves"], "top"), "1:n")
    arrow(c, side(b["templates"], "right"), side(b["uses"], "top"), "use/copy")
    arrow(c, side(b["templates"], "bottom"), side(b["analysis"], "top"), "analysis result")
    arrow(c, (420, 505), side(b["votes"], "top"), "user_id", PURPLE, dash=True)
    arrow(c, (455, 505), side(b["saves"], "top"), "user_id", PURPLE, dash=True)
    arrow(c, (480, 505), side(b["uses"], "top"), "user_id", PURPLE, dash=True)

    note_box(c, 45, 45, 370, 55, "Ce trebuie să spui", [
        "RBAC dă voie pe zona /api/coach/** doar rolului COACH, dar ReBAC verifică relația ACTIVE pentru fiecare client.",
        "Marketplace-ul folosește template_stats, template_votes, template_saves și template_use_events."
    ], fill=colors.HexColor("#f8fafc"))
    note_box(c, 450, 45, 355, 55, "Atenție la outbox", [
        "Tabelul outbox_events există în schemă, dar indexarea curentă OpenSearch folosește TransactionalEventListener AFTER_COMMIT, nu un worker outbox."
    ], fill=colors.HexColor("#fff4df"), stroke=AMBER)
    footer(c)


def snapshot_page(c: canvas.Canvas) -> None:
    header(c, "4. Fluxul de sesiune și snapshot-uri", 5)
    p1 = flow_box(c, 45, 395, 150, 82, "1. Date de planificare", [
        "template, zi, exerciții, rutine",
        "sală + echipamente",
        "editabile și soft-deletable"
    ], BLUE_FILL, BLUE)
    p2 = flow_box(c, 245, 395, 155, 82, "2. POST /workouts/start", [
        "verifică user din JWT",
        "requireOwnedDay + gym owned",
        "ziua nu poate fi goală",
        "un singur IN_PROGRESS/user"
    ], LIGHT, LINE)
    p3 = flow_box(c, 450, 395, 165, 82, "3. Snapshot în tranzacție", [
        "workout_sessions: template/day/gym names",
        "session_exercises: exercise/planned fields",
        "session_routines: routine name/content",
        "muscle groups copied"
    ], AMBER_FILL, AMBER)
    p4 = flow_box(c, 665, 395, 130, 82, "4. Sesiune live", [
        "add set/update/delete",
        "extra exercise",
        "notes",
        "finish/cancel"
    ], GREEN_FILL, GREEN)
    p5 = flow_box(c, 245, 220, 155, 82, "5. Seturi", [
        "workout_sets -> session_exercises",
        "set_number unique",
        "equipment_id nullable",
        "equipment_name_snapshot"
    ], AMBER_FILL, AMBER)
    p6 = flow_box(c, 450, 220, 165, 82, "6. Istoric stabil", [
        "template/exercise/gym pot fi redenumite",
        "istoricul afișează numele vechi",
        "analytics și search citesc snapshot-uri"
    ], AMBER_FILL, AMBER)
    p7 = flow_box(c, 665, 220, 130, 82, "7. OpenSearch", [
        "FINISHED/CANCELLED indexate",
        "document construit din snapshot-uri",
        "notes reindexate"
    ], RED_FILL, RED)
    arrow(c, side(p1, "right"), side(p2, "left"))
    arrow(c, side(p2, "right"), side(p3, "left"))
    arrow(c, side(p3, "right"), side(p4, "left"))
    arrow(c, side(p4, "bottom"), side(p5, "right"), "sets")
    arrow(c, side(p5, "right"), side(p6, "left"))
    arrow(c, side(p6, "right"), side(p7, "left"), "after commit")
    arrow(c, (532, 395), (532, 302), "source no longer needed", AMBER, dash=True)

    note_box(c, 45, 91, 360, 92, "Exemplu bun la prezentare", [
        "Dacă redenumesc 'Bench Press' după o sesiune veche, istoricul vechi rămâne cu numele capturat atunci.",
        "Dacă șterg un echipament, setul vechi păstrează equipment_name_snapshot.",
        "Asta e diferența dintre planificare editabilă și istoric factual."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    note_box(c, 440, 91, 355, 92, "Regulă de date", [
        "Corect: workout_sets -> session_exercises -> workout_sessions.",
        "Greșit: workout_sets -> template_day_exercises.",
        "Motiv: un set este execuție istorică, nu o modificare a planului."
    ], fill=colors.HexColor("#f8fafc"))
    footer(c)


def security_page(c: canvas.Canvas) -> None:
    header(c, "5. Autentificare, autorizare și anti-IDOR", 6)
    a = flow_box(c, 45, 395, 135, 76, "Register/Login", [
        "email + password",
        "password -> BCrypt",
        "user role default USER"
    ], PURPLE_FILL, PURPLE)
    b = flow_box(c, 230, 395, 135, 76, "Issue tokens", [
        "access JWT HS256",
        "refresh opac random",
        "SHA-256 hash în DB"
    ], PURPLE_FILL, PURPLE)
    c1 = flow_box(c, 415, 395, 150, 76, "Frontend", [
        "access în Authorization",
        "refresh cookie HttpOnly",
        "SameSite=Strict, Path=/api/auth"
    ], BLUE_FILL, BLUE)
    d = flow_box(c, 615, 395, 150, 76, "JWT filter", [
        "parsează token",
        "UserPrincipal(id,email,role)",
        "serverul deduce userId"
    ], GREEN_FILL, GREEN)
    e = flow_box(c, 45, 245, 155, 82, "Refresh", [
        "cookie raw refresh",
        "hash -> refresh_tokens",
        "verifică active + expiry",
        "revocă vechiul token",
        "emite pereche nouă"
    ], PURPLE_FILL, PURPLE)
    f = flow_box(c, 245, 245, 155, 82, "RBAC routes", [
        "public: auth + health",
        "/api/admin/** -> ADMIN",
        "/api/coach/** -> COACH",
        "restul /api/** -> authenticated"
    ], LIGHT, LINE)
    g = flow_box(c, 445, 245, 155, 82, "Ownership checks", [
        "resurse încărcate cu userId curent",
        "frontend nu trimite userId",
        "nested resources verifică lanțul până la owner"
    ], GREEN_FILL, GREEN)
    h = flow_box(c, 645, 245, 150, 82, "ReBAC coach", [
        "rol COACH nu ajunge",
        "CoachAccess cere relație ACTIVE",
        "lipsă relație -> 404"
    ], PURPLE_FILL, PURPLE)
    arrow(c, side(a, "right"), side(b, "left"))
    arrow(c, side(b, "right"), side(c1, "left"))
    arrow(c, side(c1, "right"), side(d, "left"))
    arrow(c, side(c1, "bottom"), side(e, "top"), "refresh flow")
    arrow(c, side(d, "bottom"), side(f, "top"), "authorize")
    arrow(c, side(f, "right"), side(g, "left"), "service layer")
    arrow(c, side(g, "right"), side(h, "left"), "coach reads")
    note_box(c, 45, 88, 355, 95, "De ce 404 pentru resursa altuia", [
        "Un 403 ar confirma că ID-ul există. Pentru resurse personale, aplicația întoarce 404 ca să reducă enumerarea de identificatori.",
        "Acest model apare în TemplateAccess, WorkoutSessionAccess, servicii de planning și CoachAccess."
    ], fill=colors.HexColor("#f8fafc"))
    note_box(c, 440, 88, 355, 95, "Fraza scurtă", [
        "Autentificarea spune cine e utilizatorul. Autorizarea verifică dacă acel utilizator are dreptul la fiecare resursă concretă.",
        "Rolurile controlează zone mari; owner checks și ReBAC controlează datele reale."
    ], fill=colors.HexColor("#eef4ff"), stroke=BLUE)
    footer(c)


def search_page(c: canvas.Canvas) -> None:
    header(c, "6. OpenSearch - model de citire derivat", 7)
    pg = flow_box(c, 40, 395, 150, 78, "PostgreSQL", [
        "sursa de adevăr",
        "template-uri active",
        "sesiuni terminale",
        "snapshot-uri"
    ], AMBER_FILL, AMBER)
    evt = flow_box(c, 235, 395, 150, 78, "Events după write", [
        "TemplateIndexEvent",
        "WorkoutSessionIndexEvent",
        "AFTER_COMMIT listener"
    ], LIGHT, LINE)
    idx = flow_box(c, 430, 395, 150, 78, "Indexers", [
        "TemplateIndexer",
        "WorkoutSessionIndexer",
        "structural vs stats-only"
    ], RED_FILL, RED)
    os = flow_box(c, 625, 395, 155, 78, "OpenSearch aliases", [
        "templates -> templates_v1_*",
        "workout_sessions -> workout_sessions_v1_*",
        "mapping JSON bundled"
    ], RED_FILL, RED)
    q = flow_box(c, 40, 222, 150, 82, "SearchController", [
        "GET /api/search/templates",
        "GET /api/search/workouts",
        "ObjectProvider: 503 dacă search off"
    ], BLUE_FILL, BLUE)
    query = flow_box(c, 235, 222, 150, 82, "Query", [
        "multi_match boosted",
        "fuzziness AUTO >= 3 chars",
        "filters, facets, highlight",
        "max size 50"
    ], RED_FILL, RED)
    valid = flow_box(c, 430, 222, 150, 82, "PG revalidation", [
        "template: PUBLIC sau owner",
        "workout: owner + terminal",
        "drop stale hits"
    ], AMBER_FILL, AMBER)
    dto = flow_box(c, 625, 222, 155, 82, "Response", [
        "items + facets",
        "score + highlights",
        "author/vote/saved pentru marketplace"
    ], GREEN_FILL, GREEN)
    re = flow_box(c, 235, 82, 345, 72, "Admin rebuild", [
        "POST /api/admin/search/reindex",
        "read IDs from PostgreSQL -> create new concrete index -> bulk index -> refresh -> atomic alias swap -> delete previous",
        "dacă eșuează, indexul anterior rămâne în spatele aliasului"
    ], LIGHT, LINE)
    arrow(c, side(pg, "right"), side(evt, "left"), "write committed")
    arrow(c, side(evt, "right"), side(idx, "left"), "after commit")
    arrow(c, side(idx, "right"), side(os, "left"), "upsert/delete")
    arrow(c, side(q, "right"), side(query, "left"))
    arrow(c, side(query, "right"), side(valid, "left"), "IDs")
    arrow(c, side(valid, "right"), side(dto, "left"))
    arrow(c, side(query, "top"), side(os, "bottom"), "read alias", RED)
    arrow(c, side(re, "left"), side(pg, "bottom"), "source", AMBER)
    arrow(c, side(re, "right"), side(os, "bottom"), "swap alias", RED)
    note_box(c, 40, 82, 160, 72, "Ce e impresionant", [
        "Căutarea nu este doar LIKE: are typo tolerance, fațete, highlight, ranking, filtre obligatorii de securitate și revalidare SQL."
    ], fill=colors.HexColor("#fae9ec"), stroke=RED)
    note_box(c, 610, 82, 170, 72, "Ce să nu spui", [
        "Nu spune că OpenSearch este sursa de adevăr. Nu spune că înlocuiește PostgreSQL."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    footer(c)


def marketplace_page(c: canvas.Canvas) -> None:
    header(c, "7. Marketplace - publish, interactions, deep copy", 8)
    p = flow_box(c, 45, 395, 150, 82, "Publish", [
        "owner-only",
        "template trebuie să aibă zi + exercițiu",
        "visibility PUBLIC",
        "published_at setat",
        "structural reindex"
    ], GREEN_FILL, GREEN)
    listbox = flow_box(c, 245, 395, 150, 82, "Marketplace list/detail", [
        "doar PUBLIC",
        "stats + autor",
        "sortări: newest/top/trending",
        "DTO include myVote/saved"
    ], BLUE_FILL, BLUE)
    inter = flow_box(c, 445, 395, 150, 82, "Vote/Save", [
        "self-vote respins",
        "vote toggle/switch",
        "save idempotent",
        "counters SQL clamped",
        "stats-only reindex"
    ], GREEN_FILL, GREEN)
    use = flow_box(c, 645, 395, 150, 82, "Use", [
        "copie PRIVATE nouă",
        "copied_from_template_id",
        "TemplateUseEvent",
        "source uses++"
    ], AMBER_FILL, AMBER)
    src = flow_box(c, 80, 220, 170, 90, "Source public template", [
        "workout_templates",
        "template_days",
        "template_day_exercises",
        "template_day_routines",
        "muscle snapshots"
    ], GREEN_FILL, GREEN)
    copy = flow_box(c, 335, 220, 175, 90, "Deep copy", [
        "zile copiate",
        "exerciții copiate cu planned fields",
        "muscle groups copiate",
        "routines copiate ca snapshots"
    ], AMBER_FILL, AMBER)
    dst = flow_box(c, 595, 220, 170, 90, "User private template", [
        "independent de sursă",
        "official exercise refs păstrate",
        "custom exercise refs -> null",
        "snapshots rămân"
    ], AMBER_FILL, AMBER)
    arrow(c, side(p, "right"), side(listbox, "left"))
    arrow(c, side(listbox, "right"), side(inter, "left"))
    arrow(c, side(inter, "right"), side(use, "left"))
    arrow(c, side(src, "right"), side(copy, "left"), "copy")
    arrow(c, side(copy, "right"), side(dst, "left"), "new private")
    arrow(c, side(use, "bottom"), side(copy, "top"), "TemplateCopyService")
    note_box(c, 65, 76, 330, 82, "De ce e corect pentru marketplace", [
        "Copia nu depinde de autorul originalului. Dacă autorul editează sau șterge template-ul, utilizatorul care l-a folosit păstrează copia lui.",
        "Referințele la exerciții custom ale altui utilizator nu sunt păstrate, dar numele și grupele musculare rămân în snapshot-uri."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    note_box(c, 430, 76, 335, 82, "Tabele implicate", [
        "workout_templates, template_days, template_day_exercises, template_day_exercise_muscle_groups, template_day_routines.",
        "template_stats, template_votes, template_saves, template_use_events."
    ], fill=colors.HexColor("#f8fafc"))
    footer(c)


def analyzer_page(c: canvas.Canvas) -> None:
    header(c, "8. Analizatorul determinist de template-uri", 9)
    a = flow_box(c, 45, 395, 145, 82, "GET analysis", [
        "GET /api/templates/{id}/analysis",
        "user din JWT",
        "template own sau PUBLIC"
    ], BLUE_FILL, BLUE)
    b = flow_box(c, 230, 395, 145, 82, "Read model", [
        "TemplateAnalysisSource",
        "template + days + exercises",
        "folosește snapshots din template"
    ], GREEN_FILL, GREEN)
    c2 = flow_box(c, 415, 395, 145, 82, "Accumulation", [
        "doar STRENGTH contează la volum",
        "PRIMARY = 1.0",
        "SECONDARY = 0.5",
        "reps parsate numeric"
    ], AMBER_FILL, AMBER)
    d = flow_box(c, 600, 395, 165, 82, "Scores", [
        "volume/coverage 35",
        "frequency 20",
        "balance 20",
        "session design 15",
        "specificity/rest 10"
    ], RED_FILL, RED)
    e = flow_box(c, 115, 220, 155, 78, "Warnings", [
        "missing push/pull/lower",
        "low/excessive volume",
        "imbalances",
        "short rest",
        "beginner heuristics"
    ], RED_FILL, RED)
    f = flow_box(c, 345, 220, 155, 78, "Output", [
        "score 0-100",
        "category: NEEDS_REVIEW, DECENT_STRUCTURE, WELL_STRUCTURED",
        "warnings/suggestions/strengths/limitations"
    ], GREEN_FILL, GREEN)
    g = flow_box(c, 575, 220, 155, 78, "Limite explicite", [
        "nu e AI",
        "nu e sfat medical",
        "nu știe RIR/RPE, recuperare, somn, tehnică, alimentație"
    ], LIGHT, LINE)
    arrow(c, side(a, "right"), side(b, "left"))
    arrow(c, side(b, "right"), side(c2, "left"))
    arrow(c, side(c2, "right"), side(d, "left"))
    arrow(c, side(d, "bottom"), side(e, "top"))
    arrow(c, side(e, "right"), side(f, "left"))
    arrow(c, side(f, "right"), side(g, "left"))
    note_box(c, 65, 80, 340, 82, "Fraza pentru comisie", [
        "Analizatorul este un evaluator structural, rule-based. Nu încearcă să fie coach AI sau medic, ci verifică dacă template-ul are o structură rezonabilă.",
        "Regulile sunt explicabile și deterministe, deci același template produce același rezultat."
    ], fill=colors.HexColor("#f8fafc"))
    note_box(c, 440, 80, 325, 82, "Atenție la date incomplete", [
        "Dacă lipsesc planned_sets, exercițiile respective nu intră complet în volum; răspunsul include limitare și warning de volum incomplet."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    footer(c)


def history_page(c: canvas.Canvas) -> None:
    header(c, "9. Istoric și analytics", 10)
    s = flow_box(c, 45, 395, 155, 82, "Workout terminal", [
        "FINISHED pentru analytics",
        "History listează toate statusurile",
        "started/finished"
    ], AMBER_FILL, AMBER)
    hist = flow_box(c, 250, 395, 155, 82, "HistoryService", [
        "GET /api/history",
        "sort desc by startedAt",
        "batch summary per page",
        "duration doar dacă finished_at"
    ], BLUE_FILL, BLUE)
    ana = flow_box(c, 455, 395, 155, 82, "AnalyticsService", [
        "GET /api/analytics/overview",
        "agregări SQL native",
        "doar FINISHED",
        "PostgreSQL-side grouping"
    ], GREEN_FILL, GREEN)
    ui = flow_box(c, 660, 395, 135, 82, "UI", [
        "history page",
        "analytics charts",
        "volume, weekly workouts, muscles, best sets"
    ], BLUE_FILL, BLUE)
    q1 = flow_box(c, 65, 205, 155, 90, "Volume over time", [
        "SUM(weight * reps)",
        "group by ws.started_at::date",
        "FINISHED only"
    ], LIGHT, LINE)
    q2 = flow_box(c, 250, 205, 155, 90, "Workouts per week", [
        "date_trunc('week')",
        "count sessions",
        "ISO Monday anchored"
    ], LIGHT, LINE)
    q3 = flow_box(c, 435, 205, 155, 90, "Muscle distribution", [
        "session_exercise_muscle_groups",
        "role_snapshot = PRIMARY",
        "count sets"
    ], LIGHT, LINE)
    q4 = flow_box(c, 620, 205, 155, 90, "Best set / 1RM", [
        "Epley: weight * (1 + reps/30)",
        "DISTINCT ON per exercise",
        "series per day/exercise"
    ], LIGHT, LINE)
    arrow(c, side(s, "right"), side(hist, "left"))
    arrow(c, side(hist, "right"), side(ana, "left"))
    arrow(c, side(ana, "right"), side(ui, "left"))
    arrow(c, side(ana, "bottom"), side(q1, "top"), "SQL")
    arrow(c, side(ana, "bottom"), side(q2, "top"), "SQL")
    arrow(c, side(ana, "bottom"), side(q3, "top"), "SQL")
    arrow(c, side(ana, "bottom"), side(q4, "top"), "SQL")
    note_box(c, 80, 75, 685, 72, "Legătura cu snapshot-urile", [
        "Analytics nu se bazează pe numele curente din exercises/templates, ci pe session_exercises și session_exercise_muscle_groups. Asta păstrează rapoartele coerente cu momentul execuției.",
        "OpenSearch workout history folosește aceeași idee: documentul de căutare se construiește din snapshot-urile sesiunii."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    footer(c)


def api_page(c: canvas.Canvas) -> None:
    header(c, "10. Hartă API exactă din controllere", 11)
    groups = [
        ("Auth", "POST /api/auth/register, /login, /refresh, /logout\nGET /api/auth/me"),
        ("Planning", "GET /api/muscle-groups\nGET /api/exercises, /official, /custom\nPOST /api/exercises/custom\nPUT/DELETE /api/exercises/custom/{exerciseId}\nGET/POST/PUT/DELETE /api/routines\nGET/POST/PUT/DELETE /api/gyms + /api/gyms/{gymId}/equipment"),
        ("Templates", "GET/POST /api/templates\nGET/PUT/DELETE /api/templates/{templateId}\nPOST /api/templates/{templateId}/publish|unpublish\nPOST /api/templates/{templateId}/days\nPUT/DELETE /api/template-days/{templateDayId}\nPOST /api/template-days/{templateDayId}/exercises|routines\nPUT/DELETE /api/template-day-exercises/{templateDayExerciseId}\nDELETE /api/template-day-routines/{templateDayRoutineId}"),
        ("Sessions", "POST /api/workouts/start\nGET /api/workouts/active, /api/workouts/{sessionId}\nPOST /api/workouts/{sessionId}/finish|cancel|extra-exercises\nPUT /api/workouts/{sessionId}/notes\nPOST /api/session-exercises/{sessionExerciseId}/sets\nPUT/DELETE /api/sets/{setId}"),
        ("History/analytics", "GET /api/history\nGET /api/analytics/overview"),
        ("Marketplace/analyzer/search", "GET /api/marketplace/templates, /{templateId}\nPOST/DELETE /api/marketplace/templates/{templateId}/vote|save\nPOST /api/marketplace/templates/{templateId}/use\nGET /api/templates/{templateId}/analysis\nGET /api/search/templates, /api/search/workouts\nPOST /api/admin/search/reindex"),
        ("Coaching", "GET /api/coach/clients\nPOST /api/coach/clients/invite\nDELETE /api/coach/clients/{clientId}\nGET /api/coach/clients/{clientId}/history|analytics\nGET /api/coach/clients/{clientId}/sessions/{sessionId}\nGET /api/coach/clients/{clientId}/search/workouts\nGET /api/coaching/invites, /coaches\nPOST /api/coaching/invites/{relationshipId}/accept|reject\nDELETE /api/coaching/coaches/{relationshipId}"),
    ]
    x_positions = [42, 306, 570]
    y_positions = [402, 222, 68]
    dims = [(235, 128), (235, 162), (235, 162), (235, 160), (235, 72), (235, 165), (235, 165)]
    placements = [
        (x_positions[0], y_positions[0], dims[0][0], dims[0][1]),
        (x_positions[1], y_positions[0] - 34, dims[1][0], dims[1][1]),
        (x_positions[2], y_positions[0] - 34, dims[2][0], dims[2][1]),
        (x_positions[0], y_positions[1] - 10, dims[3][0], dims[3][1]),
        (x_positions[1], y_positions[1] + 78, dims[4][0], dims[4][1]),
        (x_positions[1], y_positions[1] - 95, dims[5][0], dims[5][1]),
        (x_positions[2], y_positions[1] - 95, dims[6][0], dims[6][1]),
    ]
    for (title, body), (x, y, w, h) in zip(groups, placements):
        table_box(c, x, y, w, h, title, body.split("\n"), fill=LIGHT, stroke=LINE, small=True)
    note_box(c, 42, 38, 763, 40, "Regula de securitate aplicată peste API", [
        "Fiecare endpoint este fie public controlat, fie autentificat, fie admin/coach RBAC; datele concrete sunt verificate prin ownership, public visibility sau relație coach-client ACTIVE."
    ], fill=colors.HexColor("#eef4ff"), stroke=BLUE)
    footer(c)


def defense_page(c: canvas.Canvas) -> None:
    header(c, "11. Ce să poți desena pe tablă în 2 minute", 12)
    left = [
        "1. User -> React -> REST -> Spring Boot -> PostgreSQL. OpenSearch stă lateral, ca index derivat.",
        "2. Planning data: exercises/routines/gyms/templates. Historical data: workout_sessions/session_exercises/workout_sets.",
        "3. Snapshot: la start se copiază numele și câmpurile planificate în session_*; istoria nu mai depinde de template.",
        "4. Security: JWT access, refresh cookie HttpOnly, refresh token hash-uit și rotit, BCrypt pentru parole.",
        "5. Anti-IDOR: userId vine din JWT; resursele altui user se comportă ca 404.",
        "6. Coaching: ROLE_COACH plus relație ACTIVE; rolul singur nu e suficient.",
    ]
    right = [
        "7. Marketplace: publicare doar pentru template ne-gol; vote/save/use; use face deep copy privat.",
        "8. Analyzer: determinist, rule-based, scor 0-100; nu AI și nu sfat medical.",
        "9. Search: OpenSearch face full-text/fuzzy/facets/highlight, apoi rezultatele sunt revalidate în PostgreSQL.",
        "10. Reindex: admin creează index nou, bulk din PostgreSQL, swap atomic de alias.",
        "11. Testare: Testcontainers cu PostgreSQL real și OpenSearch real, nu H2.",
        "12. Nu menționa Redis ca implementat; nu este în codul curent."
    ]
    note_box(c, 45, 308, 355, 210, "Checklist rapid", left, fill=colors.HexColor("#f8fafc"))
    note_box(c, 440, 308, 355, 210, "Checklist rapid", right, fill=colors.HexColor("#f8fafc"))
    note_box(c, 60, 160, 320, 90, "Desen minimal pentru snapshot", [
        "template_day_exercises -> session_exercises -> workout_sets",
        "Arată explicit că workout_sets nu merge înapoi direct spre template_day_exercises.",
        "Spune: planul este editabil, execuția este istorică."
    ], fill=colors.HexColor("#fff8e8"), stroke=AMBER)
    note_box(c, 455, 160, 320, 90, "Desen minimal pentru search", [
        "PostgreSQL -> event after commit -> OpenSearch alias.",
        "Search query -> OpenSearch IDs -> PostgreSQL revalidation -> response.",
        "Spune: performanță și UX, fără să pierd consistența/security."
    ], fill=colors.HexColor("#fae9ec"), stroke=RED)
    c.setFillColor(NAVY)
    c.setFont(FONT_BOLD, 13)
    c.drawCentredString(W / 2, 106, "Fraza de încheiere")
    c.setFont(FONT, 10)
    text = ("Contribuția principală este integrarea unei aplicații full-stack reale cu istoric stabil prin "
            "snapshot-uri, securitate multi-user, marketplace cu copii independente, analizator determinist, "
            "coaching controlat prin relații și căutare OpenSearch derivată, reconstruită din PostgreSQL.")
    lines = wrap(text, 720, FONT, 10)
    y = 86
    for line in lines:
        c.drawCentredString(W / 2, y, line)
        y -= 13
    footer(c)


def build() -> None:
    register_fonts()
    table_count, endpoint_count = validate_sources()
    OUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUT), pagesize=PAGE_SIZE)
    title_page(c, table_count, endpoint_count)
    c.showPage()
    architecture_page(c)
    c.showPage()
    db_core_page(c)
    c.showPage()
    db_support_page(c)
    c.showPage()
    snapshot_page(c)
    c.showPage()
    security_page(c)
    c.showPage()
    search_page(c)
    c.showPage()
    marketplace_page(c)
    c.showPage()
    analyzer_page(c)
    c.showPage()
    history_page(c)
    c.showPage()
    api_page(c)
    c.showPage()
    defense_page(c)
    c.save()
    print(OUT)


if __name__ == "__main__":
    build()
