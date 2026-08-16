# Projektdokumentation

Diese Dokumentation hält Produktentscheidungen, technische Erkenntnisse und
Fehlschläge dauerhaft außerhalb einzelner Chats fest.

## Einstieg

- [`product-spec.md`](product-spec.md): fachlicher Zielzustand
- [`development-log.md`](development-log.md): chronologischer Arbeitsstand
- [`decisions/android-and-data-foundation.md`](decisions/android-and-data-foundation.md):
  verbindliche Plattform- und Datenbasis
- [`decisions/exchange-rate-provider.md`](decisions/exchange-rate-provider.md):
  Online-Kursquelle, Tagescache und Offline-Fallback
- [`decisions/receipt-image-storage.md`](decisions/receipt-image-storage.md):
  Kamera, Photo Picker und getrennte Lebenszyklen von Galerie und App-Datensatz
- [`decisions/reconciliation-matching.md`](decisions/reconciliation-matching.md):
  1:1-Zuordnung, Status und Lebenszyklus von Rechnungsabgleichen
- [`decisions/ai-image-extraction.md`](decisions/ai-image-extraction.md):
  Providergrenze, Schlüsselablage, Bildqualität und Ergebnisprüfung
- [`decisions/data-transfer-formats.md`](decisions/data-transfer-formats.md):
  selektive Vollsicherung, CSV-/PDF-Berichte und Importverhalten
- [`decisions/home-screen-widget.md`](decisions/home-screen-widget.md):
  Reiseübersicht und direkte Erfassungsaktionen auf dem Homescreen
- [`decisions/self-update-flow.md`](decisions/self-update-flow.md):
  nutzergesteuerte GitHub-Updates mit Digestprüfung und Systeminstaller
- [`technical-notes/editor-dialog-layout.md`](technical-notes/editor-dialog-layout.md):
  tastaturfester, wiederverwendbarer Aufbau für Eingabedialoge
- [`technical-notes/emulator-test-images.md`](technical-notes/emulator-test-images.md):
  privates Bildmaterial sicher in den lokalen Emulator einspielen
- [`technical-notes/offline-ocr-helper.md`](technical-notes/offline-ocr-helper.md):
  lokale Texterkennung und manuell gesteuerte Textbausteine
- [`technical-notes/github-release-process.md`](technical-notes/github-release-process.md):
  Signierschlüssel, GitHub-Secrets, Release-Vertrag und Pflichtprüfungen

## Ablage

- `decisions/`: dauerhafte Produkt- und Architekturentscheidungen
- `technical-notes/`: konkrete Implementierungserkenntnisse, Grenzen und
  Regressionstests
- `releases/`: nutzerorientierte Änderungen je Version

## Dokumentationsregel

Eine lokale Implementierungsentscheidung bleibt als kurzer Codekommentar im
Code. Wissen, das mehrere Dateien oder spätere Arbeiten betrifft, wird als
technische Notiz festgehalten. Eine bewusst gewählte und dauerhaft zu
schützende Richtung wird als Entscheidung dokumentiert.
