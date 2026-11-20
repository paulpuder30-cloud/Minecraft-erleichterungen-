# Module

> **Automatisch generiert** aus dem Quellcode mit `tools/generate_modules_md.py`.
> Nicht von Hand bearbeiten – stattdessen das Skript erneut ausführen.

**Module gesamt:** 18  
**Setting-Typen:** Bool (An/Aus), Int (Ganzzahl), Dec (Dezimal), Color (Hex), Text, Mode (Darstellung), ListEdit (ID-Liste mit Farbe), Action (Knopf).

**Pro Kategorie:** Render: 10  Bewegung: 2  Welt: 1  Spieler: 2  Sonstiges: 1  Client: 2

Dieser Client gruppiert Einstellungen **flach pro Modul** (keine Unter-Gruppen wie bei Meteor); die Spalte „Feld" nennt das zugehörige Config-Feld in `ModConfig`.


## Render  

### Block-ESP  
`id: blockesp` · Kategorie: **Render** · An/Aus-Modul  
Markiert ausgewählte Blöcke (Erze, Spawner, ...) durch Wände.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Blöcke | ListEdit | Voreinstellung (Liste) | Liste von IDs + Farbe + Modus | `blockEsp` |
| Reichweite | Int | 96 | 8 … 512 | `blockEspRange` |
| Max. Boxen | Int | 1500 | 10 … 20000 | `blockEspMaxBoxes` |
| Füllung | Bool | An | An / Aus | `blockEspFill` |
| Linienbreite | Dec | 2.0 | 0.5 … 6 (Schritt 0.5) | `blockEspLineWidth` |
| Alle: Box | Action | — | Aktion (Knopf) | `—` |
| Alle: Box & Tracer | Action | — | Aktion (Knopf) | `—` |
| Alle: Nur Tracer | Action | — | Aktion (Knopf) | `—` |

### Entity-ESP  
`id: entityesp` · Kategorie: **Render** · An/Aus-Modul  
Markiert Base-typische Entities: Item Frames, Rüstungsständer, Loren, ...

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Entities | ListEdit | Voreinstellung (Liste) | Liste von IDs + Farbe + Modus | `entityEsp` |
| Reichweite | Int | 128 | 8 … 512 | `entityEspRange` |
| Max. Boxen | Int | 1000 | 10 … 20000 | `entityEspMaxBoxes` |
| Füllung | Bool | Aus | An / Aus | `entityEspFill` |
| Linienbreite | Dec | 2.0 | 0.5 … 6 (Schritt 0.5) | `entityEspLineWidth` |
| Beschriftung | Bool | An | An / Aus | `entityEspLabels` |
| Alle: Box | Action | — | Aktion (Knopf) | `—` |
| Alle: Box & Tracer | Action | — | Aktion (Knopf) | `—` |
| Alle: Nur Tracer | Action | — | Aktion (Knopf) | `—` |

### Player-ESP  
`id: playeresp` · Kategorie: **Render** · An/Aus-Modul  
Boxen, Namen und Entfernung aller Spieler durch Wände.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Reichweite | Int | 256 | 8 … 1024 | `playerEspRange` |
| Modus | Mode | Box & Tracer | Box / Box & Tracer / Nur Tracer | `playerEspMode` |
| Box-Farbe | Color | #FF3B3B | Hex-Farbe (RRGGBB) | `playerEspColor` |
| Tracer-Farbe | Color | #FF3B3B | Hex-Farbe (RRGGBB) | `playerEspTracerColor` |
| Füllung | Bool | Aus | An / Aus | `playerEspFill` |
| Linienbreite | Dec | 2.5 | 0.5 … 6 (Schritt 0.5) | `playerEspLineWidth` |
| Namen | Bool | An | An / Aus | `playerEspNames` |

### Tracer  
`id: tracers` · Kategorie: **Render** · Aktions-/Einstellungs-Modul  
Gemeinsame Einstellungen für alle Tracer-Linien.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Linienbreite | Dec | 1.5 | 0.5 … 10 (Schritt 0.5) | `tracerWidth` |
| Alle Tracer an | Action | — | Aktion (Knopf) | `setAllTracers` |
| Alle Tracer aus | Action | — | Aktion (Knopf) | `setAllTracers` |

### Zoom  
`id: zoom` · Kategorie: **Render** · Aktions-/Einstellungs-Modul  
Verringert das Sichtfeld, solange die Zoom-Taste gehalten wird.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Zoom-FOV | Int | 30 | 30 … 110 | `zoomFov` |

### Fullbright  
`id: fullbright` · Kategorie: **Render** · An/Aus-Modul  
Setzt die Helligkeit (Gamma) auf das Maximum.

_Keine Einstellungen._

### Nametags  
`id: nametags` · Kategorie: **Render** · An/Aus-Modul  
Name, Gesundheit und Entfernung über allen Spielern.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Reichweite | Int | 128 | 16 … 512 | `nametagsRange` |
| Gesundheit | Bool | An | An / Aus | `nametagsHealth` |
| Entfernung | Bool | An | An / Aus | `nametagsDistance` |
| Größe | Dec | 1.0 | 0.5 … 3 (Schritt 0.1) | `nametagsScale` |
| Farbe | Color | #FFFFFF | Hex-Farbe (RRGGBB) | `nametagsColor` |

### Item-ESP  
`id: itemesp` · Kategorie: **Render** · An/Aus-Modul  
Markiert am Boden liegende Gegenstände mit Box, Name und Anzahl.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Reichweite | Int | 96 | 8 … 512 | `itemEspRange` |
| Modus | Mode | Box | Box / Box & Tracer / Nur Tracer | `itemEspMode` |
| Box-Farbe | Color | #55FFFF | Hex-Farbe (RRGGBB) | `itemEspColor` |
| Tracer-Farbe | Color | #55FFFF | Hex-Farbe (RRGGBB) | `itemEspTracerColor` |
| Füllung | Bool | Aus | An / Aus | `itemEspFill` |
| Linienbreite | Dec | 2.0 | 0.5 … 6 (Schritt 0.5) | `itemEspLineWidth` |
| Beschriftung | Bool | An | An / Aus | `itemEspLabels` |

### Block-Highlight  
`id: blockhighlight` · Kategorie: **Render** · An/Aus-Modul  
Hebt den Block hervor, auf den du schaust.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Farbe | Color | #FFFFFF | Hex-Farbe (RRGGBB) | `blockHighlightColor` |
| Füllung | Bool | Aus | An / Aus | `blockHighlightFill` |
| Durch Wände | Bool | Aus | An / Aus | `blockHighlightThroughWalls` |
| Linienbreite | Dec | 2.0 | 0.5 … 6 (Schritt 0.5) | `blockHighlightLineWidth` |

### NoHurtCam  
`id: nohurtcam` · Kategorie: **Render** · An/Aus-Modul  
Unterdrückt das Kamera-Wackeln beim Schadennehmen.

_Keine Einstellungen._


## Bewegung  

### Auto-Sprint  
`id: autosprint` · Kategorie: **Bewegung** · An/Aus-Modul  
Sprintet automatisch, sobald du vorwärts läufst.

_Keine Einstellungen._

### Auto-Walk  
`id: autowalk` · Kategorie: **Bewegung** · An/Aus-Modul  
Hält automatisch die Vorwärtstaste gedrückt.

_Keine Einstellungen._


## Welt  

### Geoden-Tracker  
`id: geode` · Kategorie: **Welt** · An/Aus-Modul  
Zeigt Amethyst-Knospen nach Wachstumsstufe und meldet fertige Cluster.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Reichweite | Int | 64 | 8 … 512 | `geodeRange` |
| Modus | Mode | Box | Box / Box & Tracer / Nur Tracer | `geodeMode` |
| Tracer-Farbe | Color | #3BFF5A | Hex-Farbe (RRGGBB) | `geodeTracerColor` |
| Knospender Amethyst | Bool | An | An / Aus | `geodeShowBudding` |
| Wachsende Knospen | Bool | An | An / Aus | `geodeShowGrowing` |
| Benachrichtigung | Bool | An | An / Aus | `geodeNotifyFullyGrown` |
| Sound | Bool | An | An / Aus | `geodeSound` |
| Statistik zurücksetzen | Action | — | Aktion (Knopf) | `—` |


## Spieler  

### Freecam  
`id: freecam` · Kategorie: **Spieler** · An/Aus-Modul  
Kamera frei bewegen, der Spieler bleibt stehen. WASD, Leertaste/Shift hoch/runter, Sprint = schneller.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Geschwindigkeit | Dec | 1.0 | 0.1 … 5 (Schritt 0.1) | `freecamSpeed` |

### Auto-Respawn  
`id: autorespawn` · Kategorie: **Spieler** · An/Aus-Modul  
Belebt dich nach dem Tod automatisch wieder.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Verzögerung (ms) | Int | 100 | 0 … 5000 | `autoRespawnDelayMs` |


## Sonstiges  

### Anti-Vanish  
`id: antivanish` · Kategorie: **Sonstiges** · An/Aus-Modul  
Erkennt Spieler, die sich per Vanish verstecken.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Leave-Nachricht | Bool | An | An / Aus | `antiVanishLeaveMessageCheck` |
| Leave-Timeout (ms) | Int | 2500 | 250 … 30000 | `antiVanishLeaveTimeoutMs` |
| Tab-Vervollständigung | Bool | An | An / Aus | `antiVanishCompletionCheck` |
| Befehl | Text | msg | Text | `antiVanishCompletionCommand` |
| Intervall (s) | Int | 10 | 2 … 600 | `antiVanishCompletionIntervalSec` |
| Entity-Prüfung | Bool | An | An / Aus | `antiVanishEntityCheck` |
| Zuschauermodus | Bool | An | An / Aus | `antiVanishSpectatorCheck` |
| Sound | Bool | An | An / Aus | `antiVanishSound` |
| Verdächtige markieren | Bool | An | An / Aus | `antiVanishHighlight` |
| Modus | Mode | Box & Tracer | Box / Box & Tracer / Nur Tracer | `antiVanishMode` |
| Box-Farbe | Color | #FF00FF | Hex-Farbe (RRGGBB) | `playerEspVanishedColor` |
| Tracer-Farbe | Color | #FF00FF | Hex-Farbe (RRGGBB) | `antiVanishTracerColor` |
| Liste leeren | Action | — | Aktion (Knopf) | `—` |


## Client  

### HUD  
`id: hud` · Kategorie: **Client** · An/Aus-Modul  
Info-Overlay oben links.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Koordinaten | Bool | An | An / Aus | `hudCoords` |
| Blickrichtung | Bool | An | An / Aus | `hudDirection` |
| FPS | Bool | An | An / Aus | `hudFps` |

### GUI  
`id: gui` · Kategorie: **Client** · Aktions-/Einstellungs-Modul  
Aussehen dieser Oberfläche.

| Einstellung | Typ | Standard | Bereich / Werte | Feld |
|---|---|---|---|---|
| Akzentfarbe | Color | #8A5CFF | Hex-Farbe (RRGGBB) | `guiAccent` |
| Positionen zurücksetzen | Action | — | Aktion (Knopf) | `guiPanels` |

