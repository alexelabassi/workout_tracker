import fs from "node:fs/promises";
import { Presentation, PresentationFile } from "@oai/artifact-tool";

const OUT = "C:/Users/Alexandru/Desktop/Licenta/output/presentations/licenta_project_presentation.pptx";
const PREVIEW_DIR = "C:/Users/Alexandru/Desktop/Licenta/tmp/presentations/licenta_project_presentation/tmp/preview";

const W = 1280;
const H = 720;
const ink = "#111111";
const muted = "#555555";
const panel = "#F0F1F3";
const rule = "#B8BCC4";
const accent = "#FF6B35";
const blue = "#2F6F9F";
const green = "#3C7A57";
const red = "#B23A48";
const amber = "#9A6B19";

async function writeBlob(path, blob) {
  await fs.writeFile(path, new Uint8Array(await blob.arrayBuffer()));
}

function addText(slide, text, x, y, w, h, opts = {}) {
  const shape = slide.shapes.add({
    geometry: "textbox",
    position: { left: x, top: y, width: w, height: h },
    fill: "none",
    line: { style: "solid", fill: "none", width: 0 },
  });
  shape.text = text;
  shape.text.style = {
    fontSize: opts.size ?? 22,
    bold: opts.bold ?? false,
    color: opts.color ?? ink,
    alignment: opts.align ?? "left",
  };
  return shape;
}

function addTitle(slide, title, eyebrow = "Platformă web pentru antrenamente de forță") {
  addText(slide, eyebrow, 52, 32, 640, 28, { size: 16, bold: true, color: muted });
  addText(slide, title, 52, 74, 1080, 112, { size: 40, bold: true, color: ink });
  slide.shapes.add({
    geometry: "line",
    position: { left: 52, top: 194, width: 1176, height: 0 },
    fill: "none",
    line: { style: "solid", fill: rule, width: 1 },
  });
}

function addFooter(slide, n) {
  addText(slide, "Licență - prezentare proiect", 52, 676, 320, 24, { size: 13, color: muted });
  addText(slide, String(n).padStart(2, "0"), 1190, 676, 40, 24, { size: 13, color: muted, align: "right" });
}

function panelBox(slide, x, y, w, h, title, body, opts = {}) {
  const box = slide.shapes.add({
    geometry: "rect",
    position: { left: x, top: y, width: w, height: h },
    fill: opts.fill ?? panel,
    line: { style: "solid", fill: opts.line ?? "none", width: opts.line ? 1 : 0 },
  });
  addText(slide, title, x + 18, y + 16, w - 36, 34, {
    size: opts.titleSize ?? 23,
    bold: true,
    color: opts.titleColor ?? ink,
  });
  if (Array.isArray(body)) {
    addText(slide, body.map((b) => `• ${b}`).join("\n"), x + 20, y + 62, w - 40, h - 76, {
      size: opts.bodySize ?? 20,
      color: opts.bodyColor ?? ink,
    });
  } else if (body) {
    addText(slide, body, x + 20, y + 62, w - 40, h - 76, {
      size: opts.bodySize ?? 20,
      color: opts.bodyColor ?? ink,
    });
  }
  return box;
}

function metric(slide, x, y, w, h, value, label, color = ink) {
  const box = slide.shapes.add({
    geometry: "rect",
    position: { left: x, top: y, width: w, height: h },
    fill: panel,
    line: { style: "solid", fill: "none", width: 0 },
  });
  addText(slide, value, x + 18, y + 20, w - 36, 60, { size: 46, bold: true, color });
  addText(slide, label, x + 20, y + 88, w - 40, h - 102, { size: 18, color: muted });
  return box;
}

function node(slide, x, y, w, h, title, body, color = ink) {
  const b = slide.shapes.add({
    geometry: "rect",
    position: { left: x, top: y, width: w, height: h },
    fill: "#FFFFFF",
    line: { style: "solid", fill: color, width: 2 },
  });
  addText(slide, title, x + 14, y + 12, w - 28, 28, { size: 18, bold: true, color });
  addText(slide, body, x + 14, y + 48, w - 28, h - 56, { size: 15, color: ink });
  return b;
}

function connect(slide, a, b, fromSide = "right", toSide = "left", color = "#777777") {
  const connector = slide.shapes.connect(a, b, {
    kind: "elbow",
    fromSide,
    toSide,
    line: { style: "solid", fill: color, width: 2 },
    tail: { type: "arrow", width: "sm", length: "sm" },
  });
  connector.sendToBack();
  return connector;
}

function notes(slide, lines) {
  slide.speakerNotes.textFrame.setText(lines);
  slide.speakerNotes.setVisible(true);
}

function slide1(p) {
  const s = p.slides.add();
  s.background.fill = "#FFFFFF";
  addText(s, "Platformă web sigură pentru planificarea, monitorizarea și analiza antrenamentelor de forță", 70, 120, 1000, 190, { size: 54, bold: true });
  addText(s, "Aplicație full-stack cu istoric stabil, securitate multi-user și căutare scalabilă", 74, 335, 820, 40, { size: 24, color: muted });
  panelBox(s, 74, 470, 340, 92, "Ideea centrală", "Datele de planificare se pot schimba, dar istoricul antrenamentelor trebuie să rămână corect.", { fill: panel, bodySize: 20 });
  panelBox(s, 455, 470, 340, 92, "Contribuția", "Am integrat planificare, sesiuni live, marketplace, coaching, analizator, securitate și OpenSearch într-un sistem coerent.", { fill: panel, bodySize: 18 });
  addFooter(s, 1);
  notes(s, [
    "Deschid cu ideea că nu prezint un CRUD, ci o platformă completă.",
    "Accentul este pe corectitudine istorică, securitate și scalare.",
    "Nu intru încă în detalii. Doar setez cadrul."
  ]);
}

function slide2(p) {
  const s = p.slides.add();
  addTitle(s, "Problema reală este că planul se schimbă, dar istoricul nu trebuie să se rescrie");
  panelBox(s, 70, 220, 330, 260, "Planificarea este editabilă", [
    "exerciții, rutine, săli, echipamente",
    "template-uri pe zile",
    "publicare în marketplace",
    "modificări și ștergere logică"
  ]);
  panelBox(s, 475, 220, 330, 260, "Istoricul trebuie să fie stabil", [
    "sesiuni pornite dintr-un template",
    "seturi executate de utilizator",
    "nume și valori păstrate ca la momentul execuției",
    "analytics și search peste date istorice"
  ]);
  panelBox(s, 880, 220, 300, 260, "Mai există o problemă", [
    "aplicația este multi-user",
    "datele personale nu pot fi accesate prin ID-uri ghicite",
    "coach-ul vede doar clienții cu relație activă"
  ]);
  addText(s, "De aici apar cele trei decizii importante: snapshot-uri, autorizare la nivel de resursă și model de citire OpenSearch.", 92, 552, 1050, 54, { size: 26, bold: true });
  addFooter(s, 2);
  notes(s, [
    "Aici explic tensiunea principală: planul e editabil, istoricul trebuie să rămână factual.",
    "Leg imediat problema de securitate: dacă sunt mai mulți utilizatori, autentificarea nu ajunge.",
    "Pregătesc tranziția spre fluxul aplicației."
  ]);
}

function slide3(p) {
  const s = p.slides.add();
  addTitle(s, "Fluxul principal transformă planificarea într-un istoric analizabil");
  const a = node(s, 62, 268, 150, 104, "Resurse", "exerciții\nrutine\nsăli\nechipamente", blue);
  const b = node(s, 255, 268, 150, 104, "Template", "program pe zile\nexerciții planificate\nrutine START/END", green);
  const c = node(s, 448, 268, 150, 104, "Sesiune live", "start workout\nsnapshot-uri\nextra exercises", amber);
  const d = node(s, 641, 268, 150, 104, "Seturi", "greutate\nrepetări\nRPE\nechipament", amber);
  const e = node(s, 834, 268, 150, 104, "Istoric", "sesiuni\nsummary\nnotes", green);
  const f = node(s, 1027, 268, 150, 104, "Analiză", "analytics\nsearch\ncoach view", red);
  connect(s, a, b, "right", "left");
  connect(s, b, c, "right", "left");
  connect(s, c, d, "right", "left");
  connect(s, d, e, "right", "left");
  connect(s, e, f, "right", "left");
  panelBox(s, 105, 465, 460, 96, "Ce vede utilizatorul", "Își construiește programul, pornește un antrenament, loghează seturi și apoi vede istoricul, graficele și căutarea.", { fill: "#F7F7F7", bodySize: 20 });
  panelBox(s, 665, 465, 460, 96, "Ce face sistemul", "Separă planul editabil de execuția istorică și aplică verificări de acces la fiecare pas.", { fill: "#F7F7F7", bodySize: 20 });
  addFooter(s, 3);
  notes(s, [
    "Acesta este slide-ul pe care îl folosesc ca hartă mentală pentru toată aplicația.",
    "Îl explic ca utilizator: creez resurse, template, sesiune, seturi, apoi istoric.",
    "Apoi îl explic tehnic: sistemul separă datele editabile de cele istorice."
  ]);
}

function slide4(p) {
  const s = p.slides.add();
  addTitle(s, "Arhitectura păstrează PostgreSQL ca sursă de adevăr și OpenSearch ca proiecție");
  const fe = node(s, 74, 230, 190, 110, "React + TypeScript", "Vite SPA\nfeature modules\ntheme + auth provider", blue);
  const api = node(s, 332, 230, 190, 110, "Spring Boot API", "controllers\nservices\nrepositories\nDTO-uri", green);
  const pg = node(s, 590, 188, 210, 112, "PostgreSQL 16", "Flyway migrations\nFK, CHECK, indexes\nsource of truth", amber);
  const os = node(s, 590, 355, 210, 112, "OpenSearch 2.13", "full-text\nfuzzy\nfacets\nhighlights", red);
  const tests = node(s, 868, 270, 220, 110, "Testcontainers", "PostgreSQL real\nOpenSearch real\nfără H2", ink);
  connect(s, fe, api, "right", "left", blue);
  connect(s, api, pg, "right", "left", amber);
  connect(s, api, os, "right", "left", red);
  connect(s, tests, api, "left", "right", muted);
  panelBox(s, 91, 510, 1015, 72, "Ideea de arhitectură", "Backend-ul scrie și validează datele în PostgreSQL. OpenSearch este o copie derivată pentru căutare, care poate fi reconstruită din PostgreSQL.", { fill: "#F7F7F7", bodySize: 22 });
  addFooter(s, 4);
  notes(s, [
    "Spun clar: PostgreSQL este autoritativ. OpenSearch nu înlocuiește baza de date.",
    "Menționez separarea pe module din backend și frontend.",
    "Adaug că testele folosesc containere reale, nu H2."
  ]);
}

function slide5(p) {
  const s = p.slides.add();
  addTitle(s, "Snapshot-urile sunt copii istorice ale câmpurilor care nu trebuie să se schimbe");
  const plan = node(s, 80, 220, 260, 140, "Tabele de planificare", "workout_templates\ntemplate_days\ntemplate_day_exercises\nroutines, gyms, equipment", blue);
  const start = node(s, 432, 220, 210, 140, "POST /workouts/start", "verifică owner\nverifică sala\nziua nu e goală\nun singur IN_PROGRESS", ink);
  const hist = node(s, 735, 220, 310, 140, "Tabele istorice", "workout_sessions\nsession_exercises\nsession_routines\nworkout_sets", amber);
  connect(s, plan, start, "right", "left", muted);
  connect(s, start, hist, "right", "left", amber);
  panelBox(s, 93, 425, 330, 112, "Exemplu", "exercises.name = Bench Press\n\nse copiază în\n\nsession_exercises.exercise_name_snapshot = Bench Press", { fill: panel, bodySize: 20 });
  panelBox(s, 474, 425, 330, 112, "De ce nu e doar FK", "original_exercise_id poate indica exercițiul curent, dar snapshot-ul păstrează numele de la momentul sesiunii.", { fill: "#F7F7F7", bodySize: 20 });
  panelBox(s, 855, 425, 330, 112, "Regula de date", "workout_sets -> session_exercises -> workout_sessions\n\nnu\n\nworkout_sets -> template_day_exercises", { fill: "#FFF4E6", line: amber, bodySize: 19 });
  addFooter(s, 5);
  notes(s, [
    "Aici explic foarte simplu că snapshot în proiect înseamnă câmp copiat, nu backup.",
    "Dau exemplul cu Bench Press redenumit după antrenament.",
    "Insist pe lanțul corect workout_sets -> session_exercises -> workout_sessions."
  ]);
}

function slide6(p) {
  const s = p.slides.add();
  addTitle(s, "Securitatea nu se oprește la login: fiecare resursă este verificată");
  const a = node(s, 80, 220, 200, 130, "Autentificare", "JWT access token\nrefresh token opac\nparole BCrypt", purple());
  const b = node(s, 350, 220, 210, 130, "Refresh sigur", "cookie HttpOnly\nSameSite=Strict\nhash SHA-256 în DB\nrotație la refresh", purple());
  const c = node(s, 630, 220, 200, 130, "RBAC", "/api/admin/** -> ADMIN\n/api/coach/** -> COACH\nrestul /api/** autenticat", green);
  const d = node(s, 900, 220, 210, 130, "Ownership/ReBAC", "userId din JWT\n404 pentru resursa altuia\ncoach doar cu relație ACTIVE", red);
  connect(s, a, b, "right", "left", purple());
  connect(s, b, c, "right", "left", muted);
  connect(s, c, d, "right", "left", red);
  panelBox(s, 118, 425, 440, 110, "Anti-IDOR", "Frontend-ul nu decide userId-ul resursei. Backend-ul îl ia din JWT și încarcă resursele prin query-uri owner-scoped sau verificări pe lanțul de proprietate.", { fill: "#F7F7F7", bodySize: 21 });
  panelBox(s, 690, 425, 440, 110, "RBAC vs ReBAC", "Rolul COACH deschide zona de coaching, dar accesul real la datele clientului cere relație coach-client ACTIVE.", { fill: "#F7F7F7", bodySize: 21 });
  addFooter(s, 6);
  notes(s, [
    "Spun diferența dintre autentificare și autorizare.",
    "Explic de ce refresh token-ul e cookie HttpOnly și de ce în DB e doar hash-ul.",
    "Pentru IDOR: un utilizator nu poate schimba un ID în URL ca să vadă datele altuia."
  ]);
}

function purple() {
  return "#74539B";
}

function slide7(p) {
  const s = p.slides.add();
  addTitle(s, "Modulele avansate folosesc aceleași reguli de corectitudine");
  panelBox(s, 70, 220, 330, 220, "Marketplace", [
    "template public doar dacă nu este gol",
    "vote/save/use cu contoare în template_stats",
    "use creează o copie privată independentă",
    "referințele custom ale altui user nu se păstrează"
  ], { fill: "#F7F7F7", titleColor: green, bodySize: 20 });
  panelBox(s, 475, 220, 330, 220, "Analizator", [
    "determinist, rule-based",
    "scor 0-100 și categorii explicabile",
    "volume, frequency, balance, session design, rest",
    "nu este AI și nu oferă sfat medical"
  ], { fill: "#F7F7F7", titleColor: red, bodySize: 20 });
  panelBox(s, 880, 220, 300, 220, "Coaching", [
    "invitații coach-client",
    "relație PENDING/ACTIVE/REVOKED/REJECTED",
    "coach vede doar clienții activi",
    "history, analytics, sessions și search scoped"
  ], { fill: "#F7F7F7", titleColor: purple(), bodySize: 20 });
  addText(s, "Aceste module nu sunt insule separate: toate respectă owner checks, date istorice stabile și sursa de adevăr PostgreSQL.", 110, 515, 1010, 60, { size: 27, bold: true });
  addFooter(s, 7);
  notes(s, [
    "Aici arăt că proiectul este mai mult decât OpenSearch.",
    "Marketplace-ul demonstrează deep copy și independența copiei.",
    "Analizatorul e explicabil, iar coaching-ul demonstrează ReBAC."
  ]);
}

function slide8(p) {
  const s = p.slides.add();
  addTitle(s, "OpenSearch este justificat prin căutare fuzzy și scalare măsurată");
  const pg = node(s, 82, 220, 230, 130, "PostgreSQL", "source of truth\ntranzacții și integritate\nSQL browse/list fallback", amber);
  const ev = node(s, 380, 220, 230, 130, "AFTER_COMMIT events", "indexarea nu vede rollback-uri\nfailure log + rebuild", ink);
  const osn = node(s, 678, 220, 230, 130, "OpenSearch", "templates alias\nworkout_sessions alias\nfuzzy, facets, highlight", red);
  const rv = node(s, 976, 220, 180, 130, "Revalidare", "IDs verificate din nou în PostgreSQL", green);
  connect(s, pg, ev, "right", "left", muted);
  connect(s, ev, osn, "right", "left", red);
  connect(s, osn, rv, "right", "left", green);
  metric(s, 96, 405, 235, 150, "15.8s", "PostgreSQL fuzzy p50 la 2M documente", red);
  metric(s, 378, 405, 235, 150, "3.9ms", "OpenSearch fuzzy p50 la 2M documente", green);
  metric(s, 660, 405, 235, 150, "2x", "OpenSearch a construit indexul mai repede la 2M", ink);
  metric(s, 942, 405, 235, 150, "3.6x", "index OpenSearch mai mic pe disc la 2M", ink);
  addFooter(s, 8);
  notes(s, [
    "Aici spun că OpenSearch nu a fost introdus ca ornament.",
    "Benchmark-ul compară cu PostgreSQL bine configurat, nu cu ILIKE.",
    "Cea mai puternică justificare este fuzzy search: benhc -> bench la latență interactivă."
  ]);
}

function slide9(p) {
  const s = p.slides.add();
  addTitle(s, "Corectitudinea este împinsă în baza de date și verificată cu teste reale");
  metric(s, 82, 230, 250, 150, "28", "tabele în schema Flyway V1", amber);
  metric(s, 372, 230, 250, 150, "128", "teste în backend/src/test", green);
  metric(s, 662, 230, 250, 150, "2M", "documente în benchmark-ul de căutare", red);
  metric(s, 952, 230, 250, 150, "1", "workout activ permis per utilizator", blue);
  panelBox(s, 100, 450, 480, 94, "Constrângeri care contează", "FK-uri, CHECK-uri, indecși unici parțiali, soft delete și UNIQUE pentru ordinea seturilor/exercițiilor.", { fill: "#F7F7F7", bodySize: 21 });
  panelBox(s, 700, 450, 480, 94, "Testare pe infrastructură reală", "Testcontainers pornește PostgreSQL 16, iar suita de search pornește OpenSearch real. Nu se folosește H2.", { fill: "#F7F7F7", bodySize: 21 });
  addFooter(s, 9);
  notes(s, [
    "Subliniez că nu am lăsat integritatea doar în cod.",
    "Exemplu bun: unique partial index pentru un singur workout IN_PROGRESS per user.",
    "Testele pornesc servicii reale, deci acoperă comportament apropiat de producție."
  ]);
}

function slide10(p) {
  const s = p.slides.add();
  addTitle(s, "Contribuția este integrarea coerentă a unui sistem complet, sigur și extensibil");
  panelBox(s, 78, 220, 330, 220, "Ce am construit", [
    "aplicație full-stack funcțională",
    "planificare, sesiuni, istoric, analytics",
    "marketplace, coaching, analizator, search"
  ], { fill: "#F7F7F7", bodySize: 21 });
  panelBox(s, 475, 220, 330, 220, "Ce o face robustă", [
    "snapshot-uri pentru istoric stabil",
    "JWT, refresh rotation, BCrypt",
    "ownership, RBAC și ReBAC",
    "PostgreSQL ca sursă de adevăr"
  ], { fill: "#F7F7F7", bodySize: 21 });
  panelBox(s, 872, 220, 330, 220, "Ce demonstrează tehnic", [
    "OpenSearch ca read model derivat",
    "benchmark empiric cu 2M documente",
    "teste cu infrastructură reală",
    "arhitectură modulară"
  ], { fill: "#F7F7F7", bodySize: 21 });
  addText(s, "Pe scurt: proiectul rezolvă o problemă de produs reală prin decizii tehnice care păstrează datele corecte, private și căutabile la scară.", 110, 520, 1010, 70, { size: 31, bold: true });
  addFooter(s, 10);
  notes(s, [
    "Închei revenind la ideea de început: planificare editabilă, istoric stabil.",
    "Apoi comprim contribuția: sistem complet, securitate, snapshot-uri, OpenSearch, teste.",
    "Dacă sunt întrebat ce e cel mai important, răspund integrarea acestor piese într-o platformă coerentă."
  ]);
}

async function main() {
  await fs.mkdir(PREVIEW_DIR, { recursive: true });
  const p = Presentation.create({ slideSize: { width: W, height: H } });
  [slide1, slide2, slide3, slide4, slide5, slide6, slide7, slide8, slide9, slide10].forEach((fn) => fn(p));

  for (const [index, slide] of p.slides.items.entries()) {
    const stem = `slide-${String(index + 1).padStart(2, "0")}`;
    await writeBlob(`${PREVIEW_DIR}/${stem}.png`, await p.export({ slide, format: "png", scale: 1.6 }));
    const layout = await slide.export({ format: "layout" });
    await fs.writeFile(`${PREVIEW_DIR}/${stem}.layout.json`, await layout.text(), "utf8");
  }
  await writeBlob(`${PREVIEW_DIR}/montage.webp`, await p.export({ format: "webp", montage: true, scale: 1 }));

  const snapshot = await p.inspect({ kind: "slide,textbox,shape,notes", maxChars: 12000 });
  await fs.writeFile(`${PREVIEW_DIR}/inspect.ndjson`, snapshot.ndjson, "utf8");

  const pptx = await PresentationFile.exportPptx(p);
  await pptx.save(OUT);
  console.log(OUT);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
