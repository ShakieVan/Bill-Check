# Produktspezifikation

Stand: 16.08.2026

## Kernablauf

1. Aktive Reise in der Übersicht öffnen.
2. Beleg mit Kamera aufnehmen oder vorhandenes Bild auswählen.
3. Online durch einen konfigurierbaren KI-Anbieter auswerten oder manuell
   erfassen.
4. Ergebnis sichtbar prüfen und mit „OK“ bestätigen.
5. Zwischen- oder Endrechnung als eigenen Abgleich erfassen.
6. Automatische Vorschläge kontrollieren und ungeklärte Zeilen manuell
   zuordnen oder als bekannt akzeptieren.

## Reisen

- Reisen haben einen Namen, eine unsichtbare Sortierposition, eine
  Fremdwährung, einen Standardwechselkurs sowie Standardtrinkgeld mit Währung.
- Die Reihenfolge ist per Drag-and-drop veränderbar. Sortierpositionen werden
  anschließend lückenlos neu vergeben.
- Der erste Vorschlag lautet „Reise 1“.

## Geld und Rundung

- Version 1 unterstützt EGP und EUR; das Modell ist für weitere ISO-Währungen
  vorbereitet.
- Ein Beleg speichert Fremdbetrag, Kurs-Snapshot, exakten Eurobetrag und
  optionales Trinkgeld.
- Eine Reise wählt zwischen festem Kurs und täglicher Online-Aktualisierung.
  Schlägt die Online-Abfrage fehl, bleibt der manuell eingestellte Kurs der
  Offline-Fallback.
- Exakte Eurobeträge werden kaufmännisch auf Cent gerundet.
- Die prominente Beleganzeige rundet den exakten Gesamtbetrag inklusive
  Trinkgeld immer nach oben auf volle Euro.
- Die prominente Reisesumme addiert zuerst die centgenauen Beträge und rundet
  erst das Ergebnis nach oben.

## Bilder

- Kameraaufnahmen werden in Originalqualität im Galeriealbum `Bill Check`
  gespeichert.
- Eine optionale speicheroptimierte Ablage wird später angeboten; die
  Auswertung verwendet vorher das Original.
- App-Löschen trennt standardmäßig nur die Verknüpfung und lässt das
  Galerieoriginal bestehen.
- Galerieimporte werden nicht dupliziert.
- Jede Aufnahme und Auswahl wird vor dem Anlegen des Eintrags groß angezeigt
  und ausdrücklich bestätigt.
- Verknüpfte Bilder werden in der Belegliste als Miniatur angezeigt und können
  dort ersetzt oder vom Eintrag gelöst werden.

## Belegposten

- Ein Beleg kann beliebig viele benannte Einzelposten in seiner
  Fremdwährung enthalten.
- Ist kein Gesamtbetrag eingetragen, wird die Postensumme als Gesamtbetrag
  übernommen.
- Weicht ein eingetragener Gesamtbetrag von der Postensumme ab, wird die
  Differenz sichtbar, das Speichern bleibt wegen Gebühren und Rabatten
  möglich.

## Abgleich

- Jede Zwischen- oder Endrechnung erzeugt einen unabhängigen Abgleichslauf.
- Bereits erfolgreich abgeglichene Belege werden bei späteren Läufen
  standardmäßig nicht erneut berücksichtigt.
- Statusfarben: grün korrekt, gelb unsicher, orange Betragsabweichung, rot
  nicht gefunden.
- Eine Rechnungszeile und ein Beleg werden 1:1 zugeordnet.
- Manuelle Zuordnung zeigt gerankte Kandidaten nach Checknummer, Betrag, Datum
  und Ort.
- Fremde oder absichtlich nicht protokollierte Posten können als
  „bekannt/akzeptiert“ markiert werden.
- Zuordnungen können zurückgesetzt, der Lauf neu ausgewertet oder der gesamte
  Abgleich gelöscht werden.

## Offline und KI

- Alle gespeicherten Daten und die manuelle Eingabe funktionieren offline.
- Gemini ist der erste Cloud-Anbieter; die Architektur bleibt für OpenAI und
  weitere Anbieter offen.
- Eine lokale OCR-Bausteinhilfe folgt nach dem stabilen Cloud- und
  Grundworkflow. Erkannte Textblöcke können dann durch Antippen oder Ziehen in
  Eingabefelder übernommen werden.

## Sicherung und Export

- CSV und PDF dienen Bericht und Austausch.
- `.billcheck` ist ein vollständiges, selektiv exportier- und importierbares
  Sicherungsarchiv einschließlich Bilder und Abgleichstatus.
- Mehrere Reisen können beim Export und Import ausgewählt werden.
