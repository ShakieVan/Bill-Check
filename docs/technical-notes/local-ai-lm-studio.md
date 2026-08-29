# Lokaler LM-Studio-Endpunkt und Vision-Benchmark

## Verbindung

Bill Check kann einen OpenAI-kompatiblen Endpunkt unabhängig von Gemini
vorbereiten und mit `GET /v1/models` prüfen. Der voreingestellte Endpunkt ist
`https://ai.replinator.de/v1`, das Modell `qwen3.8-27b-q8`. Basic- und
Bearer-Authentifizierung sind vorgesehen; das Geheimnis liegt verschlüsselt im
Android Keystore. Ein Verbindungstest über den öffentlichen HTTPS-Endpunkt war
auf dem Android-Emulator einschließlich Modellprüfung erfolgreich. Der
vollständige Adapter verwendet `POST /v1/chat/completions`, Vision-Inhalte als
Data-URL und ein striktes JSON-Schema. Qwen wird mit Temperatur null und ohne
Reasoning-Ausgabe aufgerufen.

Der Teststatus ist nur für die gerade sichtbaren Eingaben gültig. Sobald ein
Verbindungsfeld erneut fokussiert oder bearbeitet wird, verschwindet das alte
Ergebnis. So kann eine frühere Fehlermeldung nicht mit einem noch nicht
ausgelösten Wiederholungstest verwechselt werden.

## Privater Bildtest vom 29.08.2026

25 reale Bilder aus dem privaten Nachbar-Repository wurden ausschließlich über
`127.0.0.1` ausgewertet: 23 Einzelbelege sowie je eine Zwischen- und
Endrechnung. Qwen3.8 27B Q8 mit MTP lieferte 25/25 formal gültige strukturierte
Ergebnisse. Der Median lag bei 10,2 Sekunden; die Rechnungen benötigten 33,5
und 27,2 Sekunden.

Als lokale Gegenprobe diente Gemma 4 31B Q4. Beide Modelle fanden auf den
Rechnungen 15 und 11 Zeilen sowie die richtigen Gesamtsummen. Qwen las jedoch
die sichtbaren Beschreibungen, Beträge und Checknummern wesentlich
zuverlässiger. 22 gedruckte Hotelbelege ließen sich über Checknummer-Endung und
Betrag exakt den von Qwen erkannten Rechnungszeilen zuordnen. Der einzige nicht
zuordenbare Einzelbeleg war ein handschriftlicher Captain Order ohne gedruckte
Gesamtsumme.

## Folgerungen für den Adapter

- Qwen ist der bevorzugte lokale Vision-Provider.
- Betrag und Checknummer werden lokal strikt normalisiert; Präfixe wie `LE`
  oder `CHK` dürfen nicht ungeprüft in Domänenwerte gelangen.
- Ungültige Schreibweisen wie `20.-` bleiben sichtbare Vorschläge und werden
  nicht stillschweigend als sicherer Centbetrag übernommen.
- Ortsangaben bleiben wie alle KI-Werte korrigierbare Vorschläge. Im Test waren
  zwei konkrete Orte falsch und zwei wegen teilweiser Verdeckung leer.
- Für Rechnungsdaten im sichtbaren Format `TT.MM.JJ` verwendet der Prompt eine
  explizite, dokumentierte Jahrhundertregel. Der Emulator-Durchstich lieferte
  daraufhin vollständige, nicht mehr pauschal als mehrdeutig markierte Daten.
- Kein automatischer Fallback zu einem Cloudanbieter: Ein Wechsel zu Gemini
  erfordert eine ausdrückliche Nutzeraktion.

Dateinamen und extrahierte Inhalte bleiben ausschließlich im privaten
Testdaten-Repository.

## Emulator-Durchstich vom 29.08.2026

Die Debug-App wurde ausschließlich auf `emulator-5554` aktualisiert; ein
verbundenes Telefon wurde nicht verwendet. Die Providerauswahl stand auf dem
privaten Qwen-Server und nutzte den öffentlichen HTTPS-Endpunkt:

- Ein echter Einzelbeleg übernahm Ort, Checknummer, Datum, Gesamtbetrag und
  Einzelposten sichtbar korrekt als ungespeicherten Editorvorschlag.
- Die private Endrechnung lieferte erneut 11 Zeilen; Zeilenzahl und deklarierte
  Gesamtsumme stimmten mit dem privaten Referenzlauf überein.
- Der anschließende lokale Abgleich ordnete den im Emulator vorhandenen
  passenden Beleg zu und erzeugte eine deutsche Qwen-Zusammenfassung.
- Ein zu positives erstes Kurzfazit und eine unbelegte Ursachenbehauptung
  führten zu verschärften Regeln: Bei offenen oder unsicheren Posten darf das
  Gesamturteil nicht „korrekt“ oder „vollständig“ lauten, und Ursachen dürfen
  nur aus verifizierten Warnungen oder Einträgen stammen. Der Wiederholungstest
  erfüllte diese Vorgabe.
