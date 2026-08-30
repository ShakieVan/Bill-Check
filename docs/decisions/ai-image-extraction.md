# KI-gestützte Bildauswertung

Status: verbindlich

## Entscheidung

Jede KI-Auswertung hängt hinter der domäneneigenen Schnittstelle
`AiExtractionProvider`. Implementiert sind Gemini und ein privater,
OpenAI-kompatibler Server. Beleg- und Rechnungsdaten bleiben
providerunabhängige Kotlin-Typen; ein Anbieterwechsel verändert deshalb weder
Room noch die Editoren.

Ein eigener, OpenAI-kompatibler Server kann unabhängig davon in den
Einstellungen vorbereitet und über `GET /v1/models` geprüft werden. URL,
Modell, Authentifizierungsart und Benutzername liegen in privaten
App-Einstellungen; Kennwort oder Bearer-Token werden wie Cloud-API-Schlüssel
mit dem Android Keystore verschlüsselt. Der Verbindungstest sendet keine
Belegbilder. Der Nutzer wählt ausdrücklich zwischen dem privaten Server und
Gemini; ein automatischer Cloud-Fallback findet nicht statt.

Der ausgewählte Anbieter erhält das Bild ausschließlich nach der ausdrücklichen
Aktion „Bild auswerten“. In einem neuen, seit dem Öffnen unveränderten Beleg
werden die besten Treffer direkt in den Editor übernommen. Bei bestehenden
oder zwischenzeitlich bearbeiteten Belegen bleibt das Ergebnis zunächst als
oranger, feldnaher Vorschlag sichtbar und wird über einen direkt unter den
Bildaktionen liegenden Review-Bereich angenommen oder verworfen. Dieser
Bereich darf nur nach einer im gerade geöffneten Editor gestarteten Auswertung
erscheinen; ein älteres Provider-Ergebnis löst ihn nicht erneut aus. Das
Ergebnis wird niemals unmittelbar in Room gespeichert.

Belegantworten trennen gedruckte Mengen von Artikelbezeichnungen und liefern
für fachliche Felder bis zu drei bildgestützte Kandidaten mit Evidenz,
Sicherheit und grober Position. Vorschlagsmenüs liegen außerhalb des
editierbaren Textfeldes, damit Cursor und Textmarkierung bis zum letzten Zeichen
frei bleiben. Während einer laufenden Analyse geänderte Nutzerwerte werden bei
der Antwort und bei einer späteren Sammelübernahme nicht überschrieben.
Manuell gewählte Alternativkandidaten gelten für das jeweilige Feld als
entschieden. Strukturelle Änderungen an einer Postenliste erfordern eine
eigene ausdrückliche Entscheidung; ein unbemerkter Listentausch findet nicht
statt. Aktuelle Posten ohne positionsgleichen KI-Posten werden orange als bei
der Listenübernahme entfallend gekennzeichnet.

Kopfwerte und Posten sind zwei gleichzeitige, voneinander unabhängige
Übernahmeaktionen. Sie dürfen in beliebiger Reihenfolge ausgeführt oder durch
Abbrechen beziehungsweise Speichern beide ignoriert werden; eigene
„Behalten“-Schaltflächen sind deshalb nicht nötig.

Die erwartete Währung ist nur Kontext und niemals Ersatz für sichtbare
Währungsevidenz. Das Modell liefert bei fehlender oder uneindeutiger Evidenz
einen leeren Code. Der Editor zeigt eine erkannte Währung auch dann orange an,
wenn sie der aktuellen Auswahl entspricht, und benennt ein leeres oder nicht
nutzbares Ergebnis ausdrücklich.

Ein sichtbares KI-Transkript kann mit der lokalen ML-Kit-Zeichengeometrie
fusioniert werden. Diese Hybridlage unterstützt die manuelle Auswahl direkt im
Bild, ist aber keine automatische Wahrheit: KI-Positionen und proportional
ergänzte Zeichen bleiben Näherungen.

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

