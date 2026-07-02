from __future__ import annotations

from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import cm, mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Flowable,
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
OUT = ROOT / "output/pdf/securitate_platforma_workout.pdf"


INK = colors.HexColor("#15171c")
MUTED = colors.HexColor("#5f6673")
BLUE = colors.HexColor("#1f4e79")
GREEN = colors.HexColor("#2f7d55")
ORANGE = colors.HexColor("#c76f00")
RED = colors.HexColor("#b83a4b")
PURPLE = colors.HexColor("#74539B")
PANEL = colors.HexColor("#f5f7fb")
LINE = colors.HexColor("#d7dde8")
LIGHT_ORANGE = colors.HexColor("#fff3df")
LIGHT_GREEN = colors.HexColor("#eaf7ef")
LIGHT_BLUE = colors.HexColor("#eaf2fb")
LIGHT_RED = colors.HexColor("#fdecef")


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
        fontSize=25,
        leading=30,
        textColor=INK,
        alignment=TA_CENTER,
        spaceAfter=10,
    ))
    styles.add(ParagraphStyle(
        "Subtitle",
        parent=styles["Normal"],
        fontName=BASE_FONT,
        fontSize=11.5,
        leading=16,
        textColor=MUTED,
        alignment=TA_CENTER,
        spaceAfter=14,
    ))
    styles.add(ParagraphStyle(
        "H1",
        parent=styles["Heading1"],
        fontName=BOLD_FONT,
        fontSize=18,
        leading=23,
        textColor=BLUE,
        spaceBefore=4,
        spaceAfter=8,
    ))
    styles.add(ParagraphStyle(
        "H2",
        parent=styles["Heading2"],
        fontName=BOLD_FONT,
        fontSize=13.5,
        leading=18,
        textColor=INK,
        spaceBefore=8,
        spaceAfter=5,
    ))
    styles.add(ParagraphStyle(
        "Body",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=10.2,
        leading=14.5,
        textColor=INK,
        spaceAfter=6,
    ))
    styles.add(ParagraphStyle(
        "Small",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=8.8,
        leading=12,
        textColor=MUTED,
        spaceAfter=4,
    ))
    styles.add(ParagraphStyle(
        "CodeBlock",
        parent=styles["BodyText"],
        fontName=MONO_FONT,
        fontSize=8.6,
        leading=11.2,
        textColor=colors.HexColor("#273142"),
        backColor=colors.HexColor("#f4f6f8"),
        borderColor=colors.HexColor("#e1e6ee"),
        borderWidth=0.4,
        borderPadding=5,
        spaceAfter=7,
    ))
    styles.add(ParagraphStyle(
        "Callout",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=10,
        leading=14,
        textColor=INK,
        backColor=colors.HexColor("#fff8eb"),
        borderColor=colors.HexColor("#f1c982"),
        borderWidth=0.8,
        borderPadding=7,
        spaceBefore=4,
        spaceAfter=8,
    ))
    styles.add(ParagraphStyle(
        "TableHead",
        parent=styles["BodyText"],
        fontName=BOLD_FONT,
        fontSize=8.8,
        leading=11,
        textColor=colors.white,
        alignment=TA_LEFT,
    ))
    styles.add(ParagraphStyle(
        "TableCell",
        parent=styles["BodyText"],
        fontName=BASE_FONT,
        fontSize=8.5,
        leading=11.5,
        textColor=INK,
    ))
    return styles


S = make_styles()


def p(text: str, style: str = "Body") -> Paragraph:
    return Paragraph(text, S[style])


def code(text: str) -> Paragraph:
    return Paragraph(text.replace("\n", "<br/>"), S["CodeBlock"])


def bullets(items: list[str], level: int = 0) -> ListFlowable:
    return ListFlowable(
        [ListItem(p(item), leftIndent=0) for item in items],
        bulletType="bullet",
        bulletFontName=BASE_FONT,
        bulletFontSize=7.5,
        leftIndent=14 + level * 10,
        bulletIndent=3 + level * 10,
        start=None,
    )


def make_table(data: list[list[str]], widths: list[float] | None = None, header_color=BLUE) -> Table:
    wrapped = []
    for row_idx, row in enumerate(data):
        style = "TableHead" if row_idx == 0 else "TableCell"
        wrapped.append([Paragraph(cell, S[style]) for cell in row])
    table = Table(wrapped, colWidths=widths, hAlign="LEFT", repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), header_color),
        ("GRID", (0, 0), (-1, -1), 0.35, LINE),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 6),
        ("RIGHTPADDING", (0, 0), (-1, -1), 6),
        ("TOPPADDING", (0, 0), (-1, -1), 5),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 5),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#fafbfc")]),
    ]))
    return table


class FlowDiagram(Flowable):
    def __init__(self, steps: list[tuple[str, str, colors.Color]], width: float = 17 * cm, height: float = 48 * mm):
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
        n = len(self.steps)
        gap = 5 * mm
        box_w = (self.width - 2 * margin - gap * (n - 1)) / n
        box_h = self.height - 2 * margin
        y = margin
        for i, (title, body, fill) in enumerate(self.steps):
            x = margin + i * (box_w + gap)
            c.setFillColor(fill)
            c.setStrokeColor(colors.HexColor("#c9d3df"))
            c.roundRect(x, y, box_w, box_h, 7, stroke=1, fill=1)
            c.setFillColor(INK)
            c.setFont(BOLD_FONT, 9)
            c.drawCentredString(x + box_w / 2, y + box_h - 12, title)
            c.setFillColor(MUTED)
            c.setFont(BASE_FONT, 7.7)
            lines = body.split("\n")
            start_y = y + box_h - 25
            for line_idx, line in enumerate(lines[:4]):
                c.drawCentredString(x + box_w / 2, start_y - line_idx * 10, line)
            if i < n - 1:
                ax = x + box_w + 1.3 * mm
                ay = y + box_h / 2
                bx = x + box_w + gap - 1.5 * mm
                c.setStrokeColor(ORANGE)
                c.setLineWidth(1.2)
                c.line(ax, ay, bx, ay)
                c.setFillColor(ORANGE)
                c.line(bx, ay, bx - 3.5, ay + 2.7)
                c.line(bx, ay, bx - 3.5, ay - 2.7)


class LayerDiagram(Flowable):
    def __init__(self):
        super().__init__()
        self.width = 17 * cm
        self.height = 62 * mm

    def wrap(self, availWidth, availHeight):
        self.width = min(self.width, availWidth)
        return self.width, self.height

    def draw(self):
        c = self.canv
        labels = [
            ("1. Autentificare", "JWT validat\nUserPrincipal creat", LIGHT_BLUE, BLUE),
            ("2. RBAC", "ADMIN / COACH / USER\npe rute mari", LIGHT_GREEN, GREEN),
            ("3. Ownership", "resursa apartine\nutilizatorului curent", LIGHT_ORANGE, ORANGE),
            ("4. ReBAC", "coach-client ACTIVE\npentru coaching", LIGHT_RED, RED),
        ]
        x = 7 * mm
        y = 8 * mm
        w = self.width - 14 * mm
        h = 11 * mm
        for idx, (title, body, fill, stroke) in enumerate(labels):
            yy = y + (len(labels) - 1 - idx) * (h + 3 * mm)
            c.setFillColor(fill)
            c.setStrokeColor(stroke)
            c.roundRect(x, yy, w, h, 5, stroke=1, fill=1)
            c.setFont(BOLD_FONT, 9)
            c.setFillColor(stroke)
            c.drawString(x + 6 * mm, yy + 6.8 * mm, title)
            c.setFont(BASE_FONT, 8)
            c.setFillColor(INK)
            for j, line in enumerate(body.split("\n")):
                c.drawRightString(x + w - 6 * mm, yy + 7.2 * mm - j * 8.5, line)


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFont(BASE_FONT, 8)
    canvas.setFillColor(MUTED)
    canvas.drawString(doc.leftMargin, A4[1] - 11 * mm, "Platforma workout - ghid securitate")
    canvas.drawRightString(A4[0] - doc.rightMargin, 9 * mm, f"Pagina {doc.page}")
    canvas.setStrokeColor(LINE)
    canvas.line(doc.leftMargin, A4[1] - 14 * mm, A4[0] - doc.rightMargin, A4[1] - 14 * mm)
    canvas.restoreState()


def section(title: str) -> list:
    return [Spacer(1, 5), p(title, "H1")]


def build_story() -> list:
    story = []
    story.append(Spacer(1, 32))
    story.append(p("Securitatea aplicației workout", "TitleBig"))
    story.append(p("Ghid de învățat pentru prezentare - autentificare, autorizare, IDOR, RBAC și ReBAC", "Subtitle"))
    story.append(Spacer(1, 10))
    story.append(p(
        "Acest PDF explică partea de securitate a proiectului exact pe implementarea din cod. "
        "Ideea centrală este că aplicația nu verifică doar dacă utilizatorul este logat, ci verifică "
        "și dacă acel utilizator are dreptul concret să acceseze resursa cerută.",
        "Callout",
    ))
    story.append(Spacer(1, 8))
    story.append(make_table([
        ["Componentă", "Implementare în proiect", "Rol"],
        ["Autentificare", "JWT access token + refresh token opac", "Identifică utilizatorul curent"],
        ["Stocare parole", "BCryptPasswordEncoder", "Parolele nu sunt salvate în clar"],
        ["Refresh token", "Cookie HttpOnly + SHA-256 fingerprint în DB", "Reînnoiește sesiunea fără login repetat"],
        ["RBAC", "SecurityConfig: /api/admin/**, /api/coach/**", "Controlează zone mari ale aplicației"],
        ["Ownership / IDOR", "TemplateAccess, WorkoutSessionAccess, repository-uri owner-scoped", "Blochează accesul la resursele altui user"],
        ["ReBAC", "CoachAccess + coach_client_relationships ACTIVE", "Permite acces coach doar la clienții reali"],
    ], [3.0 * cm, 6.4 * cm, 6.2 * cm]))
    story.append(PageBreak())

    story += section("1. Harta generală a securității")
    story.append(p(
        "Securitatea este pe mai multe straturi. Primul strat identifică utilizatorul, dar asta nu este suficient. "
        "După autentificare, fiecare operație trebuie să verifice rolul, proprietarul resursei sau relația coach-client.",
    ))
    story.append(LayerDiagram())
    story.append(p("Formula scurtă pentru prezentare:", "H2"))
    story.append(p(
        "Autentificarea răspunde la întrebarea <b>cine ești?</b>. Autorizarea răspunde la întrebarea "
        "<b>ai voie să accesezi această resursă?</b> În aplicație, cele două sunt separate clar.",
        "Callout",
    ))
    story.append(p("Zonele de cod relevante:", "H2"))
    story.append(bullets([
        "<b>SecurityConfig</b> - definește rutele publice, rutele autentificate și rolurile cerute.",
        "<b>JwtAuthenticationFilter</b> - citește token-ul din header și populează SecurityContext.",
        "<b>AuthService</b> - login, register, refresh, logout, hashing parole și refresh tokens.",
        "<b>TemplateAccess / WorkoutSessionAccess</b> - ownership checks și protecție IDOR.",
        "<b>CoachAccess</b> - poarta ReBAC pentru relația activă coach-client.",
    ]))

    story += section("2. Register și login")
    story.append(p(
        "La register, backend-ul normalizează emailul, verifică dacă există deja, hash-uiește parola cu BCrypt "
        "și creează userul cu rolul USER. La login, backend-ul caută userul după email și verifică parola cu BCrypt.",
    ))
    story.append(FlowDiagram([
        ("Frontend", "email + parolă\nPOST /api/auth/login", LIGHT_BLUE),
        ("AuthService", "caută user\nverifică BCrypt", LIGHT_GREEN),
        ("JWT", "emite access token\n15 minute", LIGHT_ORANGE),
        ("Refresh", "emite token opac\ncookie HttpOnly", LIGHT_RED),
    ]))
    story.append(make_table([
        ["Endpoint", "Ce face", "Rezultat"],
        ["POST /api/auth/register", "Creează cont nou, parola devine BCrypt hash", "201 + access token + refresh cookie"],
        ["POST /api/auth/login", "Verifică email și parolă", "200 + access token + refresh cookie"],
        ["GET /api/auth/me", "Folosește UserPrincipal din JWT", "Datele userului curent"],
    ], [4.2 * cm, 6.4 * cm, 5.0 * cm]))
    story.append(p(
        "Important: access token-ul este în body-ul răspunsului, dar refresh token-ul nu este expus în body. "
        "El este pus de backend într-un cookie HttpOnly.",
        "Callout",
    ))

    story += section("3. Access token JWT")
    story.append(p(
        "Access token-ul este JWT-ul folosit la cererile normale către API. Frontend-ul îl trimite în headerul "
        "Authorization. Backend-ul îl validează la fiecare request protejat.",
    ))
    story.append(code(
        "Authorization: Bearer <access_token>\n\n"
        "Claims din JWT:\n"
        "- subject = userId\n"
        "- email\n"
        "- role\n"
        "- type = access\n"
        "- issuer\n"
        "- expiration"
    ))
    story.append(p("Ce verifică JwtService.parseAccessToken:", "H2"))
    story.append(bullets([
        "semnătura token-ului, folosind cheia HS256;",
        "issuer-ul configurat;",
        "expirarea token-ului;",
        "claim-ul <b>type = access</b>, ca să nu fie acceptat alt tip de token;",
        "formatul claim-urilor: userId valid, rol valid.",
    ]))
    story.append(p(
        "JWT-ul nu este gândit să fie rotit la fiecare request. El este refolosit până expiră. "
        "În proiect, durata access token-ului este 15 minute.",
        "Callout",
    ))

    story += section("4. Cum intră userul curent în backend")
    story.append(p(
        "JwtAuthenticationFilter este filtrul care transformă token-ul din header într-un utilizator curent disponibil în cod.",
    ))
    story.append(FlowDiagram([
        ("Request", "Authorization:\nBearer JWT", LIGHT_BLUE),
        ("Filter", "parseAccessToken\nvalidare semnătură", LIGHT_GREEN),
        ("Principal", "UserPrincipal\nid, email, role", LIGHT_ORANGE),
        ("Controller", "@AuthenticationPrincipal\nprincipal.id()", LIGHT_RED),
    ]))
    story.append(code(
        "@GetMapping(\"/{templateId}\")\n"
        "public TemplateDetailResponse get(@AuthenticationPrincipal UserPrincipal principal,\n"
        "        @PathVariable UUID templateId) {\n"
        "    return templateService.get(principal.id(), templateId);\n"
        "}"
    ))
    story.append(p(
        "Asta este motivul pentru care frontend-ul nu trebuie să trimită userId pentru operațiile personale. "
        "Identitatea vine din JWT-ul validat pe backend, nu din parametrii trimiși de client.",
        "Callout",
    ))

    story += section("5. Refresh token: ce este și când se schimbă")
    story.append(p(
        "Refresh token-ul este folosit doar ca să obțină o pereche nouă de token-uri când access token-ul expiră. "
        "El nu este trimis la fiecare operație normală. Cererile normale folosesc același access token până la expirare.",
    ))
    story.append(make_table([
        ["Operație", "Se schimbă access token?", "Se schimbă refresh token?"],
        ["GET /api/history", "Nu", "Nu"],
        ["POST /api/workouts/start", "Nu", "Nu"],
        ["POST /api/auth/login", "Da", "Da"],
        ["POST /api/auth/register", "Da", "Da"],
        ["POST /api/auth/refresh", "Da", "Da, token-ul vechi este revocat"],
        ["POST /api/auth/logout", "Nu emite token nou", "Token-ul curent este revocat"],
    ], [5.0 * cm, 5.0 * cm, 5.6 * cm], header_color=ORANGE))
    story.append(FlowDiagram([
        ("Access expirat", "frontend primește 401\nsau detectează expirarea", LIGHT_BLUE),
        ("Refresh endpoint", "POST /api/auth/refresh\ncookie trimis automat", LIGHT_GREEN),
        ("DB check", "SHA-256(token)\nrevoked_at null", LIGHT_ORANGE),
        ("Rotație", "revocă vechiul\nemite pereche nouă", LIGHT_RED),
    ]))
    story.append(p(
        "Rotația înseamnă că refresh token-ul vechi este folosit o singură dată. După refresh, el este marcat revocat "
        "și nu mai poate fi reutilizat.",
        "Callout",
    ))

    story += section("6. De ce refresh token-ul este opac și în cookie HttpOnly")
    story.append(p(
        "Refresh token-ul din proiect nu este JWT. Este o valoare random, generată cu SecureRandom, de 32 bytes, "
        "encodată Base64 URL-safe. Se numește opac fiindcă nu conține informații citibile despre user.",
    ))
    story.append(make_table([
        ["Mecanism", "În proiect", "De ce ajută"],
        ["Opac", "Random 32 bytes, nu JWT", "Nu expune userId, email sau rol"],
        ["HttpOnly", "Cookie creat de RefreshCookieFactory", "JavaScript nu îl poate citi direct prin document.cookie"],
        ["SameSite=Strict", "Cookie trimis doar în context same-site", "Reduce riscul de CSRF"],
        ["Path=/api/auth", "Cookie disponibil doar pe rutele auth", "Nu circulă la toate endpoint-urile"],
        ["Secure", "false local, true în producție prin profil", "Pe HTTPS, cookie-ul nu pleacă pe HTTP"],
    ], [3.3 * cm, 5.4 * cm, 6.9 * cm], header_color=GREEN))
    story.append(p(
        "Dacă un atacator controlează complet browserul sau calculatorul, orice mecanism poate fi compromis. "
        "Scopul aici este să reducem riscurile normale: XSS simplu, token-uri accesibile în JavaScript, leak-uri din DB.",
        "Callout",
    ))

    story += section("7. SHA-256 fingerprint pentru refresh token")
    story.append(p(
        "În baza de date nu se păstrează refresh token-ul brut. Se păstrează doar amprenta lui SHA-256 în tabelul "
        "refresh_tokens, coloana token_hash.",
    ))
    story.append(code(
        "raw_refresh_token -> SHA-256 -> token_hash\n\n"
        "refresh_tokens:\n"
        "- id\n"
        "- user_id\n"
        "- token_hash\n"
        "- expires_at\n"
        "- revoked_at\n"
        "- created_at"
    ))
    story.append(p("Avantajele acestei decizii:", "H2"))
    story.append(bullets([
        "dacă baza de date este expusă, refresh token-urile brute nu pot fi folosite direct;",
        "backend-ul poate căuta token-ul primit calculând același hash;",
        "backend-ul poate revoca token-uri individuale prin revoked_at;",
        "rotația token-ului poate fi verificată în baza de date.",
    ]))
    story.append(p(
        "Diferență față de parolă: pentru parole se folosește BCrypt, pentru refresh token se folosește SHA-256. "
        "Asta are sens deoarece refresh token-ul este deja o valoare random cu entropie mare, nu o parolă aleasă de om.",
        "Callout",
    ))

    story += section("8. Parole BCrypt")
    story.append(p(
        "Parolele utilizatorilor sunt tratate diferit de refresh tokens. Parola este aleasă de om, deci poate fi slabă "
        "sau reutilizată. De aceea se folosește BCryptPasswordEncoder.",
    ))
    story.append(make_table([
        ["Moment", "Ce face codul"],
        ["Register", "passwordEncoder.encode(rawPassword) și salvează password_hash"],
        ["Login", "passwordEncoder.matches(rawPassword, user.getPasswordHash())"],
        ["Baza de date", "app_users.password_hash, niciodată parola în clar"],
    ], [4.5 * cm, 11.1 * cm], header_color=PURPLE))
    story.append(p(
        "BCrypt include salt și este intenționat costisitor. Asta face atacurile brute-force mult mai scumpe dacă hash-urile "
        "parolelor ar ajunge la un atacator.",
        "Callout",
    ))

    story.append(PageBreak())
    story += section("9. SecurityConfig: rute publice, rute protejate, roluri")
    story.append(p(
        "SecurityConfig definește regula de intrare în API. Aplicația este stateless, CSRF este dezactivat în mod deliberat, "
        "iar JwtAuthenticationFilter este pus înainte de UsernamePasswordAuthenticationFilter.",
    ))
    story.append(make_table([
        ["Rută", "Regulă"],
        ["POST /api/auth/register", "Public"],
        ["POST /api/auth/login", "Public"],
        ["POST /api/auth/refresh", "Public, dar are nevoie de cookie refresh valid"],
        ["POST /api/auth/logout", "Public, dar revocă refresh token-ul dacă există"],
        ["GET /api/health", "Public"],
        ["/api/admin/**", "ROLE_ADMIN"],
        ["/api/coach/**", "ROLE_COACH"],
        ["/api/**", "Utilizator autentificat"],
        ["restul", "SPA static assets și client routes"],
    ], [6.0 * cm, 9.6 * cm], header_color=BLUE))
    story.append(p(
        "CSRF este dezactivat pentru că operațiile normale nu folosesc cookie ca mecanism de autentificare, ci Authorization header. "
        "Cookie-ul sensibil este refresh token-ul, limitat la /api/auth și SameSite=Strict.",
        "Callout",
    ))

    story += section("10. RBAC: acces pe roluri")
    story.append(p(
        "RBAC înseamnă Role-Based Access Control. În proiect, rolurile sunt USER, COACH și ADMIN. "
        "Rolurile decid accesul la zone mari ale aplicației.",
    ))
    story.append(code(
        ".requestMatchers(\"/api/admin/**\").hasRole(\"ADMIN\")\n"
        ".requestMatchers(\"/api/coach/**\").hasRole(\"COACH\")\n"
        ".requestMatchers(\"/api/**\").authenticated()"
    ))
    story.append(p("Interpretare:", "H2"))
    story.append(bullets([
        "un user neautentificat primește 401 pe endpoint-uri protejate;",
        "un user autentificat fără rolul necesar primește 403;",
        "un coach are acces la zona /api/coach/** doar dacă are rol COACH;",
        "rolul COACH nu îi dă automat acces la toți clienții. Acolo intervine ReBAC.",
    ]))

    story.append(PageBreak())
    story += section("11. IDOR și ownership checks")
    story.append(p(
        "IDOR înseamnă Insecure Direct Object Reference. Problema apare când utilizatorul poate schimba un id din URL "
        "și accesează resursa altcuiva.",
    ))
    story.append(code(
        "Risc:\n"
        "GET /api/templates/111  -> template-ul meu\n"
        "GET /api/templates/222  -> template-ul altcuiva\n\n"
        "Protecție:\n"
        "backend-ul verifică id-ul resursei împreună cu userId-ul din JWT"
    ))
    story.append(p(
        "În proiect, acest lucru apare în repository-uri owner-scoped și în clase dedicate de access.",
    ))
    story.append(make_table([
        ["Zonă", "Mecanism"],
        ["Template direct", "findByIdAndUserIdAndDeletedAtIsNull(templateId, userId)"],
        ["Template day", "template_day -> workout_template -> user_id"],
        ["Template day exercise", "template_day_exercise -> template_day -> workout_template -> user_id"],
        ["Workout session", "findByIdAndUserId(sessionId, userId)"],
        ["Set", "workout_set -> session_exercise -> workout_session -> user_id"],
        ["Gym/equipment/routine", "query-uri cu id + user_id + deleted_at is null"],
    ], [5.2 * cm, 10.4 * cm], header_color=ORANGE))
    story.append(p(
        "Dacă o resursă există, dar aparține altui utilizator, aplicația răspunde ca și cum nu ar exista pentru caller. "
        "De aceea se folosesc excepții de tip NotFound și 404, nu 403, pentru multe resurse personale.",
        "Callout",
    ))

    story += section("12. De ce 404 în loc de 403 la resursele altui user")
    story.append(p(
        "Pentru resurse personale, 403 ar confirma existența resursei. 404 reduce scurgerea de informații. "
        "Aceasta este o măsură anti-enumerare.",
    ))
    story.append(make_table([
        ["Situație", "Răspuns", "Motiv"],
        ["Nu trimiți JWT pe endpoint protejat", "401 Unauthorized", "Nu ești autentificat"],
        ["Ești USER și intri pe /api/coach/**", "403 Forbidden", "Ești autentificat, dar nu ai rolul necesar"],
        ["Ceri template-ul altui utilizator", "404 Not Found", "Nu trebuie confirmată existența resursei"],
        ["Coach fără relație ACTIVE cere client", "404 Not Found", "Nu trebuie confirmat că userul există sau are date"],
    ], [5.2 * cm, 3.4 * cm, 7.0 * cm], header_color=RED))
    story.append(p(
        "Regula de prezentare: 401 este despre lipsa autentificării, 403 este despre lipsa unui rol/permisiuni generale, "
        "404 este folosit intenționat pentru resurse personale sau relații lipsă ca să nu dezvăluie existența lor.",
        "Callout",
    ))

    story += section("13. ReBAC: relația coach-client")
    story.append(p(
        "ReBAC înseamnă Relationship-Based Access Control. În coaching, rolul COACH nu este suficient. "
        "Coach-ul trebuie să aibă o relație ACTIVE cu clientul.",
    ))
    story.append(FlowDiagram([
        ("Endpoint coach", "/api/coach/clients\n/{clientId}/history", LIGHT_BLUE),
        ("RBAC", "SecurityConfig cere\nROLE_COACH", LIGHT_GREEN),
        ("ReBAC", "CoachAccess caută\nrelație ACTIVE", LIGHT_ORANGE),
        ("Read", "HistoryService cu\nclientUserId", LIGHT_RED),
    ]))
    story.append(code(
        "CoachAccess.requireActiveClient(coachUserId, clientUserId):\n"
        "findByCoachUserIdAndClientUserIdAndStatus(\n"
        "    coachUserId,\n"
        "    clientUserId,\n"
        "    RelationshipStatus.ACTIVE\n"
        ")"
    ))
    story.append(p("Unde este folosit:", "H2"))
    story.append(bullets([
        "CoachViewService.clientHistory - verifică relația, apoi apelează historyService.list(clientUserId);",
        "CoachViewService.clientAnalytics - verifică relația, apoi analyticsService.overview(clientUserId);",
        "CoachViewService.clientSession - verifică relația, apoi workoutSessionService.get(clientUserId, sessionId);",
        "CoachSearchService.searchClientWorkouts - verifică relația, apoi caută istoricul clientului.",
    ]))
    story.append(p(
        "Avantajul implementării: după poarta ReBAC, aplicația reutilizează serviciile owner-scoped existente. "
        "Coach-ul nu primește query-uri speciale nesigure și nu poate muta/modifica datele clientului prin acele endpoint-uri.",
        "Callout",
    ))

    story += section("14. Clientul controlează relația cu coach-ul")
    story.append(p(
        "Relația coach-client are lifecycle: PENDING, ACTIVE, REJECTED, REVOKED. Coach-ul poate invita, dar clientul "
        "trebuie să accepte. Clientul poate revoca accesul ulterior.",
    ))
    story.append(make_table([
        ["Acțiune", "Cine o face", "Verificare de securitate"],
        ["Invite client", "Coach", "Endpoint /api/coach/** cere ROLE_COACH"],
        ["Accept invite", "Client", "RelationshipService caută relația după id + clientUserId din JWT"],
        ["Reject invite", "Client", "La fel, doar clientul relației poate respinge"],
        ["Revoke coach", "Client", "Doar relația activă a clientului curent poate fi revocată"],
        ["Read client history", "Coach", "ROLE_COACH + relație ACTIVE"],
    ], [4.0 * cm, 3.5 * cm, 8.1 * cm], header_color=GREEN))
    story.append(p(
        "Asta este important pentru prezentare: userul client păstrează controlul asupra accesului la datele sale.",
        "Callout",
    ))

    story += section("15. CORS, CSRF și SPA")
    story.append(p(
        "Frontend-ul poate rula ca SPA servit de backend sau, în development, pe Vite la localhost:5173. "
        "CORS permite originea configurată și permite credentials pentru ca browserul să poată stoca/trimite cookie-ul HttpOnly.",
    ))
    story.append(bullets([
        "Allowed methods: GET, POST, PUT, DELETE, PATCH, OPTIONS.",
        "Allowed headers: Authorization, Content-Type, Accept.",
        "Allow credentials: true, necesar pentru refresh cookie în development.",
        "CSRF dezactivat deoarece operațiile normale folosesc Authorization header, nu cookie de sesiune.",
        "Refresh cookie are SameSite=Strict și Path=/api/auth, deci este restrâns la fluxul de autentificare.",
    ]))
    story.append(p(
        "Dacă te întreabă de CSRF: modelul nu folosește cookie ca autentificare pentru toate cererile API. "
        "Cookie-ul este doar pentru refresh/logout și este restricționat.",
        "Callout",
    ))

    story += section("16. Cum se vede securitatea în baza de date")
    story.append(make_table([
        ["Tabel", "Câmpuri relevante", "Rol de securitate"],
        ["app_users", "email, password_hash, role", "Identitate, roluri, parole hash-uite"],
        ["refresh_tokens", "user_id, token_hash, expires_at, revoked_at", "Refresh token server-side, revocabil și rotit"],
        ["coach_client_relationships", "coach_user_id, client_user_id, status", "Model ReBAC pentru coaching"],
        ["workout_templates", "user_id, visibility, deleted_at", "Ownership pentru programe private/publice"],
        ["workout_sessions", "user_id, status", "Istoricul este owner-scoped"],
    ], [4.4 * cm, 5.9 * cm, 5.3 * cm], header_color=PURPLE))
    story.append(p(
        "Baza de date contribuie și prin constrângeri: roluri valide, statusuri valide, relații coach-client fără self-coaching, "
        "indecși unici pe email și token_hash, plus chei străine care șterg/revocă datele dependente când userul dispare.",
        "Callout",
    ))

    story += section("17. Limitări reale și răspunsuri sincere")
    story.append(p(
        "Este bine să poți spune și ce nu acoperă sistemul. Asta arată că înțelegi securitatea, nu că vinzi ceva magic.",
    ))
    story.append(make_table([
        ["Întrebare posibilă", "Răspuns bun"],
        ["Dacă cineva fură access token-ul?", "Poate acționa până expiră, dar fereastra este scurtă. Refresh token-ul este protejat separat."],
        ["Dacă cineva fură refresh token-ul?", "Rotația și revoked_at reduc reutilizarea, iar token-ul nu este accesibil JavaScript prin HttpOnly."],
        ["De ce nu folosești sesiuni server-side?", "JWT stateless simplifică API-ul și scalarea, iar refresh token-ul rămâne revocabil server-side."],
        ["De ce uneori 404, nu 403?", "Pentru resurse personale, 404 evită confirmarea existenței resursei altui user."],
        ["Coach-ul poate modifica datele clientului?", "Nu prin endpoint-urile implementate de coach. Sunt read-only, iar accesul cere relație ACTIVE."],
        ["Ce nu ai implementat?", "Nu am rate limiting/2FA. Pentru producție acestea ar fi extensii naturale."],
    ], [5.1 * cm, 10.5 * cm], header_color=RED))

    story += section("18. Explicația scurtă de spus la prezentare")
    story.append(p(
        "Securitatea aplicației este construită pe mai multe niveluri. Autentificarea se face cu access token JWT "
        "de durată scurtă și refresh token opac. Refresh token-ul este pus într-un cookie HttpOnly, SameSite=Strict, "
        "limitat la /api/auth, iar în baza de date este păstrat doar ca SHA-256 fingerprint. La refresh, token-ul vechi "
        "este revocat și se emite o pereche nouă. Parolele sunt stocate cu BCrypt.",
    ))
    story.append(p(
        "După autentificare, backend-ul nu are încredere în userId-uri trimise de frontend. Utilizatorul curent este luat "
        "din JWT și fiecare resursă este încărcată cu verificări de ownership. Pentru resursele altui user, sistemul "
        "răspunde cu 404 pentru a evita IDOR și enumerarea de identificatori. Pentru roluri folosesc RBAC, iar pentru "
        "coaching folosesc ReBAC: un coach poate vedea datele unui client doar dacă există o relație ACTIVE între ei.",
        "Callout",
    ))

    story += section("19. Checklist de memorat")
    story.append(bullets([
        "JWT = access token scurt, trimis în Authorization header.",
        "Refresh token = opac, random, în cookie HttpOnly, rotit la refresh.",
        "SHA-256 pentru refresh token în DB, BCrypt pentru parole.",
        "Frontend-ul nu decide userId-ul. Backend-ul ia userul din JWT.",
        "IDOR se previne prin owner-scoped queries și access helpers.",
        "404 pentru resursele altui user, ca să nu le confirmi existența.",
        "RBAC = roluri pe zone mari: ADMIN, COACH, USER.",
        "ReBAC = coach-ul are nevoie de relație ACTIVE cu clientul.",
        "Endpoint-urile de coach sunt read-only pentru datele clientului.",
        "SecurityConfig este stateless și pune JwtAuthenticationFilter în lanțul Spring Security.",
    ]))
    story.append(Spacer(1, 10))
    story.append(p(
        "Fraza-cheie: autentificarea spune cine e userul, dar fiecare operație importantă verifică separat dreptul concret "
        "de acces la resursa cerută.",
        "Callout",
    ))
    return story


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    doc = SimpleDocTemplate(
        str(OUT),
        pagesize=A4,
        rightMargin=17 * mm,
        leftMargin=17 * mm,
        topMargin=19 * mm,
        bottomMargin=16 * mm,
        title="Securitatea aplicației workout",
        author="Codex",
    )
    doc.build(build_story(), onFirstPage=header_footer, onLaterPages=header_footer)
    print(OUT)


if __name__ == "__main__":
    main()
