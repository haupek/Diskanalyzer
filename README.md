# DiskAnalyzer

> Leichtgewichtiges Windows-Tool zum Aufräumen von Laufwerken – entstanden im Rahmen von [AI Coding](https://haupek.github.io), gebaut mit KI.

DiskAnalyzer liest ein verbundenes Laufwerk aus und zeigt dir auf einen Blick, **wo der Speicher hingeht**. Ordner werden auf jeder Ebene absteigend nach Größe sortiert – so findest du die größten Speicherfresser sofort und kannst sie direkt öffnen oder löschen.

## Funktionen

- **Geordnet nach Größe** – auf jeder Ebene absteigend; die Größe eines Ordners wird über alle Unterordner aufsummiert.
- **Direktes Aufräumen** – Dateien und Ordner lassen sich aus der Ansicht heraus öffnen und löschen.
- **Leichtgewichtig** – keine Installation, einfach starten.

## Voraussetzungen

DiskAnalyzer ist eine Java-Anwendung. Zum Ausführen brauchst du eine installierte **Java-Laufzeitumgebung (JRE 17 oder neuer)**. Ob Java installiert ist, prüfst du in der Eingabeaufforderung mit:

```
java -version
```

Falls nicht vorhanden, bekommst du Java z. B. über [Adoptium / Temurin](https://adoptium.net).

## Download & Start

1. Lade die aktuelle `DiskAnalyzer.jar` unter [Releases](https://github.com/haupek/DiskAnalyzer/releases/latest) herunter.
2. Starte sie per Doppelklick – oder, falls das nicht klappt, in der Eingabeaufforderung:

```
java -jar DiskAnalyzer.jar
```

## ⚠️ Hinweis

DiskAnalyzer **löscht Dateien und Ordner endgültig** (nicht in den Papierkorb). Prüfe vor dem Löschen, was du auswählst. Die Nutzung erfolgt auf eigene Verantwortung – siehe Haftungsausschluss in der Lizenz.

## Aus dem Quellcode bauen

Das Projekt ist Open Source. Den Quellcode findest du in diesem Repository; das fertige `.jar` liegt unter [Releases](https://github.com/haupek/DiskAnalyzer/releases).

## Lizenz

Veröffentlicht unter der [MIT-Lizenz](LICENSE) – frei nutzbar, veränderbar und weitergebbar.

---

### English summary

**DiskAnalyzer** is a lightweight Windows tool for cleaning up drives. It scans a drive and sorts folders by size (largest first, summed across subfolders), letting you open and delete files and folders directly from the view. It is a Java app and requires a **Java Runtime (JRE 17+)**. Download the latest `DiskAnalyzer.jar` from the [Releases](https://github.com/haupek/DiskAnalyzer/releases/latest) page and run it. **Warning:** it deletes files permanently. Released under the [MIT license](LICENSE). Part of the [AI Coding](https://haupek.github.io) project.
