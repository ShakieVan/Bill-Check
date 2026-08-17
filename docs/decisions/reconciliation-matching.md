# Rechnungsabgleich

Status: verbindlich

## Entscheidung

Jede Zwischen- und Endrechnung ist ein eigenständiger Abgleichslauf. Eine
Rechnungszeile kann höchstens einen Beleg referenzieren und ein Beleg kann
innerhalb desselben Laufs höchstens einmal zugeordnet sein. Derselbe Beleg darf
in einer Zwischen- und einer späteren Endrechnung unabhängig geprüft werden.
Kein Beleg der Reise wird aufgrund seines Datums oder einer Zuordnung in einem
anderen Lauf ausgeblendet.

Noch freie Belege werden mit einem gewichteten Ähnlichkeitswert von 0 bis 100
gerankt: Checknummer 40 Punkte, centgenauer Betrag 30 Punkte, Datum 15 Punkte
und Ort/Restaurant 15 Punkte. Checknummer und Ort verwenden eine
Levenshtein-basierte unscharfe Ähnlichkeit; beim Ort fließt zusätzlich die
Wortüberschneidung ein. Datumsabweichungen bis zu zwei Tagen und geringe
Betragsabweichungen erhalten abgestufte Teilpunkte.

Checknummern werden ohne Trennzeichen und führende Nullen verglichen. Ist auf
der Rechnung vor der eigentlichen Checknummer zusätzlich eine Kassen-ID
angegeben, gilt eine mindestens vierstellige übereinstimmende Endung als
starkes Signal. Bei einer nur dreistelligen übereinstimmenden Endung müssen
Betrag, Währung und Datum exakt sein und der Ort ausreichend ähnlich sein.
Ein- oder zweistellige Endungen werden nur bei noch stärkerer Ortsähnlichkeit
automatisch verbunden und bleiben wegen ihrer geringen Aussagekraft sichtbar
`UNCERTAIN`.

Automatisch zugeordnet wird weiterhin nur bei centgenau gleichem Betrag und
gleicher Währung. Eine exakt normalisierte Checknummer genügt wie bisher. Bei
einer nur ähnlichen Checknummer sind mindestens 75 Punkte sowie eine
Bestätigung durch Datum oder Ort nötig. Außerdem muss der beste Kandidat
mindestens 10 Punkte vor dem zweitbesten liegen; mehrdeutige Fälle bleiben zur
manuellen Entscheidung offen.

Bei einer KI-Auswertung transkribiert der Provider zuerst ausschließlich das
Rechnungsbild. Belegdaten werden bewusst nicht in denselben Aufruf gegeben,
damit vorhandene Belege die Transkription nicht bestätigen, verändern oder
verkürzen. Neben allen Einzelzeilen werden die gedruckte Kontrollsumme und ihre
Währung extrahiert. Eine ungültige Zeile lehnt die gesamte Übernahme atomar ab;
vorhandene Zeilen bleiben erhalten.

Die App addiert alle erkannten Zeilen lokal und vergleicht sie centgenau mit
der gedruckten Kontrollsumme. In der kompakten Übersicht heißt die Summe der
erkannten Zeilen eindeutig „Rechnungssumme“. Eine fehlende zusätzliche
Kontrollsumme erzeugt dort keinen widersprüchlichen Warntext; nur eine echte
Abweichung wird im Kurzfazit erwähnt. Eine passende Summe ist weiterhin nur
eine Kontrollsumme und kein Beweis gegen gleich teure Duplikate oder andere
Datenfehler.

Nach dem lokalen Abgleich darf die KI ausschließlich aus dem geprüften
Ergebnis eine verständliche Zusammenfassung formulieren. Lokale Kennzahlen
und die gemischte chronologische Liste funktionieren auch ohne Cloud. Die
lokale Zusammenfassung nennt bei bis zu drei Auffälligkeiten konkrete Details
und verdichtet größere Fehlerbilder. Die KI-Zusammenfassung wird am Abgleich
gespeichert, optional aufgeklappt und bei Änderungen an Belegen, Zeilen oder
Zuordnungen als veraltet entfernt.

## Status

- `CORRECT`: sicherer oder manuell bestätigter gleicher Betrag, grün
- `UNCERTAIN`: manuelle Zuordnung ohne gleiche Checknummer, aber mit gleichem
  Betrag, gelb
- `AMOUNT_MISMATCH`: gleiche Checknummer bei abweichendem Betrag, orange
- `CURRENCY_MISMATCH`: gleiche Checknummer bei abweichender Währung, rot
- `DATE_MISMATCH`: ID und Betrag passen, das Datum weicht aber mehr als zwei
  Tage ab; die Zuordnung bleibt zur Korrektur sichtbar, orange
- `NOT_FOUND`: kein zugeordneter Beleg, rot
- `ACCEPTED`: bewusst als bekannt akzeptierter Fremdposten, grün

## Folgen

- Jeder Lauf sieht ausnahmslos alle Belege der Reise. Belege außerhalb des
  erkannten Rechnungsdatumsbereichs werden als Datenwarnung markiert, aber
  niemals ausgefiltert.
- Zurücksetzen entfernt nur die Zuordnungen des gewählten Laufs und gibt seine
  Belege wieder frei.
- Das Löschen eines Laufs entfernt durch Room-Kaskaden auch seine Zeilen und
  Zuordnungen, niemals jedoch Belege oder Galerieoriginale.
- Erkannte Rechnungszeilen ohne Beleg und alle im aktuellen Lauf nicht
  zugeordneten Belege
  erscheinen gemeinsam chronologisch unter einer lokalen Zusammenfassung.
