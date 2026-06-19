# Release Notes

## v1.1.0 – 2026-06-19

Performance-, Stabilitäts- und Bedienungs-Update.

### Neu
- **Paralleler Scan:** Das Einlesen der Verzeichnisstruktur läuft jetzt über mehrere Threads gleichzeitig (ein Task pro Ordner, verteilt über einen `ForkJoinPool`). Große Laufwerke werden dadurch spürbar schneller erfasst.
- **Mehrfachauswahl löschen:** Mehrere Einträge lassen sich mit **Strg-** bzw. **Umschalt-Klick** markieren und in einem Schritt löschen. Der Bestätigungsdialog zeigt Anzahl und Gesamtgröße.
- **Entf-Taste:** Löscht die aktuell ausgewählten Einträge (alternativ zum Kontextmenü).

### Verbessert
- **Aufgeklappte Zweige bleiben erhalten:** Nach dem Löschen und nach einem Rescan wird der bearbeitete Zweig wieder aufgeklappt, statt dass der ganze Baum zusammenklappt. Nach dem Löschen wird zudem der Elternordner ausgewählt, damit man im selben Bereich weiterarbeiten kann.
- **Kein UI-Stocken bei großen Bäumen:** Größen werden nach Änderungen nur noch entlang der betroffenen Pfade neu berechnet, nicht mehr über den gesamten Baum.
- **Durchgehend kommentierter Quellcode** für bessere Nachvollziehbarkeit.

### Behoben
- **Aufhängen beim Löschen von Ordnern:** Das Löschen folgt keinen Verknüpfungen mehr (Umstellung auf `Files.walkFileTree`, ohne Symlinks/Junctions zu verfolgen). Das behebt Endlosschleifen bei zyklischen Junctions – wie sie z. B. unter `C:\Users\…` vorkommen – und verhindert, dass über einen Link versehentlich Daten außerhalb des gewählten Ordners gelöscht werden.

### Hinweise
- DiskAnalyzer löscht weiterhin **endgültig** (nicht in den Papierkorb). Gerade bei der neuen Mehrfachauswahl vor dem Bestätigen prüfen, was markiert ist.
- Voraussetzung unverändert: **Java-Laufzeitumgebung (JRE 17+)**.

---

### English summary

**v1.1.0 (2026-06-19)** — Parallel directory scan (one task per folder via a `ForkJoinPool`) for noticeably faster scans on large drives. Multi-select delete (Ctrl/Shift-click) and Delete-key support. Expanded branches now stay open after delete and rescan, and the parent folder is selected after deletion. Size totals are recomputed only along affected paths to avoid UI stalls. **Fix:** deleting folders no longer hangs — deletion now uses `Files.walkFileTree` and does not follow symlinks/junctions, preventing infinite loops on cyclic junctions (e.g. under `C:\Users\…`) and accidental deletion outside the selected folder. Source code is now fully commented. Requires a Java Runtime (JRE 17+).
