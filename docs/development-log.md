# Entwicklungsprotokoll

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
