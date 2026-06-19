# DiskAnalyzer

> Leichtgewichtiges Windows-Tool zum Aufräumen von Laufwerken – entstanden im Rahmen von [AI Coding](https://haupek.github.io), gebaut mit KI.

DiskAnalyzer liest ein verbundenes Laufwerk aus und zeigt dir auf einen Blick, **wo der Speicher hingeht**. Ordner werden auf jeder Ebene absteigend nach Größe sortiert – so findest du die größten Speicherfresser sofort und kannst sie direkt öffnen oder löschen.

## Funktionen

- **Geordnet nach Größe** – auf jeder Ebene absteigend; die Größe eines Ordners wird über alle Unterordner aufsummiert.
- **Schneller paralleler Scan** – das Einlesen verteilt die Ordner auf mehrere Threads gleichzeitig und erfasst große Laufwerke dadurch spürbar schneller.
- **Direktes Aufräumen** – Dateien und Ordner lassen sich aus der Ansicht heraus öffnen und löschen – einzeln oder mehrere auf einmal.
- **Leichtgewichtig** – keine Installation, einfach starten.

## Bedienung

- **Scannen** – Pfad eingeben oder über **Laufwerke** ein Laufwerk wählen und auf **SCAN** klicken.
- **Aufklappen** – Ordner per Klick öffnen; der Inhalt wird bei Bedarf nachgeladen.
- **Kontextmenü** – Rechtsklick auf einen Eintrag bietet *Rescan*, *Öffnen* und *Löschen*.
- **Mehrere auswählen** – Einträge mit **Strg-** oder **Umschalt-Klick** markieren und gemeinsam löschen.
- **Löschen** – über das Kontextmenü oder die **Entf**-Taste. Der Zweig, in dem du gerade arbeitest, bleibt nach dem Löschen und nach einem Rescan aufgeklappt.

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

DiskAnalyzer **löscht Dateien und Ordner endgültig** (nicht in den Papierkorb). Prüfe vor dem Löschen, was du auswählst – das gilt besonders bei der Mehrfachauswahl. Verknüpfungen (Symlinks/Junctions) werden dabei selbst entfernt, aber **nicht verfolgt**; es werden also keine Daten außerhalb des gewählten Ordners gelöscht. Die Nutzung erfolgt auf eigene Verantwortung – siehe Haftungsausschluss in der Lizenz.

## Was ist neu

Die Änderungen je Version stehen in den [Release Notes](RELEASE_NOTES.md).

## Aus dem Quellcode bauen

Das Projekt ist Open Source. Den Quellcode findest du in diesem Repository; das fertige `.jar` liegt unter [Releases](https://github.com/haupek/DiskAnalyzer/releases).

## Lizenz

Veröffentlicht unter der [MIT-Lizenz](LICENSE) – frei nutzbar, veränderbar und weitergebbar.

---

### English summary

**DiskAnalyzer** is a lightweight Windows tool for cleaning up drives. It scans a drive and sorts folders by size (largest first, summed across subfolders), letting you open and delete files and folders directly from the view – individually or several at once (multi-select with Ctrl/Shift, or the Delete key). The scan runs in parallel across multiple threads for faster results on large drives. It is a Java app and requires a **Java Runtime (JRE 17+)**. Download the latest `DiskAnalyzer.jar` from the [Releases](https://github.com/haupek/DiskAnalyzer/releases/latest) page and run it. **Warning:** it deletes files permanently and does not follow symlinks/junctions when deleting. Released under the [MIT license](LICENSE). Part of the [AI Coding](https://haupek.github.io) project.
