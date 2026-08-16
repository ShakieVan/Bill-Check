# KI-gestützte Bildauswertung

Status: verbindlich

## Entscheidung

Cloud-Auswertung hängt hinter der domäneneigenen Schnittstelle
`AiExtractionProvider`. Gemini ist der erste Adapter; Beleg- und
Rechnungsdaten bleiben providerunabhängige Kotlin-Typen. Ein Wechsel oder eine
Ergänzung um weitere Anbieter verändert deshalb weder Room noch die Editoren.

Gemini erhält das Bild ausschließlich nach der ausdrücklichen Aktion „Bild
auswerten“. Das Ergebnis wird als korrigierbarer Vorschlag in den Editor
kopiert, mit einem stehenbleibenden Ergebnisdialog bestätigt und nie
unmittelbar in Room gespeichert.

API-Schlüssel liegen verschlüsselt mit einem nicht exportierbaren AES/GCM-
Schlüssel im Android Keystore. Klartextschlüssel gehören weder in Ressourcen,
BuildConfig, Logs, Backups noch in Git.

Die Auswertung sendet das Originalbild, solange es unter der Inline-Grenze
liegt. Nur übergroße Dateien werden auf höchstens 3.072 Pixel Kantenlänge und
JPEG-Qualität 94 verkleinert. Strukturierte JSON-Ausgabe begrenzt das Ergebnis
auf die erwarteten Beleg- oder Rechnungsfelder.

## Fachliche Extraktionsregeln

- `location` enthält nur den konkreten Bewirtungsort, beispielsweise
  „Sunset Lobby“ oder „Beach Restaurant“ – niemals Hotel-/Resortname, Stadt,
  Land oder Adresse.
- Zimmernummer, Unterschrift und handschriftliches Trinkgeld werden ignoriert.
- Beträge sind Dezimalstrings ohne Währungssymbol; fehlende Werte bleiben
  leer und dürfen nicht erfunden werden.
- Rechnungsbilder liefern einzelne Belastungszeilen, keine Überschriften,
  Zahlungen, Zwischen- oder Endsummen.

## Modelle und Kontingent

Die Einstellungen fragen über die offizielle Models-API nur tatsächlich für
den Schlüssel verfügbare `generateContent`-Modelle ab und zeigen deren
Kontextgrenzen. Googles API stellt über einen API-Schlüssel keinen verlässlichen
Restkontingentwert bereit. Die App verlinkt deshalb auf die offizielle
Rate-Limit-/Nutzungsansicht von Google AI Studio, statt einen Wert vorzutäuschen.

