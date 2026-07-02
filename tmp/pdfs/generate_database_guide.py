from __future__ import annotations

import re
from dataclasses import dataclass
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Flowable,
    Image,
    KeepTogether,
    ListFlowable,
    ListItem,
    PageBreak,
    Paragraph,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parents[2]
MIGRATIONS = ROOT / "backend/src/main/resources/db/migration"
V1 = MIGRATIONS / "V1__full_initial_schema.sql"
V2 = MIGRATIONS / "V2__seed_official_exercises.sql"
V3 = MIGRATIONS / "V3__one_active_workout_per_user.sql"
DIAGRAM_OVERVIEW = ROOT / "output/diagrams/database_schema_snapshots_overview.png"
DIAGRAM_DETAIL = ROOT / "output/diagrams/database_schema_snapshots.png"
OUT = ROOT / "output/pdf/baza_de_date_platforma_workout.pdf"


INK = colors.HexColor("#15171c")
MUTED = colors.HexColor("#5f6673")
BLUE = colors.HexColor("#1f4e79")
GREEN = colors.HexColor("#2f7d55")
ORANGE = colors.HexColor("#c76f00")
RED = colors.HexColor("#b83a4b")
PURPLE = colors.HexColor("#74539B")
LINE = colors.HexColor("#d7dde8")
PANEL = colors.HexColor("#f6f8fb")
LIGHT_BLUE = colors.HexColor("#eaf2fb")
LIGHT_GREEN = colors.HexColor("#eaf7ef")
LIGHT_ORANGE = colors.HexColor("#fff3df")
LIGHT_RED = colors.HexColor("#fdecef")
LIGHT_YELLOW = colors.HexColor("#fff9d8")


@dataclass
class Column:
    name: str
    type_name: str
    raw: str
    primary_key: bool = False
    not_null: bool = False
    default: str | None = None
    foreign_table: str | None = None
    foreign_action: str | None = None
    snapshot: bool = False


@dataclass
class TableInfo:
    name: str
    columns: list[Column]
    constraints: list[str]
    indexes: list[str]


def register_fonts() -> tuple[str, str, str]:
    regular = Path("C:/Windows/Fonts/segoeui.ttf")
    bold = Path("C:/Windows/Fonts/segoeuib.ttf")
    mono = Path("C:/Windows/Fonts/consola.ttf")
    if regular.exists():
        pdfmetrics.registerFont(TTFont("SegoeUI", str(regular)))
        base = "SegoeUI"
    else:
        base = "Helvetica"
    if bold.exists():
        pdfmetrics.registerFont(TTFont("SegoeUI-Bold", str(bold)))
        bold_name = "SegoeUI-Bold"
    else:
        bold_name = "Helvetica-Bold"
    if mono.exists():
        pdfmetrics.registerFont(TTFont("Consolas", str(mono)))
        mono_name = "Consolas"
    else:
        mono_name = "Courier"
    return base, bold_name, mono_name


BASE_FONT, BOLD_FONT, MONO_FONT = register_fonts()


def make_styles():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        "TitleBig",
        parent=styles["Title"],
        fontName=BOLD_FONT,
        fontSize=23,
        leading=28,
        textColor=INK,
        alignment=TA_CENTER,
        spaceAfter=8,
    ))
    styles.add(ParagraphStyle(
        "Subtitle",
        parent=styles["Normal"],
        fontName=BASE_FONT,
        fontSize=11.2,
        leading=15.5,
        textColor=MUTED,
        alignment=TA_CENTER,
        spaceAfter=12,
    ))
    styles.add(ParagraphStyle(
        "H1",
        parent=styles["Heading1"],
        fontName=BOLD_FONT,
        fontSize=17,
        leading=22,
        textColor=BLUE,
        spaceBefore=4,
        spaceAfter=7,
    ))
    styles.add(ParagraphStyle(
        "H2",
        parent=styles["Heading2"],
        fontName=BOLD_FONT,
        fontSize=12.5,
        leading=16,
        textColor=INK,
        spaceBefore=6,
        spaceAfter=4,
    ))
    styles.add(ParagraphStyle(
        "Body",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=9.6,
        leading=13.2,
        textColor=INK,
        spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        "Small",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=8.0,
        leading=10.7,
        textColor=MUTED,
        spaceAfter=3,
    ))
    styles.add(ParagraphStyle(
        "CodeBlock",
        parent=styles["BodyText"],
        fontName=MONO_FONT,
        fontSize=7.8,
        leading=10.2,
        textColor=colors.HexColor("#273142"),
        backColor=colors.HexColor("#f4f6f8"),
        borderColor=colors.HexColor("#e1e6ee"),
        borderWidth=0.4,
        borderPadding=5,
        spaceAfter=6,
    ))
    styles.add(ParagraphStyle(
        "Callout",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=9.4,
        leading=13.2,
        textColor=INK,
        backColor=colors.HexColor("#fff8eb"),
        borderColor=colors.HexColor("#f1c982"),
        borderWidth=0.8,
        borderPadding=7,
        spaceBefore=4,
        spaceAfter=7,
    ))
    styles.add(ParagraphStyle(
        "TableHead",
        parent=styles["BodyText"],
        fontName=BOLD_FONT,
        fontSize=7.4,
        leading=9.5,
        textColor=colors.white,
        alignment=TA_LEFT,
    ))
    styles.add(ParagraphStyle(
        "TableCell",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=7.2,
        leading=9.3,
        textColor=INK,
    ))
    styles.add(ParagraphStyle(
        "MonoCell",
        parent=styles["BodyText"],
        fontName=MONO_FONT,
        fontSize=6.8,
        leading=8.8,
        textColor=colors.HexColor("#273142"),
    ))
    return styles


S = make_styles()


MODULES = [
    ("Utilizatori, autentificare și coaching", [
        "app_users",
        "refresh_tokens",
        "coach_profiles",
        "coach_client_relationships",
    ]),
    ("Date de planificare", [
        "muscle_groups",
        "exercises",
        "exercise_muscle_groups",
        "routines",
        "gyms",
        "equipment",
    ]),
    ("Template-uri și programe", [
        "workout_templates",
        "template_stats",
        "template_days",
        "template_day_exercises",
        "template_day_exercise_muscle_groups",
        "template_day_routines",
    ]),
    ("Sesiuni, istoric și snapshot-uri", [
        "workout_sessions",
        "session_exercises",
        "session_exercise_muscle_groups",
        "session_routines",
        "workout_sets",
    ]),
    ("Marketplace, analizator, coaching extins, infrastructură", [
        "coach_session_comments",
        "template_votes",
        "template_saves",
        "template_use_events",
        "coach_template_assignments",
        "template_analysis_results",
        "outbox_events",
    ]),
]


SNAPSHOT_HISTORY = {
    "workout_sessions",
    "session_exercises",
    "session_exercise_muscle_groups",
    "session_routines",
    "workout_sets",
}
SNAPSHOT_PLANNING = {
    "template_day_exercises",
    "template_day_exercise_muscle_groups",
    "template_day_routines",
    "template_use_events",
}


DESCRIPTIONS: dict[str, dict[str, str | list[str]]] = {
    "app_users": {
        "role": "Tabelul central pentru conturi. Ține identitatea utilizatorului, parola hash-uită și rolul folosit de RBAC.",
        "why": "Este rădăcina pentru aproape toate datele personale: exerciții custom, rutine, săli, template-uri, sesiuni și relații de coaching.",
        "presentation": "Spune că userul este sursa de ownership pentru datele private; rolul din acest tabel alimentează Spring Security.",
    },
    "refresh_tokens": {
        "role": "Stochează refresh token-urile server-side, doar ca SHA-256 fingerprint.",
        "why": "Permite sesiuni persistente, logout, expirare și rotație a refresh token-ului fără să salveze token-ul în clar.",
        "presentation": "Este dovada că refresh token-ul este revocabil; JWT-ul de access rămâne stateless, dar refresh-ul este controlat în DB.",
    },
    "coach_profiles": {
        "role": "Definește profilul unui utilizator cu rol de coach.",
        "why": "Separă existența contului de profilul public/activ de coach.",
        "presentation": "Coach-ul nu este doar un user; profilul lui poate fi activ/inactiv și creat de admin.",
    },
    "coach_client_relationships": {
        "role": "Modelează relația coach-client cu status PENDING, ACTIVE, REVOKED sau REJECTED.",
        "why": "Este baza ReBAC: un coach are acces la un client doar dacă relația este ACTIVE.",
        "presentation": "Aici este controlul real al accesului de coaching; rolul COACH singur nu ajunge.",
    },
    "muscle_groups": {
        "role": "Dicționarul de grupe musculare folosit de exerciții, template-uri, analizator și analytics.",
        "why": "Normalizează codurile musculare și permite agregări consistente.",
        "presentation": "Este un tabel mic, dar important pentru analizator și distribuția volumului.",
    },
    "exercises": {
        "role": "Conține exerciții oficiale și exerciții custom ale utilizatorilor.",
        "why": "Permite catalog comun plus exerciții personale, cu soft delete și unicitate pe nume.",
        "presentation": "Diferența OFFICIAL/CUSTOM este controlată în bază: exercițiile oficiale nu au owner, cele custom au owner.",
    },
    "exercise_muscle_groups": {
        "role": "Leagă exercițiile de grupe musculare și rolul lor PRIMARY/SECONDARY.",
        "why": "Fără acest tabel, analizatorul nu ar putea calcula volum/frecvență pe mușchi.",
        "presentation": "Este o relație many-to-many cu atribut suplimentar: rolul muscular.",
    },
    "routines": {
        "role": "Rutine editabile de început sau final de antrenament.",
        "why": "Utilizatorul poate reutiliza încălziri/cooldown-uri în mai multe zile de template.",
        "presentation": "Rutinele sunt planificare editabilă; când sunt atașate unei zile/sesiuni, conținutul se copiază ca snapshot.",
    },
    "gyms": {
        "role": "Sălile personale ale utilizatorului.",
        "why": "Context pentru echipamente și pentru istoricul sesiunilor.",
        "presentation": "Sala este editabilă, dar numele sălii se copiază pe sesiune ca snapshot.",
    },
    "equipment": {
        "role": "Echipamentele disponibile într-o sală.",
        "why": "Seturile pot nota cu ce echipament au fost executate.",
        "presentation": "Echipamentul este legat de sală și user; numele lui se copiază pe set ca snapshot.",
    },
    "workout_templates": {
        "role": "Programul complet de antrenament: split, dificultate, zile/săptămână, vizibilitate publică/privată.",
        "why": "Este containerul principal pentru template_days și pentru marketplace.",
        "presentation": "Template-ul este programul reutilizabil, nu o sesiune executată.",
    },
    "template_stats": {
        "role": "Ține contoarele marketplace: voturi, salvări, utilizări, rating și trending.",
        "why": "Separă statisticile mutate des de definiția template-ului.",
        "presentation": "Este util pentru marketplace și poate fi actualizat fără să rescrie template-ul.",
    },
    "template_days": {
        "role": "O zi concretă dintr-un template: Push, Pull, Legs, Upper etc.",
        "why": "Utilizatorul pornește o sesiune dintr-un template_day, nu din tot programul.",
        "presentation": "Template = program complet; template_day = ziua executabilă din acel program.",
    },
    "template_day_exercises": {
        "role": "Exercițiile planificate într-o zi de template, cu valori planificate și snapshot de nume/tip.",
        "why": "Păstrează stabilitatea template-ului chiar dacă exercițiul original este modificat sau șters.",
        "presentation": "Aici apar snapshot-uri de planificare: exercise_name_snapshot și exercise_type_snapshot.",
    },
    "template_day_exercise_muscle_groups": {
        "role": "Copiază grupele musculare ale exercițiului în contextul template-ului.",
        "why": "Template-ul rămâne analizabil chiar dacă exercițiul sursă se schimbă ulterior.",
        "presentation": "Coloanele nu au sufix _snapshot, dar funcțional sunt snapshot-uri ale codului muscular și rolului.",
    },
    "template_day_routines": {
        "role": "Atașează rutine START/END la o zi de template și copiază numele/conținutul rutinei.",
        "why": "Dacă rutina originală se schimbă, ziua de template poate păstra ce avea la momentul atașării.",
        "presentation": "Este snapshot de planificare, nu istoric de sesiune.",
    },
    "workout_sessions": {
        "role": "Sesiunea de antrenament pornită de utilizator, cu status IN_PROGRESS/FINISHED/CANCELLED.",
        "why": "Este rădăcina istoricului. Păstrează snapshot-uri pentru template, zi și sală.",
        "presentation": "Aici începe istoricul stabil; după pornire, sesiunea nu mai depinde de planul live pentru afișare.",
    },
    "session_exercises": {
        "role": "Exercițiile copiate într-o sesiune concretă, cu snapshot de nume/tip/valori planificate.",
        "why": "Permite ca istoricul să arate planul de la momentul execuției, nu planul modificat ulterior.",
        "presentation": "Cel mai bun exemplu de snapshot: planned_sets_snapshot, planned_reps_snapshot, exercise_name_snapshot.",
    },
    "session_exercise_muscle_groups": {
        "role": "Grupele musculare istorice pentru exercițiile din sesiune.",
        "why": "Analytics-ul pe mușchi rămâne corect chiar dacă exercițiul original se schimbă.",
        "presentation": "Este folosit în analytics pentru distribuție musculară din istoric.",
    },
    "session_routines": {
        "role": "Rutinele copiate în sesiunea executată.",
        "why": "Istoricul păstrează încălzirea/cooldown-ul efectiv planificat la momentul sesiunii.",
        "presentation": "Este versiunea istorică a template_day_routines.",
    },
    "workout_sets": {
        "role": "Seturile efectiv executate: greutate, reps, durată, distanță, RPE, echipament.",
        "why": "Este datele brute ale execuției. Seturile aparțin sesiunii, nu template-ului.",
        "presentation": "Regula cheie: workout_sets -> session_exercises -> workout_sessions, nu direct spre template_day_exercises.",
    },
    "coach_session_comments": {
        "role": "Comentariile coach-ului pe o sesiune a clientului.",
        "why": "Adaugă feedback de coaching fără să modifice datele sesiunii.",
        "presentation": "Leagă coach, client și session; datele clientului rămân controlate prin ReBAC în servicii.",
    },
    "template_votes": {
        "role": "Voturile UP/DOWN ale utilizatorilor pentru template-uri publice.",
        "why": "Un user poate avea un singur vot per template datorită cheii compuse.",
        "presentation": "Face parte din marketplace și alimentează template_stats.",
    },
    "template_saves": {
        "role": "Template-urile salvate de utilizatori din marketplace.",
        "why": "Cheia compusă împiedică salvări duplicate pentru același user/template.",
        "presentation": "Separă interacțiunea de salvare de copierea efectivă a template-ului.",
    },
    "template_use_events": {
        "role": "Evenimente de copiere/folosire a unui template public.",
        "why": "Păstrează audit și snapshot de nume pentru template-ul sursă și copia creată.",
        "presentation": "Este snapshot de marketplace/audit, nu parte din istoricul sesiunii.",
    },
    "coach_template_assignments": {
        "role": "Template-uri atribuite de coach unui client.",
        "why": "Permite workflow de coaching în care coach-ul recomandă programe.",
        "presentation": "Statusul ASSIGNED/COMPLETED/REVOKED urmărește ciclul atribuirii.",
    },
    "template_analysis_results": {
        "role": "Rezultatele analizatorului de template-uri: scor, categorie, warnings, positives, notes.",
        "why": "Persistă rezultatele analizei deterministe și permite consultarea ulterioară.",
        "presentation": "Warnings/positives/notes sunt JSONB pentru flexibilitate.",
    },
    "outbox_events": {
        "role": "Tabel de suport pentru evenimente asincrone, în proiect folosit mai ales pentru indexare/search.",
        "why": "Permite retry și procesare controlată a evenimentelor după tranzacții.",
        "presentation": "Nu este funcționalitate de business vizibilă, ci infrastructură pentru consistență operațională.",
    },
}


def p(text: str, style: str = "Body") -> Paragraph:
    return Paragraph(str(text), S[style])


def code(text: str) -> Paragraph:
    return Paragraph(text.replace("\n", "<br/>"), S["CodeBlock"])


def bullets(items: list[str]) -> ListFlowable:
    return ListFlowable(
        [ListItem(p(item), leftIndent=0) for item in items],
        bulletType="bullet",
        bulletFontName=BASE_FONT,
        bulletFontSize=7,
        leftIndent=14,
        bulletIndent=4,
    )


def split_top_level(body: str) -> list[str]:
    parts: list[str] = []
    current: list[str] = []
    depth = 0
    for ch in body:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            part = "".join(current).strip()
            if part:
                parts.append(part)
            current = []
        else:
            current.append(ch)
    part = "".join(current).strip()
    if part:
        parts.append(part)
    return parts


def parse_schema() -> tuple[dict[str, TableInfo], list[tuple[str, str, str]], dict[str, list[str]]]:
    sql = V1.read_text(encoding="utf-8")
    tables: dict[str, TableInfo] = {}
    edges: list[tuple[str, str, str]] = []
    for m in re.finditer(r"CREATE TABLE\s+(\w+)\s*\((.*?)\);", sql, re.S):
        name, body = m.group(1), m.group(2)
        columns: list[Column] = []
        constraints: list[str] = []
        for item in split_top_level(body):
            one = re.sub(r"\s+", " ", item.strip())
            if not one:
                continue
            if one.startswith("CONSTRAINT") or one.startswith("PRIMARY KEY"):
                constraints.append(one)
                continue
            cm = re.match(r"(\w+)\s+(.+)", one)
            if not cm:
                continue
            col_name, rest = cm.group(1), cm.group(2)
            type_match = re.match(r"([a-zA-Z0-9_]+(?:\([^)]*\))?(?:\[\])?)", rest)
            type_name = type_match.group(1) if type_match else rest.split()[0]
            default = None
            dm = re.search(r"\bDEFAULT\s+(.+?)(?:\s+CONSTRAINT|\s+NOT NULL|\s+PRIMARY KEY|\s+REFERENCES|$)", rest)
            if dm:
                default = dm.group(1).strip()
            rm = re.search(r"REFERENCES\s+(\w+)\s*\(", rest)
            foreign = rm.group(1) if rm else None
            action = None
            am = re.search(r"ON DELETE\s+([A-Z ]+)", rest)
            if am:
                action = am.group(1).strip()
            if foreign:
                edges.append((name, col_name, foreign))
            columns.append(Column(
                name=col_name,
                type_name=type_name,
                raw=one,
                primary_key="PRIMARY KEY" in rest,
                not_null="NOT NULL" in rest or "PRIMARY KEY" in rest,
                default=default,
                foreign_table=foreign,
                foreign_action=action,
                snapshot=("_snapshot" in col_name) or (
                    name == "template_day_exercise_muscle_groups" and col_name in {"muscle_group_code", "role"}
                ),
            ))
        tables[name] = TableInfo(name, columns, constraints, [])

    index_map: dict[str, list[str]] = {name: [] for name in tables}
    statements = [stmt.strip() + ";" for stmt in sql.split(";") if "CREATE " in stmt]
    for stmt in statements:
        line = re.sub(r"\s+", " ", stmt.strip())
        if not line.startswith("CREATE ") or " INDEX " not in line:
            continue
        tm = re.search(r"\bON\s+(\w+)\s*\(", line)
        if tm and tm.group(1) in index_map:
            index_map[tm.group(1)].append(line)
    v3 = V3.read_text(encoding="utf-8")
    for stmt in [s.strip() + ";" for s in v3.split(";") if "CREATE " in s]:
        line = re.sub(r"\s+", " ", stmt.strip())
        tm = re.search(r"\bON\s+(\w+)\s*\(", line)
        if tm and tm.group(1) in index_map:
            index_map[tm.group(1)].append(line + " [V3]")
    for table, indexes in index_map.items():
        tables[table].indexes = indexes
    return tables, edges, index_map


def table_flow(data: list[list[str]], widths: list[float], header_color=BLUE, repeat_rows=1) -> Table:
    wrapped = []
    for row_idx, row in enumerate(data):
        row_style = "TableHead" if row_idx < repeat_rows else "TableCell"
        cells = []
        for col_idx, cell in enumerate(row):
            style = "TableHead" if row_idx < repeat_rows else ("MonoCell" if col_idx <= 1 else "TableCell")
            cells.append(Paragraph(cell, S[style]))
        wrapped.append(cells)
    t = Table(wrapped, colWidths=widths, hAlign="LEFT", repeatRows=repeat_rows)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, repeat_rows - 1), header_color),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 4.5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4.5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
        ("ROWBACKGROUNDS", (0, repeat_rows), (-1, -1), [colors.white, colors.HexColor("#fafbfc")]),
    ]))
    return t


class MiniFlow(Flowable):
    def __init__(self, steps: list[tuple[str, str, colors.Color]], width: float = 17 * cm, height: float = 38 * mm):
        super().__init__()
        self.steps = steps
        self.width = width
        self.height = height

    def wrap(self, availWidth, availHeight):
        self.width = min(self.width, availWidth)
        return self.width, self.height

    def draw(self):
        c = self.canv
        margin = 4 * mm
        gap = 4 * mm
        n = len(self.steps)
        box_w = (self.width - 2 * margin - gap * (n - 1)) / n
        box_h = self.height - 2 * margin
        y = margin
        for i, (title, body, fill) in enumerate(self.steps):
            x = margin + i * (box_w + gap)
            c.setFillColor(fill)
            c.setStrokeColor(LINE)
            c.roundRect(x, y, box_w, box_h, 6, stroke=1, fill=1)
            c.setFillColor(INK)
            c.setFont(BOLD_FONT, 8)
            c.drawCentredString(x + box_w / 2, y + box_h - 11, title)
            c.setFillColor(MUTED)
            c.setFont(BASE_FONT, 7.2)
            for idx, line in enumerate(body.split("\n")[:3]):
                c.drawCentredString(x + box_w / 2, y + box_h - 23 - idx * 8.5, line)
            if i < n - 1:
                ax = x + box_w + 1.2 * mm
                ay = y + box_h / 2
                bx = x + box_w + gap - 1.6 * mm
                c.setStrokeColor(ORANGE)
                c.setLineWidth(1)
                c.line(ax, ay, bx, ay)
                c.setFillColor(ORANGE)
                c.line(bx, ay, bx - 3, ay + 2.4)
                c.line(bx, ay, bx - 3, ay - 2.4)


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont(BASE_FONT, 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(doc.leftMargin, A4[1] - 11 * mm, "Platforma workout - ghid bază de date")
    canvas.drawRightString(A4[0] - doc.rightMargin, 9 * mm, f"Pagina {doc.page}")
    canvas.setStrokeColor(LINE)
    canvas.line(doc.leftMargin, A4[1] - 14 * mm, A4[0] - doc.rightMargin, A4[1] - 14 * mm)
    canvas.restoreState()


def header_footer_landscape(canvas, doc):
    canvas.saveState()
    canvas.setFont(BASE_FONT, 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(doc.leftMargin, landscape(A4)[1] - 11 * mm, "Platforma workout - ghid bază de date")
    canvas.drawRightString(landscape(A4)[0] - doc.rightMargin, 9 * mm, f"Pagina {doc.page}")
    canvas.setStrokeColor(LINE)
    canvas.line(doc.leftMargin, landscape(A4)[1] - 14 * mm, landscape(A4)[0] - doc.rightMargin, landscape(A4)[1] - 14 * mm)
    canvas.restoreState()


def tags_for_column(col: Column) -> str:
    tags = []
    if col.primary_key:
        tags.append("PK")
    if col.foreign_table:
        tags.append("FK")
    if col.not_null:
        tags.append("NOT NULL")
    if col.snapshot:
        tags.append("SNAPSHOT")
    if col.default:
        tags.append(f"DEFAULT {col.default}")
    if col.foreign_action:
        tags.append(f"ON DELETE {col.foreign_action}")
    return ", ".join(tags)


def explain_column(table: str, col: Column) -> str:
    name = col.name
    if name == "id":
        return "Identificator intern UUID."
    if name.endswith("_at"):
        return "Timestamp pentru lifecycle/audit."
    if name == "deleted_at":
        return "Soft delete; rândul rămâne în DB, dar nu mai este considerat activ."
    if name == "user_id" or name.endswith("_user_id") or name == "owner_user_id":
        return "Legătură de ownership / utilizator implicat."
    if name == "password_hash":
        return "Parolă stocată hash-uit, nu în clar."
    if name == "token_hash":
        return "Fingerprint SHA-256 al refresh token-ului."
    if name == "revoked_at":
        return "Marchează token/relație ca revocat(ă)."
    if "snapshot" in name:
        return "Copie istorică / de planificare, nu valoare citită live din tabela sursă."
    if name in {"status", "role", "visibility", "exercise_type", "routine_type", "set_type", "difficulty", "split_type", "focus", "category"}:
        return "Valoare controlată prin CHECK constraint / enum logic."
    if name.startswith("aggregated_"):
        return "Câmp denormalizat pentru căutare/filtrare rapidă."
    if col.foreign_table:
        return f"Referință către {col.foreign_table}."
    return ""


def build_column_table(info: TableInfo) -> Table:
    rows = [["Coloană", "Tip", "Reguli", "Explicație"]]
    for col in info.columns:
        rows.append([
            col.name,
            col.type_name,
            tags_for_column(col),
            explain_column(info.name, col),
        ])
    return table_flow(rows, [4.0 * cm, 3.0 * cm, 4.6 * cm, 5.1 * cm], header_color=BLUE)


def table_color(name: str):
    if name in SNAPSHOT_HISTORY:
        return ORANGE
    if name in SNAPSHOT_PLANNING:
        return colors.HexColor("#b88600")
    return BLUE


def table_badge(name: str) -> str:
    if name in SNAPSHOT_HISTORY:
        return "snapshot istoric"
    if name in SNAPSHOT_PLANNING:
        return "snapshot planificare/audit"
    return "tabel normal"


def table_section(name: str, info: TableInfo) -> list:
    desc = DESCRIPTIONS[name]
    color = table_color(name)
    rels_out = [f"{c.name} -> {c.foreign_table}" + (f" ({c.foreign_action})" if c.foreign_action else "") for c in info.columns if c.foreign_table]
    snapshot_cols = [c.name for c in info.columns if c.snapshot]
    story = []
    story.append(Spacer(1, 5))
    story.append(p(f"{name} - {table_badge(name)}", "H1"))
    story.append(p(desc["role"]))
    story.append(p(f"<b>De ce există:</b> {desc['why']}"))
    if rels_out or snapshot_cols:
        facts = []
        if rels_out:
            facts.append("<b>Relații FK:</b> " + "; ".join(rels_out))
        if snapshot_cols:
            facts.append("<b>Coloane snapshot:</b> " + ", ".join(snapshot_cols))
        story.append(p("<br/>".join(facts), "Callout"))
    story.append(build_column_table(info))
    short_constraints = info.constraints[:5]
    if short_constraints:
        story.append(p("Constrângeri importante", "H2"))
        story.append(bullets([constraint for constraint in short_constraints]))
    if info.indexes:
        story.append(p("Indecși", "H2"))
        story.append(bullets([idx for idx in info.indexes[:6]]))
    story.append(p(f"<b>Ce spui la prezentare:</b> {desc['presentation']}", "Callout"))
    return story


def build_story() -> list:
    tables, edges, _ = parse_schema()
    story: list = []
    story.append(Spacer(1, 26))
    story.append(p("Baza de date a platformei workout", "TitleBig"))
    story.append(p("Ghid detaliat pentru prezentare - fiecare tabel, relații, snapshot-uri și reguli de integritate", "Subtitle"))
    story.append(p(
        "Documentul este generat pe baza migrațiilor Flyway: V1 creează schema completă, V2 adaugă exercițiile oficiale, "
        "iar V3 adaugă regula finală pentru un singur workout activ per utilizator.",
        "Callout",
    ))
    story.append(table_flow([
        ["Metrică", "Valoare", "De ce contează"],
        ["Tabele", str(len(tables)), "Schema acoperă auth, planificare, istoric, marketplace, coaching, analizator și outbox."],
        ["Relații FK", str(len(edges)), "Datele sunt legate prin chei străine, nu prin id-uri libere."],
        ["Migrații", "3", "Flyway controlează schema și seed-ul oficial."],
        ["Snapshot-uri istorice", "5 tabele", "Istoricul rămâne stabil după modificări ale planului."],
        ["Snapshot-uri planificare/audit", "4 tabele", "Template-urile/copierile păstrează contextul inițial."],
    ], [3.6 * cm, 3.0 * cm, 9.6 * cm], header_color=BLUE))

    story.append(PageBreak())
    story.append(p("1. Diagrama generală", "H1"))
    story.append(p(
        "În diagramă, portocaliu înseamnă snapshot istoric pentru sesiuni, galben înseamnă snapshot de planificare/audit, "
        "iar gri/alb înseamnă tabele normale.",
    ))
    if DIAGRAM_OVERVIEW.exists():
        img = Image(str(DIAGRAM_OVERVIEW))
        max_w = 17.0 * cm
        ratio = max_w / img.imageWidth
        img.drawWidth = max_w
        img.drawHeight = img.imageHeight * ratio
        story.append(img)
    story.append(p(
        "Pentru slide-uri, folosește această diagramă ca overview. Pentru explicații orale, important este firul: "
        "user -> planificare -> template -> sesiune -> istoric -> marketplace/coaching/search.",
        "Callout",
    ))

    story.append(PageBreak())
    story.append(p("2. Ideea centrală a modelului de date", "H1"))
    story.append(MiniFlow([
        ("Date editabile", "exercises, routines\ngyms, equipment", LIGHT_BLUE),
        ("Template", "workout_templates\ntemplate_days", LIGHT_GREEN),
        ("Start sesiune", "copiere controlată\nîn tabele istorice", LIGHT_ORANGE),
        ("Istoric stabil", "workout_sessions\nsession_exercises\nworkout_sets", LIGHT_RED),
    ]))
    story.append(p(
        "Baza de date separă planificarea de istoric. Planificarea este normal să se modifice în timp: utilizatorul schimbă "
        "programul, exercițiile, seturile planificate, sala sau echipamentul. Istoricul nu trebuie să fie rescris retroactiv.",
    ))
    story.append(p(
        "Regula cea mai importantă: <b>workout_sets -> session_exercises -> workout_sessions</b>. "
        "Seturile aparțin unei sesiuni executate, nu unui template editabil.",
        "Callout",
    ))
    story.append(table_flow([
        ["Categorie", "Tabele"],
        ["Date editabile", "exercises, routines, gyms, equipment, workout_templates, template_days"],
        ["Snapshot de planificare", "template_day_exercises, template_day_exercise_muscle_groups, template_day_routines"],
        ["Snapshot istoric", "workout_sessions, session_exercises, session_exercise_muscle_groups, session_routines, workout_sets"],
        ["Marketplace/audit", "template_stats, template_votes, template_saves, template_use_events"],
        ["Coaching", "coach_profiles, coach_client_relationships, coach_session_comments, coach_template_assignments"],
        ["Infrastructură", "refresh_tokens, outbox_events, template_analysis_results"],
    ], [4.3 * cm, 11.7 * cm], header_color=GREEN))

    story.append(PageBreak())
    story.append(p("3. Migrațiile Flyway", "H1"))
    story.append(table_flow([
        ["Migrare", "Rol"],
        ["V1__full_initial_schema.sql", "Creează extensia pgcrypto, cele 28 de tabele, constrângeri, indecși și seed-ul pentru muscle_groups."],
        ["V2__seed_official_exercises.sql", "Adaugă 14 exerciții oficiale și mapările lor musculare. Nu schimbă schema."],
        ["V3__one_active_workout_per_user.sql", "Adaugă indexul unic parțial ux_workout_sessions_one_active_per_user pentru status IN_PROGRESS."],
    ], [5.2 * cm, 11.2 * cm], header_color=PURPLE))
    story.append(code(
        "CREATE UNIQUE INDEX ux_workout_sessions_one_active_per_user\n"
        "ON workout_sessions (user_id)\n"
        "WHERE status = 'IN_PROGRESS';"
    ))
    story.append(p(
        "Această regulă este importantă deoarece verificarea din service poate fi depășită de două request-uri concurente. "
        "Indexul unic parțial face baza de date sursa finală de adevăr.",
        "Callout",
    ))

    story.append(PageBreak())
    story.append(p("4. Tabele pe module", "H1"))
    for title, names in MODULES:
        story.append(p(title, "H2"))
        story.append(bullets([f"<b>{name}</b> - {DESCRIPTIONS[name]['role']}" for name in names]))

    for module_title, names in MODULES:
        story.append(PageBreak())
        story.append(p(module_title, "H1"))
        story.append(p("Următoarele pagini explică fiecare tabel din acest modul: rol, relații, coloane, constrângeri și ce poți spune la prezentare."))
        for idx, name in enumerate(names):
            if idx > 0:
                story.append(PageBreak())
            story.extend(table_section(name, tables[name]))

    story.append(PageBreak())
    story.append(p("5. Snapshot-urile într-o singură pagină", "H1"))
    story.append(table_flow([
        ["Tabel", "Tip snapshot", "Ce păstrează"],
        ["workout_sessions", "Istoric", "template_name_snapshot, template_day_name_snapshot, gym_name_snapshot"],
        ["session_exercises", "Istoric", "exercise_name/type snapshot + planned sets/reps/weight/rest/note snapshot"],
        ["session_exercise_muscle_groups", "Istoric", "muscle_group_code_snapshot, role_snapshot"],
        ["session_routines", "Istoric", "routine_name_snapshot, routine_content_snapshot"],
        ["workout_sets", "Istoric", "equipment_name_snapshot + valorile executate efectiv"],
        ["template_day_exercises", "Planificare", "exercise_name_snapshot, exercise_type_snapshot"],
        ["template_day_exercise_muscle_groups", "Planificare", "muscle_group_code, role - funcțional snapshot, fără sufix"],
        ["template_day_routines", "Planificare", "routine_name_snapshot, routine_content_snapshot"],
        ["template_use_events", "Audit marketplace", "source_template_name_snapshot, copied_template_name_snapshot"],
    ], [4.3 * cm, 3.5 * cm, 8.5 * cm], header_color=ORANGE))
    story.append(p(
        "Snapshot în acest proiect nu înseamnă backup complet. Înseamnă copierea controlată a câmpurilor care trebuie să rămână adevărate "
        "pentru istoric sau pentru o copie independentă.",
        "Callout",
    ))

    story.append(PageBreak())
    story.append(p("6. Răspuns rapid pentru comisie", "H1"))
    story.append(p(
        "Modelul de date este construit în jurul separării dintre planificare și istoric. Datele de planificare sunt editabile: "
        "exerciții, rutine, săli, echipamente și template-uri. În momentul pornirii unei sesiuni, aplicația copiază câmpurile "
        "relevante în tabele istorice: workout_sessions, session_exercises, session_exercise_muscle_groups, session_routines și workout_sets. "
        "Astfel, dacă utilizatorul modifică ulterior programul, istoricul rămâne corect.",
    ))
    story.append(p(
        "Cheile străine sunt folosite pentru integritate și navigare, dar multe referințe către planificare sunt nullable și au ON DELETE SET NULL. "
        "Adevărul istoric este păstrat în coloanele snapshot. Pentru securitate și integritate, schema mai include ownership prin user_id, "
        "soft delete, constrângeri CHECK, indecși unici parțiali și relații coach-client folosite de ReBAC.",
        "Callout",
    ))
    return story


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(
        str(OUT),
        pagesize=A4,
        rightMargin=15 * mm,
        leftMargin=15 * mm,
        topMargin=19 * mm,
        bottomMargin=15 * mm,
        title="Baza de date a platformei workout",
        author="Codex",
    )
    doc.build(build_story(), onFirstPage=header_footer, onLaterPages=header_footer)
    print(OUT)


if __name__ == "__main__":
    main()
