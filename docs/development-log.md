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
