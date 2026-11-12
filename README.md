# Erleichterungen – Fabric-Client-Mod für Minecraft 26.3

Client-seitige Fabric-Mod mit sechs Modulen und einer Click-GUI (**Rechte Shift-Taste**):

![Click-GUI](docs/gui.png)


| Modul | Taste | Beschreibung |
|---|---|---|
| **Block-ESP** | `F6` | Markiert ausgewählte Blöcke (Standard: Diamant-/Smaragderz, Antiker Schrott, Spawner) farbig durch Wände. |
| **Entity-ESP** | `F4` | Markiert Base-typische Entities durch Wände: Item Frames (mit Gegenstand als Text), leuchtende Item Frames, Rüstungsständer, Gemälde, Truhen-/Trichter-Loren, Truhen-Boote, Villager, Leinenknoten, Endkristalle. Typen frei erweiterbar. |
| **Player-ESP** | `F7` | Boxen + Name + Entfernung um alle Spieler, durch Wände sichtbar. Vanish-Verdächtige in Magenta. |
| **Geoden-Tracker** | `F8` | Findet knospenden Amethyst und alle Knospen, färbt sie nach Wachstumsstufe, erkennt Wachstum live und meldet ausgewachsene Cluster. |
| **Anti-Vanish** | `F9` | Erkennt Spieler, die per Vanish unsichtbar sind (standardmäßig an). |
| **Freecam** | `G` | Kamera frei bewegen, der Spieler bleibt stehen. WASD, Leertaste/Shift = hoch/runter, Sprint = schneller. |
| HUD | `F10` | Info-Overlay oben links. |
| **Einstellungs-GUI** | `Rechte Shift` | Alle Module und Einstellungen übersichtlich an einem Ort. |

Alle Tasten lassen sich unter *Optionen → Steuerung → Erleichterungen* ändern.

## Installation

**Direkter Download (ohne GitHub-Login):**
https://github.com/paulpuder30-cloud/Minecraft-erleichterungen-/raw/claude/epic-goldberg-fi63ne/download/erleichterungen-1.3.0.jar

1. [Fabric Loader](https://fabricmc.net/use/) **0.20.1+** für Minecraft **26.3** installieren.
2. [Fabric API](https://modrinth.com/mod/fabric-api) für 26.3 in den `mods`-Ordner legen.
3. `erleichterungen-1.3.0.jar` in den `mods`-Ordner legen.

Die fertige JAR gibt es unter **Actions → Build → Artifacts** (`erleichterungen-mod`)
oder selbst bauen:

```bash
./gradlew build      # benötigt Java 25
# Ergebnis: build/libs/erleichterungen-1.3.0.jar
```

## Click-GUI (Rechte Shift-Taste)

Ein Fenster pro Kategorie (Render, Welt, Spieler, Sonstiges, Client):

- **Linksklick** auf ein Modul: an/aus
- **Rechtsklick** auf ein Modul: Einstellungen aufklappen (Regler, Schalter, Farben, Texte)
- **Mittelklick** auf ein Modul (oder Klick auf „Taste“): Taste neu belegen – Entf = keine Taste
- Fenster am **Titel ziehen** zum Verschieben, Rechtsklick auf den Titel klappt es ein
- **Mausrad** scrollt ein Fenster bzw. stellt einen Regler fein ein
- **Suche** unten links: einfach lostippen (Esc leert die Suche) – filtert Module und Einstellungen
- Block- und Entity-Listen werden in einem eigenen Fenster bearbeitet: Suche nach deutschem Namen oder ID,
  pro Eintrag Modus sowie Box- und Tracer-Farbe
- Akzentfarbe unter *Client → GUI* änderbar; Positionen werden gespeichert

| Einstellungen | Listen-Editor |
|---|---|
| ![Einstellungen](docs/gui-einstellungen.png) | ![Listen-Editor](docs/liste-bearbeiten.png) |

| ESP + Tracer | Freecam |
|---|---|
| ![ESP](docs/esp.png) | ![Freecam](docs/freecam.png) |

## Module im Detail

### Box, Box & Tracer oder nur Tracer
Jedes Ziel hat einen **Modus**: **Box** (Umriss durch Wände), **Box & Tracer** oder **Nur Tracer**
(Linie vom Fadenkreuz zum Ziel) – jeweils mit eigener **Box-Farbe** und **Tracer-Farbe**.

| Wo | Einstellbar | GUI |
|---|---|---|
| Block-ESP | **pro Block** (z. B. Diamanterz: Box & Tracer, Spawner: nur Box) | *Render → Block-ESP → Blöcke* |
| Entity-ESP | **pro Entity-Typ** (z. B. Item Frame: nur Tracer) | *Render → Entity-ESP → Entities* |
| Player-ESP | Modus, Box-Farbe, Tracer-Farbe | *Render → Player-ESP* |
| Geoden-Tracker | Modus, Tracer-Farbe (Tracer führen zu fertigen Clustern) | *Welt → Geoden-Tracker* |
| Anti-Vanish | Modus, Box-Farbe, Tracer-Farbe für Verdächtige | *Sonstiges → Anti-Vanish* |

Im Listen-Editor einen Eintrag anklicken → unten Modus-Knöpfe, „Box-Farbe“/„Tracer-Farbe“ wählen und
mit den R/G/B-Reglern einstellen. Das Kästchen **B / B+T / T** in der Liste wechselt den Modus direkt.
In Block-/Entity-ESP setzen **„Alle: Box / Box & Tracer / Nur Tracer“** alle Einträge auf einmal.

**Tracer wieder ausschalten:** Modus auf „Box“ stellen – oder *Render → Tracer → „Alle Tracer aus“*
(bzw. `/erl tracers all false`), das entfernt sämtliche Tracer in allen Modulen.

### Anti-Vanish
Kombiniert vier Erkennungsmethoden (einzeln abschaltbar):

- **Leave-Nachricht** – Ein Spieler verschwindet aus der Tabliste, aber es kommt keine
  „hat das Spiel verlassen“-Nachricht → vermutlich Vanish. (Auf Servern ohne Leave-Nachrichten
  mit `/erl antivanish leave` abschalten.)
- **Tab-Vervollständigung** – Fragt regelmäßig unsichtbar die Vorschläge für `/msg ` ab.
  Namen, die dort auftauchen, aber nicht in der Tabliste stehen, sind versteckt online.
  Befehl änderbar mit `/erl antivanish command <befehl>`.
- **Entities** – Spieler-Entities in Sichtweite, die nicht in der Tabliste stehen
  (NPCs mit Fake-UUID werden ignoriert).
- **Zuschauermodus** – Meldet, wenn jemand in den Spectator-Modus wechselt.

### Entity-ESP
- Item Frames zeigen den enthaltenen Gegenstand als Beschriftung (z. B. „Elytra [23m]“),
  benannte Entities ihren Namen.
- Das HUD listet, wie viele von welchem Typ in der Nähe sind – praktisch, um Basen zu erkennen.
- Weitere Typen: `/erl entityesp add minecraft:allay`, eigene Farbe: `/erl entityesp color FF0000 minecraft:armor_stand`.

### Geoden-Tracker
- Lila: knospender Amethyst · Rot: kleine Knospe · Orange: mittlere · Gelb: große · **Grün (gefüllt): erntebereiter Cluster**
- HUD zeigt Anzahl knospender Blöcke, freie Wachstumsseiten, fertige Cluster, wachsende Knospen,
  Wachstums-Ereignisse der letzten 10 Minuten und wann zuletzt etwas gewachsen ist.
- Chat-Meldung + Sound, wenn ein Cluster ausgewachsen ist.
- Nur Knospen, die wirklich an knospendem Amethyst hängen, werden gezählt.

## Befehle (`/erl`)

```
/erl toggle <blockesp|entityesp|playeresp|geode|antivanish|hud|freecam>
/erl status
/erl blockesp add <block>              z.B. /erl blockesp add minecraft:gold_ore
/erl blockesp color <hex> <block>      Box-Farbe, z.B. /erl blockesp color FFD700 minecraft:gold_ore
/erl blockesp tracercolor <hex> <block>
/erl blockesp mode <box|beide|tracer> [block]    ohne Block = alle Einträge
/erl blockesp remove <block> | list | clear | range <n> | max <n> | fill
/erl entityesp add | color | tracercolor | mode | remove | list | clear | range | max | labels | fill
/erl playeresp range <n> | names | fill | mode <box|beide|tracer> | tracercolor <hex>
/erl geode range <n> | budding | growing | notify | sound | mode <..> | tracercolor <hex> | stats | reset
/erl antivanish list | clear | command <cmd> | interval <s> | timeout <ms>
/erl antivanish leave | completion | entity | spectator | sound | highlight | mode <..> | tracercolor <hex>
/erl tracers all <true|false> | width <breite>
/erl reload
```

Alle Einstellungen liegen zusätzlich in `config/erleichterungen.json`.

## Hinweis
ESP- und Anti-Vanish-Funktionen sind auf vielen Servern laut Regeln verboten.
Nutzung auf eigene Verantwortung – im Einzelspieler bzw. auf eigenen Servern unproblematisch.

## Automatischer Test

`./gradlew runClientGameTest` startet Minecraft, erstellt eine Testwelt, platziert Erz/Geode/Item Frame/
Rüstungsständer, öffnet die GUI und den Listen-Editor, testet Freecam (Spieler darf sich nicht bewegen)
und speichert Screenshots unter `build/run/clientGameTest/screenshots/`.

## Technik
- Minecraft 26.3, Fabric Loader 0.20.1, Fabric API 0.166.0+26.3, Java 25
- Rendering über Vanilla-*Gizmos* (`always on top`) im Event `LevelRenderEvents.BEFORE_GIZMOS`
- Block-Scan pro Chunk mit Paletten-Vorfilter (`maybeHas`) und Live-Updates über Block-Update-Pakete
