from __future__ import annotations

import math
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable
from xml.sax.saxutils import escape

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
SCHEMA = ROOT / "backend/src/main/resources/db/migration/V1__full_initial_schema.sql"
OUT_DIR = ROOT / "output/diagrams"


@dataclass(frozen=True)
class Column:
    name: str
    type_name: str
    primary_key: bool
    foreign_table: str | None


@dataclass(frozen=True)
class Table:
    name: str
    columns: list[Column]


@dataclass(frozen=True)
class Edge:
    child: str
    column: str
    parent: str


def parse_schema(sql: str) -> tuple[dict[str, Table], list[Edge]]:
    tables: dict[str, Table] = {}
    edges: list[Edge] = []
    pattern = re.compile(r"CREATE TABLE\s+(\w+)\s*\((.*?)\);", re.S)

    for match in pattern.finditer(sql):
        table_name = match.group(1)
        body = match.group(2)
        columns: list[Column] = []
        for raw_line in body.splitlines():
            line = raw_line.strip().rstrip(",")
            if not line or line.startswith("CONSTRAINT") or line.startswith("PRIMARY KEY"):
                continue
            m = re.match(r"(\w+)\s+(.+)", line)
            if not m:
                continue
            col_name = m.group(1)
            rest = m.group(2)
            type_name = rest.split()[0]
            primary = "PRIMARY KEY" in rest
            ref = re.search(r"REFERENCES\s+(\w+)\s*\(", rest)
            foreign_table = ref.group(1) if ref else None
            columns.append(Column(col_name, type_name, primary, foreign_table))
            if foreign_table:
                edges.append(Edge(table_name, col_name, foreign_table))
        tables[table_name] = Table(table_name, columns)
    return tables, edges


HISTORY_SNAPSHOT = {
    "workout_sessions",
    "session_exercises",
    "session_exercise_muscle_groups",
    "session_routines",
    "workout_sets",
}

PLAN_SNAPSHOT = {
    "template_day_exercises",
    "template_day_exercise_muscle_groups",
    "template_day_routines",
    "template_use_events",
}


POSITIONS = {
    "app_users": (80, 120),
    "refresh_tokens": (80, 330),
    "coach_profiles": (80, 510),
    "coach_client_relationships": (80, 710),
    "muscle_groups": (520, 120),
    "exercises": (520, 330),
    "exercise_muscle_groups": (900, 250),
    "routines": (520, 600),
    "gyms": (520, 820),
    "equipment": (900, 820),
    "workout_templates": (1280, 120),
    "template_stats": (1660, 120),
    "template_votes": (1660, 300),
    "template_saves": (1660, 465),
    "template_days": (1280, 380),
    "template_day_exercises": (1280, 625),
    "template_day_exercise_muscle_groups": (1660, 625),
    "template_day_routines": (1280, 900),
    "template_use_events": (1660, 830),
    "template_analysis_results": (1660, 1040),
    "workout_sessions": (2080, 270),
    "session_exercises": (2080, 540),
    "session_exercise_muscle_groups": (2480, 540),
    "session_routines": (2080, 810),
    "workout_sets": (2080, 1060),
    "coach_session_comments": (2480, 810),
    "coach_template_assignments": (2480, 1040),
    "outbox_events": (2480, 120),
}


SELECTED_COLUMNS = {
    "app_users": ["id", "email", "role"],
    "refresh_tokens": ["id", "user_id", "token_hash", "revoked_at"],
    "coach_profiles": ["coach_user_id", "created_by_admin_id", "active"],
    "coach_client_relationships": [
        "id",
        "coach_user_id",
        "client_user_id",
        "status",
        "created_by_user_id",
    ],
    "muscle_groups": ["code", "display_name"],
    "exercises": ["id", "owner_user_id", "name", "exercise_type", "visibility", "deleted_at"],
    "exercise_muscle_groups": ["exercise_id", "muscle_group_code", "role"],
    "routines": ["id", "user_id", "name", "routine_type", "deleted_at"],
    "gyms": ["id", "user_id", "name", "deleted_at"],
    "equipment": ["id", "user_id", "gym_id", "name", "equipment_type", "deleted_at"],
    "workout_templates": [
        "id",
        "user_id",
        "name",
        "visibility",
        "copied_from_template_id",
        "aggregated_exercise_names",
    ],
    "template_stats": ["template_id", "upvotes_count", "saves_count", "uses_count", "trending_score"],
    "template_votes": ["user_id", "template_id", "vote_type"],
    "template_saves": ["user_id", "template_id"],
    "template_days": ["id", "template_id", "day_number", "name", "focus"],
    "template_day_exercises": [
        "id",
        "template_day_id",
        "exercise_id",
        "exercise_name_snapshot",
        "exercise_type_snapshot",
        "planned_sets",
        "planned_reps",
    ],
    "template_day_exercise_muscle_groups": [
        "template_day_exercise_id",
        "muscle_group_code",
        "role",
    ],
    "template_day_routines": [
        "id",
        "template_day_id",
        "routine_id",
        "routine_name_snapshot",
        "routine_content_snapshot",
    ],
    "template_use_events": [
        "id",
        "user_id",
        "source_template_id",
        "copied_template_id",
        "source_template_name_snapshot",
        "copied_template_name_snapshot",
    ],
    "template_analysis_results": ["id", "template_id", "score", "category", "warnings"],
    "workout_sessions": [
        "id",
        "user_id",
        "template_id",
        "template_name_snapshot",
        "template_day_name_snapshot",
        "gym_name_snapshot",
        "status",
    ],
    "session_exercises": [
        "id",
        "session_id",
        "original_template_day_exercise_id",
        "original_exercise_id",
        "exercise_name_snapshot",
        "planned_sets_snapshot",
        "planned_reps_snapshot",
    ],
    "session_exercise_muscle_groups": [
        "session_exercise_id",
        "muscle_group_code_snapshot",
        "role_snapshot",
    ],
    "session_routines": [
        "id",
        "session_id",
        "original_routine_id",
        "routine_name_snapshot",
        "routine_content_snapshot",
    ],
    "workout_sets": [
        "id",
        "session_exercise_id",
        "set_number",
        "weight",
        "reps",
        "equipment_id",
        "equipment_name_snapshot",
    ],
    "coach_session_comments": ["id", "coach_user_id", "client_user_id", "session_id"],
    "coach_template_assignments": ["id", "coach_user_id", "client_user_id", "template_id", "status"],
    "outbox_events": ["id", "aggregate_type", "aggregate_id", "event_type", "status"],
}


WIDTH = 2880
HEIGHT = 1320
BOX_W = 330
HEADER_H = 38
ROW_H = 20

COLORS = {
    "background": "#fbfbf8",
    "ink": "#14151a",
    "muted": "#60646f",
    "line": "#9ca3af",
    "normal_fill": "#ffffff",
    "normal_border": "#cbd5e1",
    "history_fill": "#fff1db",
    "history_border": "#d97706",
    "plan_fill": "#fff9d8",
    "plan_border": "#ca8a04",
    "title_blue": "#1f4e79",
    "edge_highlight": "#d97706",
}


def table_kind(table: str) -> str:
    if table in HISTORY_SNAPSHOT:
        return "history"
    if table in PLAN_SNAPSHOT:
        return "plan"
    return "normal"


def display_columns(table: Table) -> list[Column]:
    selected = SELECTED_COLUMNS.get(table.name)
    if not selected:
        return table.columns[:6]
    by_name = {c.name: c for c in table.columns}
    return [by_name[name] for name in selected if name in by_name]


def table_height(table: Table) -> int:
    return HEADER_H + 18 + ROW_H * len(display_columns(table)) + 12


def rect_for(table: str, tables: dict[str, Table]) -> tuple[int, int, int, int]:
    x, y = POSITIONS[table]
    return x, y, BOX_W, table_height(tables[table])


def edge_points(a: tuple[int, int, int, int], b: tuple[int, int, int, int]) -> tuple[tuple[float, float], tuple[float, float]]:
    ax, ay, aw, ah = a
    bx, by, bw, bh = b
    ac = (ax + aw / 2, ay + ah / 2)
    bc = (bx + bw / 2, by + bh / 2)

    def intersect(rect: tuple[int, int, int, int], src: tuple[float, float], dst: tuple[float, float]) -> tuple[float, float]:
        x, y, w, h = rect
        cx, cy = src
        dx = dst[0] - cx
        dy = dst[1] - cy
        candidates: list[tuple[float, float, float]] = []
        if dx:
            for px in (x, x + w):
                t = (px - cx) / dx
                py = cy + t * dy
                if 0 <= t <= 1 and y <= py <= y + h:
                    candidates.append((t, px, py))
        if dy:
            for py in (y, y + h):
                t = (py - cy) / dy
                px = cx + t * dx
                if 0 <= t <= 1 and x <= px <= x + w:
                    candidates.append((t, px, py))
        if not candidates:
            return src
        _, px, py = min(candidates, key=lambda item: item[0])
        return px, py

    return intersect(a, ac, bc), intersect(b, bc, ac)


def arrow_head(start: tuple[float, float], end: tuple[float, float], size: float = 9.0) -> list[tuple[float, float]]:
    sx, sy = start
    ex, ey = end
    angle = math.atan2(ey - sy, ex - sx)
    left = (ex - size * math.cos(angle - math.pi / 6), ey - size * math.sin(angle - math.pi / 6))
    right = (ex - size * math.cos(angle + math.pi / 6), ey - size * math.sin(angle + math.pi / 6))
    return [end, left, right]


def col_label(column: Column) -> str:
    marks = []
    if column.primary_key:
        marks.append("PK")
    if column.foreign_table:
        marks.append("FK")
    if "_snapshot" in column.name:
        marks.append("SNAP")
    prefix = f"{'/'.join(marks)} " if marks else ""
    return f"{prefix}{column.name}"


def write_svg(tables: dict[str, Table], edges: list[Edge], out: Path) -> None:
    parts: list[str] = []
    parts.append(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">'
    )
    parts.append(f'<rect width="{WIDTH}" height="{HEIGHT}" fill="{COLORS["background"]}"/>')
    parts.append(
        '<defs><filter id="shadow" x="-10%" y="-10%" width="120%" height="120%">'
        '<feDropShadow dx="0" dy="2" stdDeviation="3" flood-color="#000000" flood-opacity="0.12"/>'
        "</filter></defs>"
    )
    parts.append(
        f'<text x="80" y="54" font-family="Segoe UI, Arial" font-size="34" font-weight="700" fill="{COLORS["ink"]}">'
        "Database schema - snapshot tables highlighted</text>"
    )
    parts.append(
        f'<text x="80" y="88" font-family="Segoe UI, Arial" font-size="18" fill="{COLORS["muted"]}">'
        f"Generated from V1__full_initial_schema.sql: {len(tables)} tables, {len(edges)} foreign-key references.</text>"
    )

    # Legend
    legend_x = 1710
    legend_y = 38
    legend = [
        (COLORS["history_fill"], COLORS["history_border"], "session/history snapshot tables"),
        (COLORS["plan_fill"], COLORS["plan_border"], "template/marketplace snapshot tables"),
        (COLORS["normal_fill"], COLORS["normal_border"], "regular tables"),
    ]
    for idx, (fill, stroke, label) in enumerate(legend):
        y = legend_y + idx * 28
        parts.append(f'<rect x="{legend_x}" y="{y}" width="24" height="16" rx="4" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
        parts.append(
            f'<text x="{legend_x + 34}" y="{y + 13}" font-family="Segoe UI, Arial" font-size="16" fill="{COLORS["ink"]}">'
            f"{escape(label)}</text>"
        )

    clusters = [
        ("Users / auth / coaching", 70, 102, 380, 780),
        ("Planning resources", 500, 102, 760, 900),
        ("Templates / marketplace / analyzer", 1260, 102, 740, 1120),
        ("Workout history", 2060, 250, 760, 980),
        ("Async/search support", 2460, 102, 380, 145),
    ]
    for label, x, y, w, h in clusters:
        parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="18" fill="none" stroke="#e5e7eb" stroke-width="2"/>'
        )
        parts.append(
            f'<text x="{x + 16}" y="{y + 28}" font-family="Segoe UI, Arial" font-size="17" font-weight="700" fill="{COLORS["muted"]}">'
            f"{escape(label)}</text>"
        )

    important = {
        ("template_days", "template_id"),
        ("template_day_exercises", "template_day_id"),
        ("template_day_exercise_muscle_groups", "template_day_exercise_id"),
        ("template_day_routines", "template_day_id"),
        ("workout_sessions", "template_day_id"),
        ("session_exercises", "session_id"),
        ("session_exercises", "original_template_day_exercise_id"),
        ("session_exercise_muscle_groups", "session_exercise_id"),
        ("session_routines", "session_id"),
        ("workout_sets", "session_exercise_id"),
    }

    # Edges behind boxes.
    for edge in edges:
        if edge.child not in POSITIONS or edge.parent not in POSITIONS:
            continue
        start, end = edge_points(rect_for(edge.child, tables), rect_for(edge.parent, tables))
        hi = (edge.child, edge.column) in important or edge.child in HISTORY_SNAPSHOT
        color = COLORS["edge_highlight"] if hi else COLORS["line"]
        width = 2.0 if hi else 1.25
        opacity = 0.76 if hi else 0.38
        parts.append(
            f'<line x1="{start[0]:.1f}" y1="{start[1]:.1f}" x2="{end[0]:.1f}" y2="{end[1]:.1f}" '
            f'stroke="{color}" stroke-width="{width}" stroke-opacity="{opacity}" />'
        )
        ah = arrow_head(start, end, 9 if hi else 7)
        points = " ".join(f"{x:.1f},{y:.1f}" for x, y in ah)
        parts.append(f'<polygon points="{points}" fill="{color}" fill-opacity="{opacity}"/>')

    # Tables
    for name in tables:
        table = tables[name]
        if name not in POSITIONS:
            continue
        x, y, w, h = rect_for(name, tables)
        kind = table_kind(name)
        if kind == "history":
            fill, stroke = COLORS["history_fill"], COLORS["history_border"]
        elif kind == "plan":
            fill, stroke = COLORS["plan_fill"], COLORS["plan_border"]
        else:
            fill, stroke = COLORS["normal_fill"], COLORS["normal_border"]
        dash = ' stroke-dasharray="8 5"' if name == "template_day_exercise_muscle_groups" else ""
        parts.append(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{fill}" '
            f'stroke="{stroke}" stroke-width="2.2"{dash} filter="url(#shadow)"/>'
        )
        title_size = 14 if len(name) > 28 else 16 if len(name) > 23 else 18
        parts.append(
            f'<text x="{x + 14}" y="{y + 26}" font-family="Segoe UI, Arial" font-size="{title_size}" font-weight="700" fill="{COLORS["ink"]}">'
            f"{escape(name)}</text>"
        )
        tag = "history snapshot" if kind == "history" else "snapshot" if kind == "plan" else ""
        if tag and len(name) <= 23:
            parts.append(
                f'<text x="{x + w - 14}" y="{y + 26}" text-anchor="end" font-family="Segoe UI, Arial" font-size="12" font-weight="700" fill="{stroke}">'
                f"{tag}</text>"
            )
        parts.append(f'<line x1="{x}" y1="{y + HEADER_H}" x2="{x + w}" y2="{y + HEADER_H}" stroke="{stroke}" stroke-width="1.4"/>')
        for idx, column in enumerate(display_columns(table)):
            label = col_label(column)
            color = COLORS["edge_highlight"] if "_snapshot" in column.name else COLORS["ink"]
            if column.name in {"muscle_group_code", "role"} and name == "template_day_exercise_muscle_groups":
                color = COLORS["edge_highlight"]
            parts.append(
                f'<text x="{x + 14}" y="{y + HEADER_H + 24 + idx * ROW_H}" font-family="Consolas, Menlo, monospace" '
                f'font-size="13.5" fill="{color}">{escape(label)}</text>'
            )
        if len(table.columns) > len(display_columns(table)):
            more = len(table.columns) - len(display_columns(table))
            parts.append(
                f'<text x="{x + 14}" y="{y + h - 10}" font-family="Segoe UI, Arial" font-size="12" fill="{COLORS["muted"]}">'
                f"+ {more} other columns</text>"
            )

    parts.append("</svg>")
    out.write_text("\n".join(parts), encoding="utf-8")


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        "C:/Windows/Fonts/segoeuib.ttf" if bold else "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arialbd.ttf" if bold else "C:/Windows/Fonts/arial.ttf",
    ]
    for candidate in candidates:
        path = Path(candidate)
        if path.exists():
            return ImageFont.truetype(str(path), size)
    return ImageFont.load_default()


def write_png(tables: dict[str, Table], edges: list[Edge], out: Path) -> None:
    scale = 1
    image = Image.new("RGB", (WIDTH * scale, HEIGHT * scale), COLORS["background"])
    draw = ImageDraw.Draw(image)
    title_font = load_font(34, True)
    subtitle_font = load_font(18)
    heading_font = load_font(17, True)
    table_font = load_font(18, True)
    tag_font = load_font(12, True)
    mono_font = load_font(13)
    small_font = load_font(12)

    draw.text((80, 26), "Database schema - snapshot tables highlighted", fill=COLORS["ink"], font=title_font)
    draw.text(
        (80, 70),
        f"Generated from V1__full_initial_schema.sql: {len(tables)} tables, {len(edges)} foreign-key references.",
        fill=COLORS["muted"],
        font=subtitle_font,
    )

    legend_x = 1710
    legend_y = 38
    for idx, (fill, stroke, label) in enumerate(
        [
            (COLORS["history_fill"], COLORS["history_border"], "session/history snapshot tables"),
            (COLORS["plan_fill"], COLORS["plan_border"], "template/marketplace snapshot tables"),
            (COLORS["normal_fill"], COLORS["normal_border"], "regular tables"),
        ]
    ):
        y = legend_y + idx * 28
        draw.rounded_rectangle((legend_x, y, legend_x + 24, y + 16), radius=4, fill=fill, outline=stroke, width=2)
        draw.text((legend_x + 34, y - 1), label, fill=COLORS["ink"], font=small_font)

    for label, x, y, w, h in [
        ("Users / auth / coaching", 70, 102, 380, 780),
        ("Planning resources", 500, 102, 760, 900),
        ("Templates / marketplace / analyzer", 1260, 102, 740, 1120),
        ("Workout history", 2060, 250, 760, 980),
        ("Async/search support", 2460, 102, 380, 145),
    ]:
        draw.rounded_rectangle((x, y, x + w, y + h), radius=18, outline="#e5e7eb", width=2)
        draw.text((x + 16, y + 9), label, fill=COLORS["muted"], font=heading_font)

    important = {
        ("template_days", "template_id"),
        ("template_day_exercises", "template_day_id"),
        ("template_day_exercise_muscle_groups", "template_day_exercise_id"),
        ("template_day_routines", "template_day_id"),
        ("workout_sessions", "template_day_id"),
        ("session_exercises", "session_id"),
        ("session_exercises", "original_template_day_exercise_id"),
        ("session_exercise_muscle_groups", "session_exercise_id"),
        ("session_routines", "session_id"),
        ("workout_sets", "session_exercise_id"),
    }

    for edge in edges:
        if edge.child not in POSITIONS or edge.parent not in POSITIONS:
            continue
        start, end = edge_points(rect_for(edge.child, tables), rect_for(edge.parent, tables))
        hi = (edge.child, edge.column) in important or edge.child in HISTORY_SNAPSHOT
        color = COLORS["edge_highlight"] if hi else COLORS["line"]
        width = 2 if hi else 1
        draw.line((start[0], start[1], end[0], end[1]), fill=color, width=width)
        draw.polygon(arrow_head(start, end, 9 if hi else 7), fill=color)

    for name in tables:
        table = tables[name]
        if name not in POSITIONS:
            continue
        x, y, w, h = rect_for(name, tables)
        kind = table_kind(name)
        if kind == "history":
            fill, stroke = COLORS["history_fill"], COLORS["history_border"]
        elif kind == "plan":
            fill, stroke = COLORS["plan_fill"], COLORS["plan_border"]
        else:
            fill, stroke = COLORS["normal_fill"], COLORS["normal_border"]
        draw.rounded_rectangle((x, y, x + w, y + h), radius=12, fill=fill, outline=stroke, width=2)
        title_font_for_table = load_font(14 if len(name) > 28 else 16 if len(name) > 23 else 18, True)
        draw.text((x + 14, y + 8), name, fill=COLORS["ink"], font=title_font_for_table)
        tag = "history snapshot" if kind == "history" else "snapshot" if kind == "plan" else ""
        if tag and len(name) <= 23:
            bbox = draw.textbbox((0, 0), tag, font=tag_font)
            draw.text((x + w - 14 - (bbox[2] - bbox[0]), y + 11), tag, fill=stroke, font=tag_font)
        draw.line((x, y + HEADER_H, x + w, y + HEADER_H), fill=stroke, width=1)
        for idx, column in enumerate(display_columns(table)):
            label = col_label(column)
            color = COLORS["edge_highlight"] if "_snapshot" in column.name else COLORS["ink"]
            if column.name in {"muscle_group_code", "role"} and name == "template_day_exercise_muscle_groups":
                color = COLORS["edge_highlight"]
            draw.text((x + 14, y + HEADER_H + 9 + idx * ROW_H), label, fill=color, font=mono_font)
        if len(table.columns) > len(display_columns(table)):
            more = len(table.columns) - len(display_columns(table))
            draw.text((x + 14, y + h - 18), f"+ {more} other columns", fill=COLORS["muted"], font=small_font)

    image.save(out, quality=95)


def write_mermaid(tables: dict[str, Table], edges: list[Edge], out: Path) -> None:
    lines = ["erDiagram"]
    for edge in edges:
        lines.append(f"  {edge.parent} ||--o{{ {edge.child} : {edge.column}")
    for table in tables.values():
        lines.append(f"  {table.name} {{")
        for column in table.columns:
            dtype = re.sub(r"[^A-Za-z0-9_]", "_", column.type_name)
            markers = []
            if column.primary_key:
                markers.append("PK")
            if column.foreign_table:
                markers.append("FK")
            marker = " " + " ".join(markers) if markers else ""
            lines.append(f"    {dtype} {column.name}{marker}")
        lines.append("  }")
    out.write_text("\n".join(lines) + "\n", encoding="utf-8")


def overview_rect_for(table: str) -> tuple[int, int, int, int]:
    x, y = POSITIONS[table]
    return x, y, BOX_W, 62


def write_overview_svg(tables: dict[str, Table], edges: list[Edge], out: Path) -> None:
    parts: list[str] = []
    parts.append(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" viewBox="0 0 {WIDTH} {HEIGHT}">'
    )
    parts.append(f'<rect width="{WIDTH}" height="{HEIGHT}" fill="{COLORS["background"]}"/>')
    parts.append(
        f'<text x="80" y="54" font-family="Segoe UI, Arial" font-size="34" font-weight="700" fill="{COLORS["ink"]}">'
        "Database schema overview - snapshot tables highlighted</text>"
    )
    parts.append(
        f'<text x="80" y="88" font-family="Segoe UI, Arial" font-size="18" fill="{COLORS["muted"]}">'
        f"All {len(tables)} PostgreSQL tables from Flyway V1, with {len(edges)} foreign-key references.</text>"
    )
    legend_x = 1710
    legend_y = 38
    for idx, (fill, stroke, label) in enumerate(
        [
            (COLORS["history_fill"], COLORS["history_border"], "session/history snapshot tables"),
            (COLORS["plan_fill"], COLORS["plan_border"], "template/marketplace snapshot tables"),
            (COLORS["normal_fill"], COLORS["normal_border"], "regular tables"),
        ]
    ):
        y = legend_y + idx * 28
        parts.append(f'<rect x="{legend_x}" y="{y}" width="24" height="16" rx="4" fill="{fill}" stroke="{stroke}" stroke-width="2"/>')
        parts.append(
            f'<text x="{legend_x + 34}" y="{y + 13}" font-family="Segoe UI, Arial" font-size="16" fill="{COLORS["ink"]}">'
            f"{escape(label)}</text>"
        )
    for label, x, y, w, h in [
        ("Users / auth / coaching", 70, 102, 380, 780),
        ("Planning resources", 500, 102, 760, 900),
        ("Templates / marketplace / analyzer", 1260, 102, 740, 1120),
        ("Workout history", 2060, 250, 760, 980),
        ("Async/search support", 2460, 102, 380, 145),
    ]:
        parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="18" fill="none" stroke="#e5e7eb" stroke-width="2"/>')
        parts.append(
            f'<text x="{x + 16}" y="{y + 28}" font-family="Segoe UI, Arial" font-size="17" font-weight="700" fill="{COLORS["muted"]}">'
            f"{escape(label)}</text>"
        )
    for edge in edges:
        if edge.child not in POSITIONS or edge.parent not in POSITIONS:
            continue
        start, end = edge_points(overview_rect_for(edge.child), overview_rect_for(edge.parent))
        hi = edge.child in HISTORY_SNAPSHOT or edge.child in PLAN_SNAPSHOT
        color = COLORS["edge_highlight"] if hi else COLORS["line"]
        width = 2.0 if hi else 1.15
        opacity = 0.72 if hi else 0.32
        parts.append(
            f'<line x1="{start[0]:.1f}" y1="{start[1]:.1f}" x2="{end[0]:.1f}" y2="{end[1]:.1f}" '
            f'stroke="{color}" stroke-width="{width}" stroke-opacity="{opacity}" />'
        )
        ah = arrow_head(start, end, 8 if hi else 6)
        points = " ".join(f"{x:.1f},{y:.1f}" for x, y in ah)
        parts.append(f'<polygon points="{points}" fill="{color}" fill-opacity="{opacity}"/>')
    for name in tables:
        if name not in POSITIONS:
            continue
        x, y, w, h = overview_rect_for(name)
        kind = table_kind(name)
        if kind == "history":
            fill, stroke = COLORS["history_fill"], COLORS["history_border"]
        elif kind == "plan":
            fill, stroke = COLORS["plan_fill"], COLORS["plan_border"]
        else:
            fill, stroke = COLORS["normal_fill"], COLORS["normal_border"]
        title_size = 14 if len(name) > 28 else 16 if len(name) > 23 else 18
        parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="12" fill="{fill}" stroke="{stroke}" stroke-width="2.2"/>')
        parts.append(
            f'<text x="{x + w / 2}" y="{y + 38}" text-anchor="middle" font-family="Segoe UI, Arial" '
            f'font-size="{title_size}" font-weight="700" fill="{COLORS["ink"]}">{escape(name)}</text>'
        )
    parts.append("</svg>")
    out.write_text("\n".join(parts), encoding="utf-8")


def write_overview_png(tables: dict[str, Table], edges: list[Edge], out: Path) -> None:
    image = Image.new("RGB", (WIDTH, HEIGHT), COLORS["background"])
    draw = ImageDraw.Draw(image)
    title_font = load_font(34, True)
    subtitle_font = load_font(18)
    heading_font = load_font(17, True)
    small_font = load_font(12)
    draw.text((80, 26), "Database schema overview - snapshot tables highlighted", fill=COLORS["ink"], font=title_font)
    draw.text(
        (80, 70),
        f"All {len(tables)} PostgreSQL tables from Flyway V1, with {len(edges)} foreign-key references.",
        fill=COLORS["muted"],
        font=subtitle_font,
    )
    legend_x = 1710
    legend_y = 38
    for idx, (fill, stroke, label) in enumerate(
        [
            (COLORS["history_fill"], COLORS["history_border"], "session/history snapshot tables"),
            (COLORS["plan_fill"], COLORS["plan_border"], "template/marketplace snapshot tables"),
            (COLORS["normal_fill"], COLORS["normal_border"], "regular tables"),
        ]
    ):
        y = legend_y + idx * 28
        draw.rounded_rectangle((legend_x, y, legend_x + 24, y + 16), radius=4, fill=fill, outline=stroke, width=2)
        draw.text((legend_x + 34, y - 1), label, fill=COLORS["ink"], font=small_font)
    for label, x, y, w, h in [
        ("Users / auth / coaching", 70, 102, 380, 780),
        ("Planning resources", 500, 102, 760, 900),
        ("Templates / marketplace / analyzer", 1260, 102, 740, 1120),
        ("Workout history", 2060, 250, 760, 980),
        ("Async/search support", 2460, 102, 380, 145),
    ]:
        draw.rounded_rectangle((x, y, x + w, y + h), radius=18, outline="#e5e7eb", width=2)
        draw.text((x + 16, y + 9), label, fill=COLORS["muted"], font=heading_font)
    for edge in edges:
        if edge.child not in POSITIONS or edge.parent not in POSITIONS:
            continue
        start, end = edge_points(overview_rect_for(edge.child), overview_rect_for(edge.parent))
        hi = edge.child in HISTORY_SNAPSHOT or edge.child in PLAN_SNAPSHOT
        color = COLORS["edge_highlight"] if hi else COLORS["line"]
        width = 2 if hi else 1
        draw.line((start[0], start[1], end[0], end[1]), fill=color, width=width)
        draw.polygon(arrow_head(start, end, 8 if hi else 6), fill=color)
    for name in tables:
        if name not in POSITIONS:
            continue
        x, y, w, h = overview_rect_for(name)
        kind = table_kind(name)
        if kind == "history":
            fill, stroke = COLORS["history_fill"], COLORS["history_border"]
        elif kind == "plan":
            fill, stroke = COLORS["plan_fill"], COLORS["plan_border"]
        else:
            fill, stroke = COLORS["normal_fill"], COLORS["normal_border"]
        title_font_for_table = load_font(14 if len(name) > 28 else 16 if len(name) > 23 else 18, True)
        draw.rounded_rectangle((x, y, x + w, y + h), radius=12, fill=fill, outline=stroke, width=2)
        bbox = draw.textbbox((0, 0), name, font=title_font_for_table)
        draw.text((x + (w - (bbox[2] - bbox[0])) / 2, y + 21), name, fill=COLORS["ink"], font=title_font_for_table)
    image.save(out, quality=95)


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    sql = SCHEMA.read_text(encoding="utf-8")
    tables, edges = parse_schema(sql)
    write_svg(tables, edges, OUT_DIR / "database_schema_snapshots.svg")
    write_png(tables, edges, OUT_DIR / "database_schema_snapshots.png")
    write_overview_svg(tables, edges, OUT_DIR / "database_schema_snapshots_overview.svg")
    write_overview_png(tables, edges, OUT_DIR / "database_schema_snapshots_overview.png")
    write_mermaid(tables, edges, OUT_DIR / "database_schema_snapshots.mmd")
    print(f"Wrote {OUT_DIR / 'database_schema_snapshots.svg'}")
    print(f"Wrote {OUT_DIR / 'database_schema_snapshots.png'}")
    print(f"Wrote {OUT_DIR / 'database_schema_snapshots_overview.svg'}")
    print(f"Wrote {OUT_DIR / 'database_schema_snapshots_overview.png'}")
    print(f"Wrote {OUT_DIR / 'database_schema_snapshots.mmd'}")


if __name__ == "__main__":
    main()
