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

- Reisen haben einen Namen, eine unsichtbare Sortierposition, einen
  unveränderlichen Heimatwährungs-Snapshot, mehrere Reisewährungen samt Kurs
  und Modus sowie Standardtrinkgeld mit Währung.
- Jede Reise kann über das Stift-Symbol in der Reiseliste bearbeitet werden.
  Geänderte Währungs-, Kurs- und Trinkgeldvorgaben gelten für neue Belege;
  bestehende Belege behalten ihre historischen Snapshots.
- Im Bearbeitungsdialog kann die Reise nach einer ausdrücklichen Bestätigung
  vollständig aus der App gelöscht werden. Die Bestätigung nennt die Zahl der
  betroffenen Belege und Abgleiche; Galerieoriginale bleiben erhalten.
- Die Reihenfolge ist per Drag-and-drop veränderbar. Sortierpositionen werden
  anschließend lückenlos neu vergeben.
- Der Drag-Griff ist vom Stift für die Reisebearbeitung getrennt, damit
  Auswählen, Bearbeiten und Sortieren nicht miteinander kollidieren.
- Der erste Vorschlag lautet „Reise 1“.

## Geld und Rundung

- Die Einstellungen legen die Heimatwährung für neue Reisen fest. Bestehende
  Reisen behalten ihre Heimatwährung.
- Aktuelle ISO-Währungen sind offline nach Kürzel, Name und Land/Region
  durchsuchbar. Jede Reise enthält die Heimatwährung und beliebig viele weitere
  Reisewährungen; genau eine ist Standard für neue Belege.
- Ein Beleg speichert Betrag, Währung, Kurs-Snapshot, exakten Betrag in der
  Heimatwährung und optionales Trinkgeld mit eigenem Kurs-Snapshot.
- Jede Reisewährung wählt zwischen festem Kurs und täglicher Online-Aktualisierung.
  Schlägt die Online-Abfrage fehl, bleibt der manuell eingestellte Kurs der
  Offline-Fallback.
- Exakte Beträge werden kaufmännisch auf die ISO-Minor-Unit der Heimatwährung gerundet.
- Die prominente Beleganzeige rundet den exakten Gesamtbetrag inklusive
  Trinkgeld immer nach oben auf volle Haupteinheiten der Heimatwährung.
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
  und ausdrücklich bestätigt. Ein Tipp auf das Bild öffnet auch dort die
  bildschirmfüllende, zoombare Detailansicht.
- Verknüpfte Bilder werden in der Belegliste als Miniatur angezeigt und können
  dort per Tipp bildschirmfüllend geprüft werden. Ersetzen und Lösen erfolgen
  ausschließlich über den Belegeditor.
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
  Checknummer, Belegdatum, Gesamtbetrag, Trinkgeldwahl und Einzelposten
  bearbeitet werden. Das Datum aus einer Bilderkennung bleibt ein sichtbarer,
  korrigierbarer Vorschlag.
- Der Belegeditor besitzt einen festen Kopf und Aktionsbereich; nur sein Inhalt
  scrollt und bleibt auch bei geöffneter Bildschirmtastatur vollständig
  erreichbar.
- Für Ort/Restaurant und Postenbezeichnungen zeigt ein Verlaufssymbol die
  zuletzt in derselben Reise verwendeten eindeutigen Texte. Betragsfelder und
  Checknummern erhalten bewusst keine solchen Vorschläge.

## Abgleich

- Jede Zwischen- oder Endrechnung erzeugt einen unabhängigen Abgleichslauf.
- Jeder Lauf berücksichtigt und zeigt alle Belege der Reise, auch wenn ihr
  Datum auffällig ist oder sie in einem anderen Lauf bereits zugeordnet wurden.
- Statusfarben: grün korrekt, gelb unsicher, orange Betragsabweichung, rot
  nicht gefunden.
- Eine Rechnungszeile und ein Beleg werden innerhalb eines Laufs 1:1
  zugeordnet. Zwischen unabhängigen Zwischen- und Endrechnungen darf derselbe
  Beleg erneut geprüft werden.
- Automatischer und manueller Abgleich verwenden einen gewichteten,
  Levenshtein-basierten Trefferwert aus Checknummer, Betrag, Datum und Ort.
  Führende Nullen und eine vorangestellte Kassen-ID verhindern die Zuordnung
  nicht; automatische Treffer verlangen weiterhin centgenauen Betrag,
  passende Währung und einen eindeutigen Abstand zum nächsten Kandidaten.
- Die KI transkribiert das Rechnungsbild unabhängig von den gespeicherten
  Belegen und liefert Einzelzeilen, Roh- und normalisierte Datumswerte sowie
  die gedruckte Rechnungskontrollsumme. Schon eine ungültige KI-Zeile blockiert
  die gesamte atomare Übernahme; vorhandene Daten bleiben erhalten.
- Oberhalb der Einträge stehen vier lokale Kennzahlen-Kacheln für
  Rechnungssumme, Summe der zugeordneten Belege, nicht zugeordnete Belege und
  offene Rechnungsposten. Darunter folgt ein kurzes lokales Fazit: Bei bis zu
  drei Auffälligkeiten nennt es Ort, Datum und Checknummer konkret; größere
  Fehlerbilder werden bewusst zusammengefasst. Darunter werden
  erkannte Rechnungszeilen und sämtliche Belege gemeinsam chronologisch
  dargestellt. Die erkannte Zeilensumme wird lokal mit der gedruckten
  Kontrollsumme verglichen. Nur eine tatsächliche Abweichung wird im Kurzfazit
  erwähnt; eine fehlende zusätzliche Kontrollsumme wird nicht mit einer
  fehlenden Rechnungssumme verwechselt.
- Nach dem lokalen Abgleich kann die Cloud-KI aus den bereits geprüften Fakten
  eine gespeicherte, verständliche Zusammenfassung formulieren. Sie ist in der
  kompakten Übersicht optional aufklappbar. Ohne API-Key bleiben das lokale
  Kurzfazit und sämtliche Einträge vollständig nutzbar.
- Die Bezeichnung einer Rechnung ist in Übersicht und Detailkopf ausdrücklich
  gekennzeichnet und im Detail bearbeitbar.
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
  Textfeldern aneinandergefügt werden. Nach einer solchen Ergänzung stehen
  sichtbarer Ausschnitt und Cursor am neuen Textende; eine normale manuelle
  Cursorposition wird bei der Tastatureingabe nicht überschrieben. Lange Orts-
  und Postentexte brechen vollständig auf sichtbare Zeilen um, damit jede
  Cursorposition ohne horizontales Randscrollen erreichbar bleibt.

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
