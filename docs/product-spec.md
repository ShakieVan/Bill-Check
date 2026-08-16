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

## Navigation

- Das Burgermenü links oben enthält die Reisen und das Anlegen einer neuen
  Reise. Die aktive Reise bleibt in der Übersicht kompakt sichtbar.
- Das Dreipunktmenü rechts oben ist der Einstieg für die späteren
  App-Einstellungen.

## Reisen

- Reisen haben einen Namen, eine unsichtbare Sortierposition, eine
  Fremdwährung, einen Standardwechselkurs sowie Standardtrinkgeld mit Währung.
- Jede Reise kann über das Stift-Symbol in der Reiseliste bearbeitet werden.
  Geänderte Währungs-, Kurs- und Trinkgeldvorgaben gelten für neue Belege;
  bestehende Belege behalten ihre historischen Snapshots.
- Die Reihenfolge ist per Drag-and-drop veränderbar. Sortierpositionen werden
  anschließend lückenlos neu vergeben.
- Der Drag-Griff ist vom Stift für die Reisebearbeitung getrennt, damit
  Auswählen, Bearbeiten und Sortieren nicht miteinander kollidieren.
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
- Neben dem Android Photo Picker steht eine Ordnerauswahl zur Verfügung. Sie
  startet nach Möglichkeit in `DCIM/Bill Check`, weil der Google-Photo-Picker
  lokale Ordner nicht verlässlich als Alben darstellt.
- Jede Aufnahme und Auswahl wird vor dem Anlegen des Eintrags groß angezeigt
  und ausdrücklich bestätigt.
- Verknüpfte Bilder werden in der Belegliste als Miniatur angezeigt und können
  dort ersetzt oder vom Eintrag gelöst werden.
- Im Belegeditor öffnet ein Tipp auf die Miniatur eine bildschirmfüllende
  Ansicht mit Pinch-Zoom, Verschieben und Doppeltipp-Zoom. Ein eigener
  „Bearbeiten“-Button trennt diese Prüfung klar von Ersetzen und Entknüpfen.
- Kamera und Photo Picker sind auch direkt im Editor neuer und bestehender
  Belege erreichbar. Noch nicht gespeicherte Felder und Posten bleiben beim
  Wechsel zur Bildprüfung erhalten.
- Ein verknüpftes Bild bietet im Editor den neutralen Einstieg „Bild
  auswerten“. Das Cloud-Ergebnis wird erst nach einem stehenbleibenden
  Ergebnisdialog in das weiterhin editierbare Formular übernommen.

## Belegposten

- Ein Beleg kann beliebig viele benannte Einzelposten in seiner
  Fremdwährung enthalten.
- Ist kein Gesamtbetrag eingetragen, wird die Postensumme als Gesamtbetrag
  übernommen.
- Weicht ein eingetragener Gesamtbetrag von der Postensumme ab, wird die
  Differenz sichtbar, das Speichern bleibt wegen Gebühren und Rabatten
  möglich.
- Ein gespeicherter Beleg kann durch Antippen einschließlich Ort,
  Checknummer, Gesamtbetrag, Trinkgeldwahl und Einzelposten bearbeitet werden.
- Der Belegeditor besitzt einen festen Kopf und Aktionsbereich; nur sein Inhalt
  scrollt und bleibt auch bei geöffneter Bildschirmtastatur vollständig
  erreichbar.
- Für Ort/Restaurant und Postenbezeichnungen zeigt ein Verlaufssymbol die
  zuletzt in derselben Reise verwendeten eindeutigen Texte. Betragsfelder und
  Checknummern erhalten bewusst keine solchen Vorschläge.

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
- API-Schlüssel werden mit Android Keystore verschlüsselt und können in den
  Einstellungen wieder entfernt werden.
- Verfügbare Gemini-Modelle und Kontextgrenzen sind abfragbar. Da Gemini kein
  Restkontingent über den API-Schlüssel liefert, führt ein Link zur
  Rate-Limit-/Nutzungsansicht in Google AI Studio.
- Bei Belegen enthält „Ort/Restaurant“ nur den konkreten Bewirtungsort, nicht
  Hotelname, Stadt oder Adresse.
- Die lokale OCR-Bausteinhilfe erkennt Text offline. Nach Wahl eines Zielfelds
  können einzelne Wörter oder Beträge per Tipp kontrolliert übernommen und bei
  Textfeldern aneinandergefügt werden.

## Darstellung

- Beim ersten App-Start wird die aktuelle helle oder dunkle Systemdarstellung
  als Anfangswert übernommen.
- Danach ist Hell oder Dunkel ausdrücklich in den Einstellungen wählbar und
  bleibt unabhängig von späteren Systemwechseln gespeichert.

## Sicherung und Export

- CSV und PDF dienen Bericht und Austausch.
- `.billcheck` ist ein vollständiges, selektiv exportier- und importierbares
  Sicherungsarchiv einschließlich Bilder und Abgleichstatus.
- Mehrere Reisen können beim Export und Import ausgewählt werden.
- Ein CSV-Bericht enthält Reise-, Beleg- und Abgleichübersichten ohne Bilder
  und Einzelposten. Beim erneuten Import entsteht wieder eine bedienbare
  Abgleichansicht.
- Importierte Reisen sind neue Datensätze; Namenskollisionen werden mit einem
  sichtbaren „(Import)“-Zusatz aufgelöst.
