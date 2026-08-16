# Belegbilder: Kamera, Galerie und Verknüpfung

Status: verbindlich für Android 16

## Entscheidung

Kameraaufnahmen werden über die installierte Systemkamera direkt in einen
von Bill Check angelegten `MediaStore`-Eintrag geschrieben. Das Ziel liegt im
Galeriealbum `Pictures/Bill Check`, verwendet JPEG und behält die von der
Kamera gelieferte Auflösung. Die App fordert dafür weder Kamera- noch breite
Speicherberechtigungen an.

Vorhandene Bilder werden ausschließlich über den Android Photo Picker gewählt.
Bill Check merkt sich die Leseberechtigung des ausgewählten URI und erzeugt
keine Kopie.

## Lebenszyklen

- Nach Aufnahme oder Auswahl erscheint immer zuerst eine bildschirmfüllende
  Prüfansicht. Erst „Bild verwenden“ verknüpft das Bild mit einem neuen Eintrag.
- Ein verknüpftes Bild lässt sich über seine Miniatur erneut öffnen, ersetzen
  oder vom Eintrag lösen.
- Das Lösen der Verknüpfung und das Löschen eines Belegs löschen niemals das
  Galerieoriginal.
- Eine abgebrochene Kameraaufnahme entfernt nur den zuvor angelegten leeren
  Galerieeintrag.
- Ziel-URI und laufender Ersetzungsmodus werden über Activity-Neuerstellungen
  hinweg gespeichert.

## Android-16-Erkenntnis

Ein mit `MediaStore.IS_PENDING = 1` markiertes Bild kann unter Android 16 nur
von seinem Eigentümer geöffnet werden. Die externe Systemkamera ist trotz
temporärer URI-Freigabe nicht dieser Eigentümer und kann beim Bestätigen der
Aufnahme abstürzen. Deshalb ist der Ziel-Eintrag während des Kameraaufrufs
bereits veröffentlicht; bei Abbruch wird er wieder entfernt. Zusätzlich prüft
Bill Check bei uneinheitlichen Kamera-Rückgabewerten, ob tatsächlich Bilddaten
geschrieben wurden.

