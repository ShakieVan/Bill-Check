# Entwicklungsprotokoll

## 31.08.2026 – Heimatwährungsbeträge und kompakte Abgleichsposten

- Die Summe erkannter Rechnungszeilen und die Summe zugeordneter Belege
  zusätzlich näherungsweise in der Heimatwährung dargestellt. Die Umrechnung
  nutzt ausschließlich die aktuell in der Reise hinterlegten Kurse,
  kennzeichnet das Ergebnis mit `≈` und erklärt die Kursgrundlage direkt unter
  den Kennzahlen.
- Jede fremdwährungsbasierte Rechnungszeile um eine eigene, optisch abgesetzte
  Heimatwährungszeile ergänzt. Bei gleicher Heimatwährung wird kein doppelter
  Betrag gezeigt; bei fehlendem oder ungültigem Kurs erscheint kein
  irreführender Teilwert.
- Die dauerhaft sichtbaren Bearbeiten-, Löschen-, Akzeptieren- und
  Zuordnungselemente aus den Rechnungszeilen entfernt. Ein zustandsabhängiges
  `…`-Menü bietet nun Bearbeiten, Zuordnen oder Ändern, Lösen,
  Akzeptieren beziehungsweise Zurücknehmen und Löschen ohne Funktionsverlust.
- Compose-Instrumentierung um die sichtbare Heimatwährungsumrechnung sowie das
  zunächst geschlossene und anschließend vollständig bedienbare Optionsmenü
  erweitert; der reale Abgleich mit 15 Rechnungszeilen wurde im
  Android-16-Arbeitsemulator visuell gegengeprüft.

## 30.08.2026 – Responsive Belegkarten und bestätigtes Swipe-Löschen

- Die bisher konkurrierenden Bild-, Text-, Preis- und Löschspalten in drei
  klare vertikale Bereiche aufgeteilt: Bild mit Kopfdaten, kompakte
  Gesamtsumme und Postenliste. Lange Orts- und Postentexte erhalten dadurch
  die verfügbare Kartenbreite, während Beträge einzeilig rechts stehen.
- Die Rechnungsdaten hinter der mittig dargestellten, aufgerundeten
  Heimatwährungssumme aufklappbar gemacht. Fremdwährung, Kurse, Trinkgeld und
  centgenauer Heimatbetrag erscheinen erst auf Wunsch.
- Zwei Posten bleiben stets sichtbar; zusätzliche Posten lassen sich über eine
  kurz zweimal pulsierende Aktion ein- und ausklappen. Eine verschachtelte
  Scrollfläche innerhalb der Dashboardliste wurde bewusst vermieden.
- End-to-start-Wischen legt eine vollhohe rote Löschaktion frei. Wischen allein
  verändert keine Daten; erst Aktion und Bestätigungsdialog löschen den Beleg,
  das Galerieoriginal bleibt erhalten. Scrolling und das Antippen einer
  anderen Karte schließen die offene Aktion, und der Belegeditor bietet einen
  gleichwertigen barrierearmen Löschweg.
- Die Interaktionsflächen getrennt, damit Preiszeile, Bildminiatur,
  Belegbearbeitung und Postenumschalter einander nicht abfangen. Vier neue
  Compose-Instrumentierungstests sichern Gliederung, Detailaufklappen,
  Postenbegrenzung und den zweistufigen Löschablauf.

## 30.08.2026 – Deterministische Datumswerte im KI-Abgleichsfazit

- Einen reproduzierten Widerspruch beseitigt, bei dem das lokale Kurzfazit den
  20.12.2024 korrekt zeigte, Qwen denselben rohen Unix-Zeitstempel aber als
  07.12.2024 wiedergab.
- Die Faktenaufbereitung für lokale KI und Gemini vereinheitlicht. Beide
  Anbieter erhalten nun ein anzeigefertiges `occurredOn` (`20.12.2024` auf
  Deutsch) und niemals den internen Millisekundenwert.
- Der Prompt verbietet eine erneute Datumsumrechnung; ein Regressionstest
  prüft den konkreten 20.12.2024-Fall sowie die Abwesenheit von `occurredAt`.

## 30.08.2026 – Persistente Stapelverarbeitung für Belegbilder

- In der aktiven Reise den Einzelbutton zu „Bild“ verkürzt, daneben
  „Mehrere Bilder“ mit echter Mehrfachauswahl ergänzt und „Manuell erfassen“
  in eine eigene Zeile verschoben.
- Ausgewählte Bilder dauerhaft nach `Pictures/Bill Check` importiert und in
  einer Room-Warteschlange nacheinander über den gewählten KI-Anbieter
  verarbeitet. Alle KI-Aufträge sind appweit serialisiert, damit LM Studio/Qwen
  nicht durch konkurrierende Anfragen überlastet wird.
- Erfolgreiche Extraktionen erzeugen sofort normale Belege. Unklare Daten,
  mögliche Dubletten und fehlende oder nicht eingerichtete Währungen werden
  als orange Review-Fälle gespeichert, ohne den gesamten Stapel anzuhalten.
- Fortschritt, Abbruch und Einzelwiederholung persistiert; stabile IDs
  verhindern doppelte Belege bei einer Wiederaufnahme nach Prozessabbruch.
- Die zuvor fehlende Migration 5→6 verlustfrei nachgezogen, Migration 6→7 für
  die Warteschlange ergänzt und den destruktiven Room-Fallback entfernt. Der
  komplette Pfad wurde auf einem separaten Android-16-Test-AVD geprüft; danach
  blieb beim Update des Arbeits-Emulators dessen vorhandener Bestand erhalten.

## 30.08.2026 – Schnellere KI-Auswertung und Systemgalerie

- Fachliche Belegextraktion und räumliches Volltranskript in zwei unabhängige
  KI-Aufträge getrennt; das Transkript läuft nur noch bei „Text im Bild
  auswählen“ und bleibt mit der lokalen Zeichengeometrie kombinierbar.
- Verspätete OCR-/Transkriptantworten gegen Bild- und Editorwechsel abgesichert;
  bei langsamer KI ist ein bewusster lokaler Sofortstart möglich.
- Konservative Summenplausibilität ergänzt: Eine mutmaßlich aus einem einzelnen
  Posten übernommene Gesamtsumme wird nicht automatisch eingetragen, aber auch
  niemals still durch eine berechnete Summe ersetzt.
- Primäre Auswahl auf den System-Galeriefluss umgestellt. Temporäre Auswahl-URIs
  werden robust in `Pictures/Bill Check` importiert; verworfene Kopien werden
  wieder entfernt.
- Der getrennte Extraktionsvertrag wurde gegen 23 private Einzelbelege geprüft:
  keine unvollständige Antwort, alle separat gedruckten kanonischen Summen wie
  in der Referenz und eine Medianlaufzeit von 24,9 statt zuvor 49,5 Sekunden.
  Belegnamen, Bilder und Rohantworten bleiben im privaten Daten-Repository.

## 30.08.2026 – Feldnahe KI-Ergebnisprüfung

- Die bisherige allgemeine Konfliktkarte durch einen umbruchsicheren
  Review-Bereich mit „Auswertung fertig“ und vollbreiten Schaltflächen ersetzt.
  Er liegt direkt unter „Text im Bild auswählen“ und verwendet zusammen mit
  den feldnahen Vorschlägen eine klar zuordenbare orange Akzentfarbe.
- Potenziell abweichende Kopf- und Postenwerte stehen in derselben Akzentfarbe
  ohne platzraubendes Präfix direkt unter ihrem Zielfeld. Ein Tipp übernimmt
  nur diesen Wert; die bisherigen Mehrfachvorschläge bleiben parallel nutzbar.
- Neue, unberührte Belege übernehmen den besten Treffer automatisch.
  Bestehende oder während der Analyse bearbeitete Belege behalten ihre Werte;
  geänderte beziehungsweise bewusst gewählte Felder sind auch vor einer
  späteren Sammelübernahme geschützt.
- Bei konkurrierenden Postenlisten folgt eine eigene Entscheidung. Geschützte
  Namen und Beträge bleiben selbst bei der ausdrücklichen Listenübernahme
  erhalten; Hinzufügen oder Löschen eines Postens beendet die unsichere
  positionsbasierte Zusammenführung. Posten, die bei einer Übernahme entfallen
  würden, werden vorher orange und ausdrücklich als solche gekennzeichnet.
- Ein im ViewModel verbliebenes älteres KI-Ergebnis öffnet beim erneuten
  Bearbeiten keinen Review mehr. Nur eine im aktuellen Editor gestartete
  Analyse darf neue Vorschläge und Aktionen einblenden.
- „Erkannte Werte übernehmen“ und „Erkannte Posten übernehmen“ stehen nun
  gleichzeitig zur Verfügung und funktionieren unabhängig voneinander; die
  redundanten „Meine … behalten“-Schaltflächen entfallen.
- Die bisher unsichtbare Bestätigung einer mit der Auswahl übereinstimmenden
  Währung wird orange eingeblendet. Der KI-Prompt darf die erwartete Währung
  nicht mehr ohne Bildbeleg als Fallback ausgeben; fehlende, unbekannte oder
  für die Reise nicht verfügbare Währungsergebnisse werden ausdrücklich
  benannt.
- Die destruktive Aktion „Reise löschen“ aus der gemeinsamen Zeile mit
  „Abbrechen/Speichern“ gelöst, damit keine Beschriftung mehr schmal umbricht.

## 29.08.2026 – Generisches Mehrwährungsmodell

- Heimatwährung als Vorgabe in den Einstellungen und unveränderlicher Snapshot
  pro Reise eingeführt.
- Mehrere Reisewährungen mit festem oder täglichem Kurs, Standardauswahl und
  Schutz bereits verwendeter Währungen umgesetzt.
- Belegbetrag und Trinkgeld besitzen getrennte Kurs-Snapshots; JPY, KWD und
  andere ISO-Nachkommastellen werden ohne feste Cent-Annahme verarbeitet.
- Belegkarten und PDF-Berichte weisen tatsächliches Trinkgeld einschließlich
  seines unabhängigen Kurses aus. Null-Trinkgeld wird als Heimatwährung/Kurs 1
  kanonisiert und sperrt dadurch keine unbenutzte Reisewährung.
- Offline-Suchdialog nach Kürzel, Name und Land/Region in Reise-, Beleg- und
  Rechnungsabläufe integriert; KI-Abweichungen werden sichtbar behandelt.
- Room-Schema 6, Backup/CSV 2, PDF und Widget auf die Reise-Heimatwährung
  umgestellt. Der nicht produktive Entwicklungsdatenbestand wird beim Upgrade
  bewusst verworfen.
- Reisen lassen sich im Bearbeitungsdialog nach einer Bestätigung vollständig
  löschen. Room entfernt Belege, Posten und Abgleiche kaskadierend; unabhängige
  Galerieoriginale werden ausdrücklich nicht gelöscht.

## 29.08.2026 – Mengen, Feldkandidaten und räumliche Textauswahl

- Belegantworten um getrennte Mengen, maximal drei bildgestützte Kandidaten je
  Kopf- und Postenfeld sowie vollständige Transkriptzeilen erweitert.
- Mengen werden im Postentext als `5 × Bezeichnung` sichtbar; bereits vom
  Modell wiederholte Mengen werden ohne Verfälschung von Namen wie `500ml`
  normalisiert.
- „Früher verwendet“ und KI-Vorschläge liegen in einem Menü unterhalb des
  Textfeldes. Der Auslöser belegt keinen Teil der editierbaren Zeile mehr.
- Spät eintreffende KI-Ergebnisse überschreiben zwischenzeitliche Eingaben
  nicht. Kandidaten und eine explizite Übernahme der KI-Posten bleiben verfügbar.
- Das bestätigte KI-Ergebnis bleibt für die Lebensdauer des Belegeditors erhalten,
  damit Vorschlagsmenüs und KI-Transkript nach dem Schließen des Ergebnisdialogs
  nicht verschwinden.
- Die alte OCR-Wortfetzenliste durch eine zoombare, bildgebundene Wort- und
  Zeichenauswahl ersetzt. ML Kit liefert die Geometrie; KI-Transkripte können
  besseren Text daran ausrichten. Bei leerem lokalen-Qwen-Transkript erfolgt
  ein separater OCR-Fallback-Durchlauf.
- Die Anfangs- und Endgriffe der Auswahl lassen sich wie bei der
  Android-Textauswahl direkt verschieben. Während des Ziehens zeigt eine Lupe
  die Zeichen unter dem Finger; die Auswahl bleibt weiterhin zeichengenau.
- Im Rechnungsabgleich pulsiert ausschließlich der sinnvolle nächste Schritt
  dezent: nach einem gewählten, noch nicht ausgewerteten Bild „Bild auswerten“,
  nach extrahierten Zeilen „Abgleich ausführen“. Ein lokaler Abgleich wird auch
  ohne optionale KI-Zusammenfassung dauerhaft als ausgeführt markiert, sodass
  die Aufforderung danach endet.
- Private Problembelege #5595 und #783 bleiben außerhalb des Repositorys. Im
  Emulator reproduzierte #5595 den schwachen lokalen Restauranttext und führte
  zur verpflichtenden Hybrid-/Fallback-Strategie.

## 29.08.2026 – Verbindungstest für lokalen KI-Server

- Die Einstellungen um eine getrennte Konfiguration für den privaten,
  OpenAI-kompatiblen KI-Endpunkt ergänzt. Voreingestellt sind
  `https://ai.replinator.de/v1` und `qwen3.8-27b-q8`.
- Basic-Authentifizierung und Bearer-Token werden unterstützt; das jeweilige
  Geheimnis wird mit einem eigenen Android-Keystore-Schlüssel verschlüsselt.
- Der Verbindungstest ruft ausschließlich `/v1/models` auf, misst die
  Antwortzeit und prüft, ob das konfigurierte Modell tatsächlich geladen ist.
  Ein altes Erfolgs- oder Fehlerergebnis verschwindet beim erneuten Fokussieren
  oder Bearbeiten der Verbindungsfelder, damit es nicht als Ergebnis einer noch
  nicht gestarteten Wiederholungsprüfung missverstanden wird.
- Den lokalen Vision-Durchstich mit 25 privaten Bildern vollständig wiederholt
  und gegen Gemma 4 31B Q4 sowie die Originale geprüft. Qwen3.8 27B Q8 lieferte
  25/25 gültige JSON-Ergebnisse; 22 gedruckte Einzelbelege passten anhand von
  Checknummer-Endung und Betrag exakt zu den 26 erkannten Rechnungszeilen.
  Dauerhafte Folgerungen zu Normalisierung, Ortsfehlern und zu konservativer
  Datumsbewertung stehen in `technical-notes/local-ai-lm-studio.md`.
  Gemini blieb bis zum unmittelbar folgenden vollständigen LM-Studio-Adapter
  der aktive Bildauswertungsanbieter.
- Den vollständigen OpenAI-kompatiblen Vision-Adapter ergänzt. In den
  Einstellungen ist der private Qwen-Server nun ausdrücklich neben Gemini als
  Auswertungsanbieter wählbar; es gibt keinen automatischen Cloud-Fallback.
- Lokale Antworten werden über ein striktes JSON-Schema begrenzt. Beträge,
  Währungscodes und bekannte Checknummer-Präfixe werden deterministisch
  normalisiert; unsichere Schreibweisen bleiben sichtbar und scheitern an der
  bestehenden fachlichen Validierung statt stillschweigend übernommen zu
  werden.
- Den vollständigen Ablauf ausschließlich auf `emulator-5554` geprüft: echter
  Einzelbeleg korrekt als Editorvorschlag, private Endrechnung mit denselben 11
  Zeilen und derselben Gesamtsumme wie im Referenzlauf sowie anschließender
  Abgleich mit lokaler deutscher Qwen-Zusammenfassung.
- Nach einem zu positiven ersten KI-Fazit den Zusammenfassungs-Prompt gegen
  unbelegte Ursachen und positive Gesamturteile bei offenen Abweichungen
  verschärft und den Emulatorlauf erfolgreich wiederholt.

## 16.08.2026 – Projektstart

- Bestehenden HTML-Prototyp und 25 reale Beispielbilder analysiert.
- Produktanforderungen für Reisen, Rundung, Trinkgeld, Bildablage,
  Zwischenrechnungen, Export und Updates geklärt.
- Öffentliches Repository `ShakieVan/Bill-Check` angelegt.
- Privates Repository `ShakieVan/Bill-Check-Data` angelegt und Originalbilder
  dorthin kopiert.
- Öffentliche Git-Grenzen vor dem ersten Commit eingerichtet.
- Android-16-/Compose-/Room-Fundament begonnen.
- Ersten nativen Durchstich auf einem Android-16-Emulator installiert und
  bedient: Reise anlegen, zwei Belege erfassen und lokale Daten beobachten.
- Rundungsfall im UI bestätigt: zweimal exakt 5,20 EUR erscheint je Beleg als
  6 EUR, die exakte Gesamtsumme 10,40 EUR jedoch korrekt als 11 EUR.

### Frühe Erkenntnisse

- Belege enthalten häufig nur verkürzte Checknummern, Hotelrechnungen dagegen
  aufgefüllte Nummern. Der spätere Matcher muss Nummern normalisieren.
- Eine Zwischenrechnung ist ein eigener Abgleichslauf und kein endgültiger
  Reiseabschluss.
- Galerieoriginal und App-Datensatz haben getrennte Lebenszyklen.

### Fehlversuch

- Der erste Kopierversuch verwendete `Copy-Item -LiteralPath` mit einem
  Wildcard-Pfad. `LiteralPath` expandiert Wildcards bewusst nicht. Die Dateien
  wurden anschließend sicher über `Get-ChildItem -LiteralPath` kopiert; alle
  25 Quelldateien blieben unverändert erhalten.
- Der erste Android-Build wendete bei AGP 9.2 zusätzlich das frühere
  `org.jetbrains.kotlin.android`-Plugin an. AGP 9 bringt Kotlin bereits
  eingebaut mit und registrierte deshalb die `kotlin`-Erweiterung doppelt.
  Das Projekt wurde direkt auf Built-in Kotlin und Room-KSP migriert, statt
  den nur vorübergehend verfügbaren Legacy-Opt-out zu verwenden.
- Der von AGP 9 mindestens akzeptierte ältere KSP-Build fügte generierte
  Quellen noch über die verbotene `kotlin.sourceSets`-DSL hinzu. KSP 2.3.10
  enthält die aktuelle Built-in-Kotlin-/AGP-9-Integration und ersetzt diesen
  Übergangsstand.
- Die neuesten AndroidX-Core-/Lifecycle-Artefakte verlangen bereits
  `compileSdk 37`. Weil Android 16/API 36 eine bewusste Produktgrenze ist,
  verwendet Bill Check die letzten stabilen API-36-kompatiblen Versionen,
  statt die Plattform unbemerkt anzuheben.
- Bei koordinatenbasierter Emulatorbedienung verschiebt die Bildschirmtastatur
  Compose-Dialoge nach oben. UI-Tests müssen deshalb semantische Selektoren
  statt fester Bildschirmkoordinaten verwenden.

## 16.08.2026 – Belegposten und Online-Kurse

- Manuelle Belege um beliebig viele benannte Einzelposten erweitert.
- Leerer Gesamtbetrag übernimmt die Postensumme; Abweichungen werden sichtbar
  gemacht, bleiben aber wegen möglicher Gebühren oder Rabatte zulässig.
- Posten werden atomar mit dem Beleg gespeichert und im Dashboard angezeigt.
- Schlüssellose EUR-Kursabfrage mit EGP-Unterstützung, Tagescache, Attribution
  und Anbieterabstraktion ergänzt.
- Feste und täglich aktualisierte Kurse sind pro Reise wählbar; jeder Beleg
  behält seinen konkreten Kurs-Snapshot.
- Datenbankschema 1→2 migriert und auf dem Emulator mit vorhandenen Belegen
  ohne Datenverlust verifiziert.
- GitHub Actions nach der Node-20-Abkündigung auf die aktuellen Hauptversionen
  von Checkout, Java-Setup und Gradle-Setup angehoben.

## 16.08.2026 – Kamera- und Galeriefluss

- Systemkamera schreibt Aufnahmen ohne App-Kameraberechtigung in das sichtbare
  Galeriealbum `Bill Check`.
- Android Photo Picker für vorhandene Bilder integriert; Importe bleiben an
  ihrem Ort und werden nicht dupliziert.
- Native Prüfansicht mit Neuaufnahme, anderer Auswahl, bewusster Bestätigung
  und Rückkehr ohne Verknüpfung ergänzt.
- Bild-URI wird gemeinsam mit dem Beleg gespeichert und als Miniatur in der
  Übersicht dargestellt.
- Verknüpfte Bilder lassen sich ersetzen oder entknüpfen; das Galerieoriginal
  bleibt dabei nachweislich erhalten.
- URI-Zustand gegen Activity-Neuerstellung abgesichert.

### Erkenntnis und behobener Fehlversuch

- Ein zunächst verwendeter `IS_PENDING`-MediaStore-Eintrag ließ sich auf
  Android 16 von der externen Systemkamera beim Bestätigen nicht erneut öffnen.
  Der Emulator reproduzierte die Eigentümerprüfung bis zum Kameraprozess-
  Absturz. Bill Check veröffentlicht den leeren Ziel-Eintrag daher vor dem
  Kameraaufruf und entfernt ihn bei Abbruch; Aufnahme, Prüfansicht und
  Galerieerhalt wurden anschließend vollständig durchgespielt.

## 16.08.2026 – UX-Pass nach S24-Ultra-Test

- Externe Systemkamera auf einem Galaxy S24 Ultra bestätigt und die erwogene
  CameraX-Eigenentwicklung bewusst verworfen.
- Ausgeschalteten Tageskurs-Schalter mit farbigem Bedienelement, kompletter
  klickbarer Zeile und explizitem Status „Ein/Aus“ eindeutig gemacht.
- Technische Beschriftung „Ohne Verknüpfung zurück“ durch „Abbrechen“ ersetzt.
- Belegerfassung in einen nahezu bildschirmhohen Editor mit festem Kopf,
  festem Aktionsbereich und unabhängig scrollbar gestalteter Feldfläche umgebaut.
- Vorhandene Belege einschließlich Einzelposten und Trinkgeldwahl editierbar
  gemacht; das Bild bleibt beim Bearbeiten erhalten.
- Große Reise-Drop-down-Box aus der Übersicht entfernt. Reisen und „Neue
  Reise“ liegen nun im Burgermenü, der vorbereitete Einstellungseinstieg im
  Dreipunktmenü.

## 16.08.2026 – Reisebearbeitung und Darstellung

- Jede Reise im Burgermenü um ein klar zugängliches Stift-Symbol ergänzt.
- Gemeinsamen Editor für neue und bestehende Reisen geschaffen; Name,
  Fremdwährung, Offline-Kurs, Tageskurswahl und Trinkgeldvorgaben sind
  bearbeitbar.
- Historische Belegdaten bleiben bei geänderten Reisevorgaben unverändert.
- Den Einstellungsplatzhalter durch eine persistente Hell-/Dunkel-Auswahl
  ersetzt. Nur beim ersten Start dient die Systemdarstellung als Anfangswert.
- Reise- und Belegeditor auf eine gemeinsame tastaturfeste Dialogbasis mit
  festem Kopf, frei scrollbarer Feldfläche und festem Aktionsbereich
  vereinheitlicht.
- Reiseänderung, unveränderte Bestandsbelege, Scrollen bei sichtbarer
  Bildschirmtastatur sowie beide Darstellungsmodi im Emulator geprüft.

## 16.08.2026 – Bildverwaltung im Belegeditor

- Bildbereich in den Editor neuer und bestehender Belege integriert.
- Fehlende Bilder lassen sich dort per Systemkamera oder Photo Picker
  hinzufügen; vorhandene Bilder können geprüft, ersetzt oder entknüpft werden.
- Bildprüfungszustand und noch ungespeichertes Belegbild getrennt, damit die
  Prüfansicht eindeutig bestätigt oder abgebrochen werden kann.
- Editorzustand während Kamera, Photo Picker und Prüfansicht lebendig gehalten;
  noch nicht gespeicherte Feld- und Postenänderungen gehen nicht verloren.
- Neutralen Einstieg „Bild auswerten“ für die spätere KI-/OCR-Pipeline ergänzt und
  den noch ausstehenden Erkennungsbaustein transparent erläutert.
- Hinzufügen, Rückkehr mit erhaltenem Textentwurf, erneute Prüfansicht,
  Entknüpfen und Wiederherstellung der Testdaten im Emulator durchgespielt.

## 16.08.2026 – Ordnerzugang, Reisesortierung und Textverlauf

- Ergänzend zum Photo Picker einen System-Dateidialog eingebaut, der nach
  Möglichkeit direkt in `DCIM/Bill Check` startet.
- Die 25 privaten Beispielbilder tatsächlich per ADB in diesen Emulatorordner
  kopiert und einzeln indexiert; der Dateidialog zeigt den Ordner und seine
  Bilder nun unmittelbar an.
- Reisen mit eigenem Drag-Griff versehen und die neue Reihenfolge atomar sowie
  lückenlos in Room speicherbar gemacht.
- Zuletzt verwendete eindeutige Orts- und Postenbezeichnungen pro Reise als
  reaktive Room-Abfragen ergänzt.
- Verlaufssymbol und Auswahlmenü ausschließlich an den beiden gewünschten
  Textfeldern eingebaut; Betrag und Checknummer bleiben unverändert.
- Ordnerauswahl bis zur Bildprüfansicht sowie beide Vorschlagsmenüs im
  Android-16-Emulator geprüft.

## 16.08.2026 – Zwischen- und Endrechnungsabgleich

- Unabhängige Abgleichsläufe pro Zwischen- oder Endrechnung mit optionalem
  Rechnungsbild und beliebig vielen editierbaren Rechnungszeilen ergänzt.
- Checknummern für den Vergleich normalisiert, damit insbesondere führende
  Nullen und Trennzeichen auf der Hotelrechnung nicht stören.
- Automatischen sicheren Treffer auf identische Checknummer, Währung und
  Betrag begrenzt; schwächere Kandidaten werden nach Checknummer, Betrag,
  Datum und Ort für die manuelle Auswahl gerankt.
- Datenbankseitige 1:1-Zuordnung eingeführt. Bereits in einem anderen Lauf
  zugeordnete Belege werden nicht erneut angeboten oder automatisch benutzt.
- Statusdarstellung in Grün (korrekt/akzeptiert), Gelb (unsicher), Orange
  (Betragsabweichung) und Rot (nicht gefunden) umgesetzt.
- Manuelles Zuordnen, Ändern und Lösen sowie „bekannt/akzeptiert“, komplettes
  Zurücksetzen, erneuter Abgleich und kaskadierendes Löschen eines Laufs
  ergänzt.
- Room-Schema 2→3 migriert und Matcher mit Unit-Tests abgesichert.
- Den kompletten Bedienweg im Android-16-Emulator mit Bestandsbelegen geprüft:
  Lauf und Zeile anlegen, Kandidatenranking, unsichere Zuordnung, Reset,
  Akzeptieren und bestätigtes Löschen.

## 16.08.2026 – Gemini-Auswertung und lokale OCR

- Providerunabhängige KI-Domänenschnittstelle und ersten Gemini-Adapter für
  Belege sowie Zwischen-/Endrechnungen ergänzt.
- Strukturierte JSON-Ausgabe für Ort, Checknummer, Gesamtbetrag, Datum,
  Einzelposten beziehungsweise Rechnungszeilen erzwungen.
- Originalbild bis zur Inline-Grenze erhalten; nur übergroße Bilder werden
  hochwertig und begrenzt verkleinert.
- API-Schlüssel per AES/GCM mit Android Keystore verschlüsselt und eine
  Löschaktion in den Einstellungen eingebaut.
- Verfügbare Gemini-Modelle samt Kontextgrenzen live abgefragt. Da die API kein
  Restkontingent ausgibt, die offizielle AI-Studio-Nutzungsansicht verlinkt.
- Ergebnisdialog bleibt bis „OK“ sichtbar; erkannte Daten landen nur als
  kontrollierbare Formularwerte und werden nicht automatisch gespeichert.
- Gebündelte ML-Kit-OCR als vollständig lokale Bausteinhilfe ergänzt. Zielfeld
  und zu übernehmende Wörter/Beträge werden bewusst manuell gewählt.
- Cloud-Durchstich mit genau einer Gemini-2.5-Flash-Auswertung eines privaten
  Emulatorbilds verifiziert: Ort, Checknummer, Gesamtbetrag und Posten wurden
  strukturiert übernommen; Teständerungen nicht gespeichert und Schlüssel
  anschließend aus dem Emulator entfernt.
- Dabei erkannt, dass ein allgemeines Ortsfeld zusätzlich Hotel und Stadt
  enthielt. Prompt, Schema und Regressionstest verlangen nun ausschließlich
  den konkreten Restaurant-/Bar-/Lounge-Namen.

## 16.08.2026 – Selektiver Export und Import

- Versioniertes `.billcheck`-Vollarchiv mit Reisen, Belegen, Einzelposten,
  Bildern, Rechnungsabgleichen, Status und 1:1-Zuordnungen umgesetzt.
- CSV-Bericht als wieder importierbare Übersicht und PDF als druckbaren
  Nur-Lese-Bericht ergänzt; Einzelposten bleiben bewusst dem Vollarchiv
  vorbehalten.
- Vor Export und Import eine Mehrfachauswahl der Reisen eingebaut. Der Import
  zeigt vor dem Schreiben Name, Beleg- und Abgleichanzahl.
- Beim Import sämtliche IDs neu erzeugt und Referenzen konsistent abgebildet;
  doppelte Namen erhalten einen nummerierten „(Import)“-Zusatz.
- Wiederhergestellte Bilder sicher über MediaStore erneut im Album
  `DCIM/Bill Check` veröffentlicht.
- Vollständigen Emulator-Durchstich ausgeführt: zwei Reisen mit 2,2-MB-Archiv
  exportiert, genau eine ausgewählt, drei Belege und ein verknüpftes Bild
  wiederhergestellt und Galerieoriginal geprüft.
- Android DocumentsUI ergänzte bei `application/zip` trotz gewünschtem Namen
  ein `.zip`. Der Vollarchiv-Export verwendet deshalb
  `application/octet-stream`; der sichtbare Name bleibt exakt `.billcheck`.
- CSV-Quoting und Roundtrip einschließlich Semikolon, Anführungszeichen,
  historischen Kursen und manueller Zuordnung per Unit-Test abgesichert.

## 16.08.2026 – Homescreen-Widget

- Native Widget-Übersicht für die zuletzt gewählte Reise ergänzt: prominent
  aufgerundete Euro, centgenaue Summe und Belegzahl.
- Schnellaktionen für Kamera, vorhandenes Bild, manuellen Beleg und
  Rechnungsabgleich direkt mit den vorhandenen App-Abläufen verbunden.
- Hell-/Dunkelfarben, öffnende Gesamtfläche, Room-Hintergrundzugriff und
  Aktualisierung nach Daten- oder Reiseänderungen umgesetzt.
- Widget im Android-16-Launcher hinzugefügt und mit echten lokalen Reisedaten
  geprüft; alle vier Einstiege öffnen Kamera, Photo Picker, Belegeditor oder
  Abgleichsverwaltung wie vorgesehen.
- Unit-Tests und Android-Lint nach der Integration ohne Befund ausgeführt.

## 16.08.2026 – Signierte GitHub-Updates

- Updateverwaltung nach dem bewährten Tube-NEXT-Ablauf ergänzt: automatische
  24-Stunden-Prüfung beim Öffnen, manueller Reload, Release Notes, privater
  Download und bewusste Übergabe an den Android-Systeminstaller.
- Auswahl auf eine eindeutig benannte Universal-APK begrenzt und aktuelle
  GitHub-Asset-Digests eingelesen.
- Download über `.part`-Datei gehärtet; Dateigröße und SHA-256 werden vor der
  Installationsfreigabe zwingend geprüft. Manipulationsfall per JVM-Test
  abgesichert.
- Updateverwaltungsdialog mit festem Kopf/Abschluss, scrollbarem Inhalt und
  allen Zuständen für kein Release, aktuell, verfügbar, Download, Fehler und
  Installation umgesetzt.
- Eigenen Bill-Check-Produktionsschlüssel außerhalb des Repositories erzeugt,
  Recovery-Pfad dokumentiert und vier verschlüsselte GitHub-Secrets gesetzt.
- Tag-gesteuerten Release-Workflow für Unit-Tests, Lint, signierten
  Universal-Build, Prüfsummen, Signaturprüfung und GitHub-Release ergänzt.
- Reale GitHub-Abfrage im Emulator geprüft; der derzeit korrekte Zustand
  „noch kein öffentliches Release“ erscheint. Minifizierte, signierte
  Release-APK gebaut, Signatur/Metadaten geprüft und erfolgreich im
  Android-16-Emulator gestartet.

## 16.08.2026 – Abschließender Spezifikationsaudit

- Sämtliche Produktabschnitte gegen den implementierten Stand geprüft; die
  speicheroptimierte Bildablage bleibt wie spezifiziert eine ausdrücklich
  spätere Option, damit die aktuelle OCR-/KI-Qualität nicht beeinträchtigt
  wird.
- Veralteten Platzhaltertext zur noch nicht vorhandenen Bilderkennung
  entfernt und die Notiz für das erste öffentliche Release korrigiert.
- Deutsch und Englisch zusätzlich über Androids systemeigene App-Sprachauswahl
  veröffentlicht; die App folgt damit der gewählten Gerätesprache ohne eigene
  parallele Spracheinstellung.
- Bisher im Datenmodell bereits vorbereitete Reiseoption zum Vorauswählen des
  Standard-Trinkgelds vollständig in Anlage und Bearbeitung verdrahtet. Der
  vorgeschlagene Betrag bleibt 1 EUR, die Vorauswahl bleibt standardmäßig aus.
- Historische Beleg-Snapshots gehärtet: Eine spätere Änderung der
  Reisevorgaben überschreibt beim Editieren weder vorhandenes Trinkgeld noch
  dessen Währung; Editor und erneute Bildauswertung verwenden die damalige
  Belegwährung. Die drei Umschaltfälle sind als Regressionstests festgehalten.
- Den auf `product-spec.md` bezogenen Abschlussnachweis als eigene Audit-Matrix
  dokumentiert.
- Auf Wunsch des Projekteigentümers GPL Version 3 als `GPL-3.0-only`
  festgelegt und den kanonischen Lizenztext hinzugefügt. Private Testbilder
  bleiben ausdrücklich außerhalb des öffentlichen Lizenz-Repositories.
- Abschließenden lokalen Lauf mit 26 fehlerfreien Unit-Tests, erfolgreichem
  Android-Lint und minifiziertem Release-Build ausgeführt. Die signierte APK
  anschließend im Android-16-Emulator installiert, gestartet und ohne
  `AndroidRuntime`-Absturz kontrolliert.

## 16.08.2026 – Vollbildprüfung und OCR-Releasekorrektur

- Den vom Galaxy S24 Ultra geholten privaten Screenshot außerhalb des
  Repositorys ausgewertet. Er zeigte eine interne Nullreferenz der lokalen
  Texterkennung im minifizierten Release-Build.
- OCR-Bilder nun in hoher, speicherbegrenzter Auflösung über Androids
  `ImageDecoder` vorbereitet und Fehler mit vollständigem Stack protokolliert.
- Den Stack über das R8-Mapping bis zu einer fehlerhaft zusammengeführten
  internen ML-Kit-Telemetrieklasse zurückverfolgt und den betroffenen
  ML-Kit-Bereich durch gezielte Keep-Regeln geschützt.
- Fehler mit demselben privaten Beleg in der signierten Release-APK auf dem
  S24 Ultra reproduziert und nach der Korrektur erfolgreich gegengeprüft:
  Textbausteine und Zielfelder sichtbar, keine OCR-Fehlermeldung.
- Belegminiatur und Bildverwaltung getrennt: Miniatur öffnet nun eine
  bildschirmfüllende Ansicht mit 1–5-fachem Pinch-Zoom, Verschieben und
  Doppeltipp; „Bearbeiten“ führt weiterhin zu Ersetzen und Entknüpfen.

## 16.08.2026 – Cursorführung bei langen Texten

- Einen privaten Gerätescreenshot vom Galaxy S24 Ultra außerhalb des
  Repositorys ausgewertet. Die vollständige Ortsangabe reichte rechts über
  den sichtbaren Ausschnitt hinaus, während der Cursor am alten Einfügepunkt
  stehen blieb.
- Verlaufstextfelder auf explizite Text- und Auswahlverwaltung umgestellt.
  Ergänzungen aus lokaler Texthilfe und Verlauf setzen den Cursor nun an das
  neue Ende und scrollen die einzeilige Eingabe automatisch dorthin.
- Auf dem S24 Ultra mit geöffneter Tastatur gegengeprüft: Cursor zunächst
  absichtlich mitten im langen Text platziert, OCR-Baustein ergänzt und
  anschließend Cursor sowie sichtbaren Ausschnitt am neuen Textende geprüft.
  Der veränderte Testbeleg wurde verworfen.
- Beim anschließenden manuellen Ziehen des Cursor-Griffs zeigte sich eine
  weitere Compose-Grenze: Am horizontalen Feldrand scrollte der Text weder
  links noch rechts weiter. Verlaufstextfelder brechen lange Inhalte deshalb
  nun vollständig auf sichtbare Zeilen um und behalten auf der Tastatur die
  Aktion „Fertig“. Der gemeinsame Editor kann die gewachsene Feldhöhe vertikal
  scrollen.
- Die parallel installierbare Testvariante heißt im Launcher künftig eindeutig
  „Bill Check Debug“. Ihre getrennte App-ID schützt die Daten der regulären
  Installation; auf persönlichen Testgeräten wird nach Abschluss wieder nur
  die signierte Release-App belassen.

## 16.08.2026 – Einheitliche Bilddetailansicht

- Jede tatsächlich dargestellte Belegabbildung öffnet beim Antippen dieselbe
  bildschirmfüllende Ansicht mit Pinch-, Schwenk- und Doppeltipp-Gesten.
- Die Miniatur in der Belegliste führt nicht mehr in die Bildverwaltung. Der
  restliche Kartenbereich öffnet weiterhin den Belegeditor; nur dort werden
  Bilder ersetzt oder entknüpft.
- Das große Bild in „Bild prüfen“ ist ebenfalls antippbar. Der Hilfetext weist
  in Deutsch und Englisch auf die Vergrößerung hin.
- Alle drei Einstiege mit der signierten Release-App auf dem Galaxy S24 Ultra
  geprüft: Belegliste, Belegeditor und Bildprüfung öffnen die Detailansicht und
  kehren beim Schließen in ihren jeweiligen Ausgangszustand zurück.

## 17.08.2026 – Unscharfer Rechnungsabgleich

- Rechnungsbezeichnungen in Übersicht und Detailkopf ausdrücklich beschriftet
  und im Detail bearbeitbar gemacht.
- Den bisherigen Rangwert durch einen Prozentwert aus Checknummer, Betrag,
  Datum und Ort ersetzt. Checknummern und Ortsangaben tolerieren mit
  Levenshtein-Ähnlichkeit Schreibfehler und Abkürzungen.
- Vorangestellte Kassen-IDs werden über eine starke Endübereinstimmung der
  eigentlichen, von führenden Nullen bereinigten Checknummer abgefangen.
- Automatische Fuzzy-Zuordnungen an exakten Betrag, gleiche Währung, mindestens
  75 Punkte und einen Abstand von 10 Punkten zum nächsten Kandidaten gebunden.
  Präfix-, Tippfehler-, Fremd-ID- und Mehrdeutigkeitsfälle per JVM-Test
  abgesichert.
- Den realen Fall `0015512` auf der Rechnung gegen `5512` auf dem Beleg mit
  den privaten Originalbildern im Android-16-Emulator vollständig geprüft.
  Dabei fiel auf, dass das erkannte Belegdatum zwar geliefert, aber im Editor
  bisher weder angezeigt noch gespeichert wurde. Der Belegeditor enthält nun
  ein editierbares, streng validiertes Datum und übernimmt den KI-Vorschlag.
- Der reale Fall wird auch ohne vorhandenes Belegdatum mit 78 Punkten erkannt:
  starke Checknummer-Endung, centgenauer Betrag und ähnlicher Restaurantname.
  Exakter Betrag, gleiche Währung und der Eindeutigkeitsabstand bleiben dabei
  zwingende Schutzbedingungen.

## 17.08.2026 – Hybride Rechnungsprüfung

- Den im HTML-Prototyp vorhandenen freien KI-Analysetext mit der strukturierten
  Android-Rechnungsprüfung zusammengeführt, ohne Gemini zum verbindlichen
  Abgleichssystem zu machen.
- Gemini erhält Rechnungsbild und noch verfügbare Belege mit stabilen IDs und
  liefert je Rechnungszeile optional Beleg-ID, Konfidenz und Begründung.
  Unbekannte IDs, bereits verwendete Belege sowie abweichende Beträge oder
  Währungen werden vor dem Speichern lokal verworfen.
- Verbindliche Zuordnung und Vollständigkeit bleiben beim deterministischen
  Matcher. Rechnungszeilen ohne Beleg und freie Belege ohne Rechnungszeile
  erscheinen in einer lokal sortierten gemeinsamen Chronologie.
- Die lokale Diskrepanzübersicht mit Anzahlen und centgenauen Summen steht
  oberhalb der Einträge. Anschließend darf
  Gemini nur aus den bereits geprüften Fakten eine verständliche Zusammenfassung
  formulieren; sie wird am Abgleich gespeichert und nach Änderungen verworfen.
- Room-Schema 3→4 sowie Vollbackup, CSV und PDF um KI-Vorschläge und die
  gespeicherte Zusammenfassung erweitert.

## 17.08.2026 – Schonungslose Vollständigkeits- und Fallenprüfung

- Den gravierenden Bedeutungsfehler behoben, bei dem eine einzige erkannte und
  zugeordnete Rechnungszeile als „0 Rechnungszeilen ohne Beleg“ eine vollständige
  Rechnung suggerieren konnte. Gedruckte Kontrollsumme und Währung werden nun
  in Room 5 persistiert und lokal gegen die Summe aller erkannten Zeilen geprüft.
- Die echte Utopia-Endrechnung mit Gemini 3.6 Flash ohne Belegkontext geprüft:
  exakt 11 Zeilen, 7.404,20 EGP Kontrollsumme und sämtliche Checknummern wurden
  korrekt transkribiert. `0015512` wurde lokal dem Beleg `5512` zugeordnet; die
  übrigen zehn Rechnungszeilen bleiben ausdrücklich ohne Beleg sichtbar.
- Einen zweiten, synthetischen Gemini-Fallenlauf mit doppelter Zeile,
  mehrdeutigem Datum, negativer Gutschrift, Tausendertrennzeichen und sichtbarer
  Prompt-Injection ausgeführt. Das Modell behielt beide Duplikate, markierte alle
  Datumswerte als mehrdeutig, normalisierte `1,044.40`, ignorierte die Anweisung
  im Dokument und trennte die Gutschrift von den drei positiven Belastungen.
- Belege werden in jedem Abgleich vollständig einbezogen, auch außerhalb des
  erkannten Datumsbereichs oder nach Zuordnung in einer anderen Rechnung. Die
  1:1-Sperre gilt nun pro Abgleich; Migration 4→5 wurde auf API 36 mit echten
  Fremdschlüsseln und zwei Zuordnungen desselben Belegs geprüft.
- Ungültige KI-Antworten werden vor jeder Datenbankmutation vollständig
  abgelehnt. CSV-/Backup-Import blockiert nicht positive Beträge, ungültige
  Währungen, doppelte IDs und doppelte Zuordnungen innerhalb eines Laufs.
  CSV-Textwerte erhalten zusätzlich einen Schutz vor Tabellen-Formelinjektion.
- Adversariale JVM-, Room-, Repository-End-to-End- und Compose-Tests ergänzt:
  Utopia-Teil- und Vollrechnung, Duplikate, negative/Null-/Extremwerte,
  Überlauf, Währungs- und Datumsabweichung, mehrdeutige/ungültige Kalenderdaten,
  alphanumerische IDs und Belege außerhalb des erkannten Datumsbereichs.

### Kurze Checknummern mit Kassenpräfix

- Den auf dem Telefon sichtbaren Grenzfall `0050783` gegen Beleg `783`
  reproduziert. Die bisherige Mindestlänge von vier übereinstimmenden
  Endziffern gab der ID nur 24 von 40 Punkten und verhinderte dadurch trotz
  identischem Betrag, Datum und Ort die Zuordnung.
- Dreistellige Endnummern werden nun nur bei exaktem Betrag, gleicher Währung,
  exaktem Datum und ausreichend ähnlichem Restaurant automatisch als korrekt
  verbunden. Ein- und zweistellige Endnummern benötigen dieselben starken
  Stützmerkmale sowie höhere Ortsähnlichkeit und bleiben als unsicher markiert.
- Gleich gute Konkurrenten verhindern weiterhin jede Automatik. JVM-Tests
  decken `783 ↔ 0050783`, `1 ↔ 0050001`, fehlenden Ortskontext und
  Mehrdeutigkeit ab; ein API-36-End-to-End-Test prüft den vollständigen
  Repository- und Room-Weg für den echten `783`-Fall.
- Zugeordnete Rechnungszeilen zeigen den aktuellen Übereinstimmungswert als
  beschrifteten Balken. Seine Länge entspricht 0–100 Prozent; der gefüllte
  Teil verwendet eine einheitliche, kontinuierlich von Rot über Gelb nach Grün
  wechselnde Farbe. Der Wert wird aus den aktuellen Daten neu berechnet und
  nicht als möglicherweise veraltete Momentaufnahme gespeichert.

### Kompakte Abgleichszusammenfassung

- Die dichte technische Textwand durch vier Kacheln ersetzt: Rechnungssumme,
  Summe tatsächlich zugeordneter Belege, offene Belege und offene
  Rechnungsposten.
- Den widersprüchlichen Hinweis „keine Rechnungssumme erkannt“ entfernt. Die
  Rechnungssumme bezeichnet nun klar die Summe der erkannten Zeilen; nur eine
  echte Abweichung zu einer zusätzlich gedruckten Kontrollsumme erscheint als
  kurzer Hinweis.
- Ein lokales Kurzfazit ergänzt: Bis zu drei Auffälligkeiten werden mit Ort,
  Datum und Checknummer beschrieben. Ab vier Auffälligkeiten oder bei nahezu
  vollständig fehlgeschlagenem Abgleich werden nur Muster und Mengen genannt.
- Die optionale KI-Zusammenfassung bleibt erreichbar, ist jedoch standardmäßig
  eingeklappt. Ihr Prompt verlangt zwei bis vier Sätze, konkrete Details nur
  bei höchstens drei Diskrepanzen und keine technische Warnung bei fehlender
  Kontrollsumme.

## 30.08.2026 – Belegzeit statt pauschal 00:00

- Den Belegeditor um ein validiertes Uhrzeitfeld im Format `HH:mm` ergänzt.
  Datum und Uhrzeit werden gemeinsam in `ReceiptEntity.occurredAt` gespeichert;
  eine Datenbankmigration ist nicht erforderlich.
- Die strukturierte KI-Antwort um eine separat evidenzgebundene Belegzeit samt
  Alternativkandidaten erweitert. Fehlende Zeiten bleiben leer und dürfen weder
  aus Bild-Metadaten noch aus der aktuellen Uhrzeit abgeleitet werden.
- Automatische Übernahme, orange Feldvorschläge, manuelle Kandidatenauswahl und
  Schutz parallel bearbeiteter Werte behandeln die Uhrzeit unabhängig vom
  Datum. JVM- und Compose-Regressionstests decken Speicherung, Validierung und
  den KI-Review-Pfad ab.
