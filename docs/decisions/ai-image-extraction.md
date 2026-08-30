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

Datum und Uhrzeit sind getrennte KI-Felder. Die Uhrzeit wird nur aus einer
sichtbar gedruckten Angabe übernommen, als `HH:mm` normalisiert und niemals aus
Foto-Metadaten, aktueller Gerätezeit oder Öffnungszeiten abgeleitet. Im Editor
bleibt sie separat änderbar und wird gemeinsam mit dem Datum im vorhandenen
Belegzeitstempel gespeichert; deshalb ist keine zusätzliche Room-Spalte nötig.

Kopfwerte und Posten sind zwei gleichzeitige, voneinander unabhängige
Übernahmeaktionen. Sie dürfen in beliebiger Reihenfolge ausgeführt oder durch
Abbrechen beziehungsweise Speichern beide ignoriert werden; eigene
„Behalten“-Schaltflächen sind deshalb nicht nötig.

Die erwartete Währung ist nur Kontext und niemals Ersatz für sichtbare
Währungsevidenz. Das Modell liefert bei fehlender oder uneindeutiger Evidenz
einen leeren Code. Der Editor zeigt eine erkannte Währung auch dann orange an,
wenn sie der aktuellen Auswahl entspricht, und benennt ein leeres oder nicht
nutzbares Ergebnis ausdrücklich.

Der fachliche Extraktionslauf enthält bewusst kein Volltranskript. Erst die
separate Nutzeraktion „Text im Bild auswählen“ fordert ein räumliches
KI-Transkript an und fusioniert es mit der lokalen ML-Kit-Zeichengeometrie.
Diese Trennung verkürzt und stabilisiert die normale Belegauswertung. Die
Hybridlage unterstützt die manuelle Auswahl direkt im Bild, ist aber keine
automatische Wahrheit: KI-Positionen und proportional ergänzte Zeichen bleiben
Näherungen.

Eine lokale Plausibilitätsprüfung ersetzt niemals die gedruckte Gesamtsumme
durch die Summe erkannter Posten, weil Steuern, Service, Rabatt oder
unvollständig erkannte Zeilen legitime Abweichungen verursachen. Entspricht die
vorgeschlagene Gesamtsumme jedoch auffällig genau einem von mindestens zwei
Posten, wird nur dieser Betrag von der automatischen Übernahme ausgenommen und
im Editor ausdrücklich zur Prüfung markiert.

Die Stapelverarbeitung verwendet denselben providerunabhängigen
Extraktionsvertrag, speichert ihre Ergebnisse aber ohne vorgeschalteten Editor
direkt als Belege. Provideraufrufe werden appweit serialisiert; insbesondere
laufen Stapel, Einzelbeleg, Rechnungsanalyse und räumliches Transkript nicht
gleichzeitig gegen einen lokalen Qwen-Server. Eine persistente Room-
Warteschlange sichert Reihenfolge, Einzelwiederholung und Wiederaufnahme beim
nächsten App-Start. Die Warteschlangen-ID ist zugleich die ID des erzeugten
Belegs, sodass ein Absturz zwischen Beleg- und Statusspeicherung keinen
doppelten Datensatz erzeugt.

Automatische Übernahme bedeutet im Stapel nicht ungeprüfte Gewissheit. Fehlende
Pflichtangaben, ungültige Zeitangaben, Kandidatenkonflikte, eine auffällige
Gesamtsumme, unvollständige Posten, eine mögliche Dublette sowie eine fehlende
oder in der Reise nicht eingerichtete Währung werden als Review-Gründe am
Beleg gespeichert. Solche Belege bleiben vollständig editierbar und werden in
der Belegliste orange markiert. Eine manuelle Speicherung bestätigt den
geprüften Zustand.

Auch sprachliche Abgleichsfazits erhalten ausschließlich lokal vorbereitete
Fakten. Belegdaten werden deterministisch als anzeigefertiges Kalenderdatum der
Ausgabesprache formatiert; Unix-Zeitstempel werden dem Sprachmodell weder zur
Umrechnung noch zur Darstellung überlassen. Das Modell darf ein solches Datum
nur sprachlich einbetten, nicht neu berechnen oder umformatieren.

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
- Gedruckte Belegzeiten werden als `HH:mm` im 24-Stunden-Format geliefert;
  ohne sichtbare Zeit bleibt das Feld leer.
- Rechnungsbilder liefern einzelne Belastungszeilen, keine Überschriften,
  Zahlungen, Zwischen- oder Endsummen.

## Modelle und Kontingent

Die Einstellungen fragen über die offizielle Models-API nur tatsächlich für
den Schlüssel verfügbare `generateContent`-Modelle ab und zeigen deren
Kontextgrenzen. Googles API stellt über einen API-Schlüssel keinen verlässlichen
Restkontingentwert bereit. Die App verlinkt deshalb auf die offizielle
Rate-Limit-/Nutzungsansicht von Google AI Studio, statt einen Wert vorzutäuschen.

