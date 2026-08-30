# Homescreen-Widget

## Entscheidung

Bill Check stellt ein kompaktes, frei platzierbares Homescreen-Widget bereit.
Es zeigt ausschließlich die Übersicht der zuletzt in der App gewählten Reise:

- Reisebezeichnung
- prominent die aus der exakten Gesamtsumme immer aufgerundeten Haupteinheiten
  der Reise-Heimatwährung
- exakte Gesamtsumme in der Reise-Heimatwährung
- Anzahl der Belege

Die gesamte Fläche öffnet die App. Vier getrennte Schnellaktionen starten
direkt Kamera, Bildauswahl, manuellen Beleg oder Rechnungsabgleich. Ein
automatisches Weitergehen oder Speichern findet auch über diese Einstiege
nicht statt.

## Daten und Aktualisierung

- Die Reise-ID liegt nur in privaten App-Einstellungen.
- Der Widget-Provider liest Room im Hintergrund und rendert klassische
  `RemoteViews`; damit bleibt das Widget unabhängig von einer laufenden App.
- Änderungen an Reisen, Belegen oder Auswahl stoßen eine Aktualisierung an.
- Zusätzlich ist ein sparsames Systemintervall von 30 Minuten als Rückfall
  eingetragen.
- Ist die ausgewählte Reise nicht mehr vorhanden, wird die erste vorhandene
  Reise verwendet. Ohne Reise zeigt das Widget einen neutralen Leerzustand.

## Gestaltung

Das Widget folgt dem ruhigen Weiß-/Grau-/Blau-Schema der App und besitzt
eigene Farben für Hell- und Dunkeldarstellung. Es enthält absichtlich weder
Postenlisten noch Statusdetails; dafür öffnet ein Tipp die vollständige App.

