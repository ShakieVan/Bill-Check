# Rechnungsabgleich

Status: verbindlich

## Entscheidung

Jede Zwischen- und Endrechnung ist ein eigenständiger Abgleichslauf. Eine
Rechnungszeile kann höchstens einen Beleg referenzieren und ein Beleg kann über
alle Läufe seiner Reise hinweg höchstens einmal zugeordnet sein. Room erzwingt
beide Seiten dieser 1:1-Regel mit eindeutigen Indizes.

Automatisch zugeordnet wird nur, wenn normalisierte Checknummer, Minor-Unit-
Betrag und Währung übereinstimmen. Checknummern werden ohne Trennzeichen und
führende Nullen verglichen. Für manuelle Entscheidungen werden noch freie
Belege anhand von Checknummer, Betrag, Datum, Währung und Wortüberschneidungen
zwischen Rechnungsbeschreibung und Ort gerankt.

## Status

- `CORRECT`: sicherer oder manuell bestätigter gleicher Betrag, grün
- `UNCERTAIN`: manuelle Zuordnung ohne gleiche Checknummer, aber mit gleichem
  Betrag, gelb
- `AMOUNT_MISMATCH`: gleiche Checknummer bei abweichendem Betrag, orange
- `NOT_FOUND`: kein zugeordneter Beleg, rot
- `ACCEPTED`: bewusst als bekannt akzeptierter Fremdposten, grün

## Folgen

- Spätere Zwischen- oder Endrechnungen sehen bestätigte Belege standardmäßig
  nicht erneut.
- Zurücksetzen entfernt nur die Zuordnungen des gewählten Laufs und gibt seine
  Belege wieder frei.
- Das Löschen eines Laufs entfernt durch Room-Kaskaden auch seine Zeilen und
  Zuordnungen, niemals jedoch Belege oder Galerieoriginale.
