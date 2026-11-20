#!/usr/bin/env python3
"""
Generiert MODULES.md automatisch aus dem Quellcode.

Quellen:
  - src/main/java/de/erleichterungen/gui/Modules.java   (Moduldeklarationen + Settings)
  - src/main/java/de/erleichterungen/config/ModConfig.java (Standardwerte der Felder)
  - src/main/java/de/erleichterungen/gui/Module.java    (Kategorie-Titel)

Aufruf:  python3 tools/generate_modules_md.py
"""
import re
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "src/main/java/de/erleichterungen"
MODULES = (SRC / "gui/Modules.java").read_text(encoding="utf-8")
CONFIG = (SRC / "config/ModConfig.java").read_text(encoding="utf-8")
MODULEJAVA = (SRC / "gui/Module.java").read_text(encoding="utf-8")

MODE_LABEL = {"BOX": "Box", "BOTH": "Box & Tracer", "TRACER": "Nur Tracer"}


# ---------------------------------------------------------------- Hilfsparser
def find_group(s, open_idx):
    """s[open_idx] == '('  ->  Index der passenden schließenden ')'."""
    depth = 0
    in_str = in_char = esc = False
    i = open_idx
    while i < len(s):
        ch = s[i]
        if esc:
            esc = False
        elif ch == "\\":
            esc = True
        elif in_str:
            if ch == '"':
                in_str = False
        elif in_char:
            if ch == "'":
                in_char = False
        elif ch == '"':
            in_str = True
        elif ch == "'":
            in_char = True
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1
    return -1


def split_args(s):
    """Teilt eine Argumentliste an Top-Level-Kommas (respektiert Strings/Klammern)."""
    args = []
    depth = 0
    in_str = in_char = esc = False
    cur = ""
    for ch in s:
        if esc:
            cur += ch
            esc = False
            continue
        if ch == "\\":
            cur += ch
            esc = True
            continue
        if in_str:
            cur += ch
            if ch == '"':
                in_str = False
            continue
        if in_char:
            cur += ch
            if ch == "'":
                in_char = False
            continue
        if ch == '"':
            in_str = True
            cur += ch
            continue
        if ch == "'":
            in_char = True
            cur += ch
            continue
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            args.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        args.append(cur.strip())
    return args


def strip_str(x):
    if x is None:
        return ""
    x = x.strip()
    if len(x) >= 2 and x[0] == '"' and x[-1] == '"':
        return x[1:-1]
    return x


def fmt_num(x):
    if x is None:
        return "—"
    x = x.strip()
    if x.endswith("f") or x.endswith("F"):
        x = x[:-1]
    return x


def fmt_color(x):
    if x is None:
        return "—"
    return "#" + strip_str(x)


def find_field(argstr):
    m = re.search(r"c\(\)\.(\w+)", argstr)
    return m.group(1) if m else None


# ---------------------------------------------------------------- ModConfig-Defaults
defaults = {}
for m in re.finditer(r"public\s+(?:final\s+)?[\w<>\[\],.?\s]+?\s+(\w+)\s*=\s*(.+?);", CONFIG):
    defaults[m.group(1)] = m.group(2).strip()
for m in re.finditer(r"public\s+Map<[^;=]+>\s+(\w+)\s*;", CONFIG):
    defaults.setdefault(m.group(1), None)


# ---------------------------------------------------------------- Kategorie-Titel
cat_titles = {}
enum_start = MODULEJAVA.index("enum Category")
enum_body = MODULEJAVA[enum_start:MODULEJAVA.index(";", enum_start)]
for m in re.finditer(r"(\w+)\(\"([^\"]+)\"\)", enum_body):
    cat_titles[m.group(1)] = m.group(2)
cat_order = list(cat_titles.keys())


# ---------------------------------------------------------------- Settings
def build_setting(typ, name, desc, args, field):
    rawdef = defaults.get(field) if field else None
    d = {"type": typ, "name": name, "desc": desc, "field": field or "—"}
    if typ == "Bool":
        d["default"] = "An" if rawdef == "true" else "Aus"
        d["range"] = "An / Aus"
    elif typ == "Int":
        d["default"] = fmt_num(rawdef)
        d["range"] = f"{args[2].strip()} … {args[3].strip()}"
    elif typ == "Dec":
        d["default"] = fmt_num(rawdef)
        d["range"] = f"{fmt_num(args[2])} … {fmt_num(args[3])} (Schritt {fmt_num(args[4])})"
    elif typ == "Color":
        d["default"] = fmt_color(rawdef)
        d["range"] = "Hex-Farbe (RRGGBB)"
    elif typ == "Text":
        d["default"] = strip_str(rawdef) if rawdef else "\"\""
        d["range"] = "Text"
    elif typ == "Mode":
        lab = rawdef.split(".")[-1] if rawdef else ""
        d["default"] = MODE_LABEL.get(lab, lab)
        d["range"] = "Box / Box & Tracer / Nur Tracer"
    elif typ == "ListEdit":
        d["default"] = "Voreinstellung (Liste)"
        d["range"] = "Liste von IDs + Farbe + Modus"
    elif typ == "Action":
        d["default"] = "—"
        d["range"] = "Aktion (Knopf)"
    else:
        d["default"] = "—"
        d["range"] = "—"
    return d


def parse_setting(inner):
    inner = inner.strip()
    mm = re.match(r"new\s+Setting\.(\w+)\s*\(", inner)
    if mm:
        typ = mm.group(1)
        op = inner.index("(", mm.end() - 1)
        cl = find_group(inner, op)
        body = inner[op + 1:cl]
        args = split_args(body)
        name = strip_str(args[0]) if args else "?"
        desc = strip_str(args[1]) if len(args) > 1 else ""
        return build_setting(typ, name, desc, args, find_field(body))
    if inner.startswith("bulkMode"):
        op = inner.index("(")
        cl = find_group(inner, op)
        args = split_args(inner[op + 1:cl])
        return build_setting("Action", strip_str(args[0]), "Sammelaktion für alle Listeneinträge.", args, None)
    return None


# ---------------------------------------------------------------- Module
def parse_module(stmt):
    s = stmt.strip()
    ctor = "toggleable" if s.startswith("toggleable") else ("new Module" if s.startswith("new Module") else None)
    if ctor is None:
        return None
    op = s.index("(")
    cl = find_group(s, op)
    head = split_args(s[op + 1:cl])
    mid = strip_str(head[0])
    name = strip_str(head[1])
    cat = head[2].split(".")[-1]
    desc = strip_str(head[3]) if len(head) > 3 else ""
    if ctor == "toggleable":
        toggle = True
    else:
        toggle = len(head) > 4 and head[4].strip() != "null"
    rest = s[cl + 1:]
    settings = []
    j = 0
    while True:
        a = rest.find(".add(", j)
        if a < 0:
            break
        aop = a + len(".add")
        acl = find_group(rest, aop)
        st = parse_setting(rest[aop + 1:acl])
        if st:
            settings.append(st)
        j = acl + 1
    return {"id": mid, "name": name, "cat": cat, "desc": desc, "toggle": toggle, "settings": settings}


body = MODULES[MODULES.index("List<Module> list = new ArrayList<>();"):]
modules = []
idx = 0
while True:
    k = body.find("list.add(", idx)
    if k < 0:
        break
    op = k + len("list.add")
    cl = find_group(body, op)
    mod = parse_module(body[op + 1:cl])
    if mod:
        modules.append(mod)
    idx = cl + 1

registered = len(re.findall(r"list\.add\(", body))


# ---------------------------------------------------------------- Markdown
out = []
out.append("# Module\n")
out.append("> **Automatisch generiert** aus dem Quellcode mit `tools/generate_modules_md.py`.")
out.append("> Nicht von Hand bearbeiten – stattdessen das Skript erneut ausführen.\n")
out.append(f"**Module gesamt:** {len(modules)}  ")
out.append("**Setting-Typen:** Bool (An/Aus), Int (Ganzzahl), Dec (Dezimal), Color (Hex), "
           "Text, Mode (Darstellung), ListEdit (ID-Liste mit Farbe), Action (Knopf).\n")
out.append("Dieser Client gruppiert Einstellungen **flach pro Modul** (keine Unter-Gruppen wie bei Meteor); "
           "die Spalte „Feld\" nennt das zugehörige Config-Feld in `ModConfig`.\n")

counts = {}
for cat in cat_order:
    mods = [m for m in modules if m["cat"] == cat]
    if not mods:
        continue
    counts[cat] = len(mods)
    out.append(f"\n## {cat_titles.get(cat, cat)}  \n")
    for m in mods:
        toggle = "An/Aus-Modul" if m["toggle"] else "Aktions-/Einstellungs-Modul"
        out.append(f"### {m['name']}  ")
        out.append(f"`id: {m['id']}` · Kategorie: **{cat_titles.get(cat, cat)}** · {toggle}  ")
        out.append(f"{m['desc']}\n")
        if m["settings"]:
            out.append("| Einstellung | Typ | Standard | Bereich / Werte | Feld |")
            out.append("|---|---|---|---|---|")
            for s in m["settings"]:
                nm = s["name"].replace("|", "\\|")
                rg = s["range"].replace("|", "\\|")
                df = str(s["default"]).replace("|", "\\|")
                out.append(f"| {nm} | {s['type']} | {df} | {rg} | `{s['field']}` |")
            out.append("")
        else:
            out.append("_Keine Einstellungen._\n")

# Übersicht pro Kategorie
summary = "  ".join(f"{cat_titles.get(c, c)}: {counts[c]}" for c in cat_order if c in counts)
out.insert(5, f"**Pro Kategorie:** {summary}\n")

(ROOT / "MODULES.md").write_text("\n".join(out) + "\n", encoding="utf-8")

# ---------------------------------------------------------------- Verifikation (Schritt 5)
print(f"Geparste Module:        {len(modules)}")
print(f"Registriert (list.add): {registered}")
print("Pro Kategorie:", {cat_titles.get(c, c): counts[c] for c in counts})
if len(modules) != registered:
    print("FEHLER: Anzahl stimmt nicht überein!", file=sys.stderr)
    sys.exit(1)
print("OK: MODULES.md geschrieben, Modulanzahl stimmt überein.")
