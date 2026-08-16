# Sicherung, Bericht und Wiederherstellung

Status: verbindlich

## Formate

- `.billcheck` ist ein ZIP-basiertes, versioniertes Vollarchiv. `manifest.json`
  enthält Reisen, Belege, Einzelposten, Rechnungsabgleiche, Status und
  Zuordnungen. Verknüpfte Beleg- und Rechnungsbilder liegen als eigene
  Archiveinträge daneben.
- CSV ist eine UTF-8-/Excel-freundliche Übersicht mit Semikolon als Trennzeichen
  und einem Bill-Check-Formatkopf. Es enthält Reisen, Belegübersichten und
  Abgleichzeilen, bewusst aber keine Bilder und keine Einzelposten. Derselbe
  Bericht kann wieder importiert werden und stellt die Abgleichansicht her.
- PDF ist ein nur lesbarer, druckbarer Reise- und Abgleichbericht.

## Selektiver Ablauf

Vor jedem Export werden eine oder mehrere Reisen gewählt. Beim Import liest
die App zunächst ausschließlich das Manifest beziehungsweise die CSV-Struktur
und zeigt gefundene Reisen samt Beleg- und Abgleichanzahl. Erst nach der
zweiten Auswahl werden Daten geschrieben.

Importierte Reisen erhalten immer neue UUIDs und kollidieren deshalb nicht mit
vorhandenen Datensätzen. Bei bereits vorhandenem Namen wird „(Import)“ mit
fortlaufender Nummer ergänzt. Bilder aus einem Vollarchiv werden wieder im
Galeriealbum `DCIM/Bill Check` veröffentlicht. Original-IDs werden intern auf
die neuen IDs abgebildet, sodass 1:1-Zuordnungen erhalten bleiben.

## Sicherheit und Grenzen

- Der Manifestleser akzeptiert nur Formatversion 1 und begrenzt das Manifest
  auf 10 MiB.
- Es werden nur explizit im Manifest referenzierte Bilder wiederhergestellt;
  beliebige ZIP-Pfade werden niemals auf das Dateisystem geschrieben.
- API-Schlüssel sind nicht Teil eines Exports.
- CSV ist ein Austausch-/Berichtsformat und kann deshalb weder Bilder noch
  Einzelposten wiederherstellen. Für eine vollständige Sicherung ist immer
  `.billcheck` zu verwenden.

