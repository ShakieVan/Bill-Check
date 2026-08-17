# Abschlussaudit v0.1.0

Stand: 17.08.2026

Dieser Audit ordnet die verbindliche [`product-spec.md`](product-spec.md) dem
implementierten Android-16-Stand zu. Er ist ein Nachweis für den Draft-PR und
keine Behauptung, dass es künftig keine Erweiterungen mehr geben wird.

| Bereich | Umgesetzter Nachweis | Prüfung |
|---|---|---|
| Kernablauf und Navigation | Native Compose-Übersicht, Reise-Drawer, Dreipunkt-Einstellungen, Kamera-, Bild-, manuelle und Abgleich-Einstiege | Sämtliche Widget- und App-Einstiege im Android-16-Emulator geöffnet |
| Reisen | Anlage/Bearbeitung, Drag-and-drop, unsichtbare fortlaufende Sortierung, Währung, fixer oder täglicher Kurs, Trinkgeldbetrag/-währung/-vorauswahl | Anlage, Änderung, Umsortierung und Tageskurs-Fallback manuell geprüft |
| Historische Geldwerte | Kurs, Währung und vorhandenes Trinkgeld bleiben beim Bearbeiten alter Belege erhalten; neu aktiviertes Trinkgeld verwendet die aktuelle Vorgabe | `ReceiptSnapshotRulesTest`, `MoneyCalculatorTest` |
| Geld und Rundung | Centgenaue EUR-Beträge, Aufrunden je Anzeige und Summieren vor dem Aufrunden der Reisesumme | `MoneyCalculatorTest` und Dashboard-Durchstich |
| Bilder | Kameraoriginal in `DCIM/Bill Check`, Photo Picker und Ordnerwahl, große Vorschau, Verknüpfen/Ersetzen/Lösen, Galerieoriginal bleibt erhalten | Kamera auf Galaxy S24 Ultra sowie Kamera, Galerie und DocumentsUI im Emulator geprüft |
| Belegposten und Editor | Beliebig viele Posten, Summenübernahme/-abweichung, Verlauf für Textfelder, tastaturfester scrollbarer Editor, vollständige Nachbearbeitung | Mehrposten-Belege mit Bildschirmtastatur im Emulator geprüft |
| Zwischen-/Endrechnung | Unabhängige Läufe, vollständiger Audit aller Belege, 1:1-Automatik, Fuzzy-ID-Abgleich mit stützenden Merkmalen, sichtbarer Score, Kontrollsumme, kompakte Kennzahlen und lokale Befundzusammenfassung | Utopia-Endrechnung mit 11 Zeilen, `0015512`/`5512` und `0050783`/`783`; `ReconciliationMatcherTest`, `ReconciliationAuditTest`, `ReconciliationReportTest`, `ReconciliationNarrativeTest`, `UtopiaReconciliationEndToEndTest`, `ReconciliationSummaryUiTest` |
| Cloud-KI | Providergrenze, verschlüsselter Gemini-Schlüssel, unabhängige bildtreue Transkription, strikte Antwortvalidierung, Modelle/Kontext, AI-Studio-Link und bestätigungspflichtiges Ergebnis | Sparsamer echter Gemini-Durchstich; `GeminiPromptFactoryTest`, `StatementExtractionValidatorTest` |
| Offline-OCR | Gebündelte lokale Erkennung mit Zielfeld und einzeln antippbaren Text-/Betragsbausteinen | Offline mit privaten Emulatorbildern geprüft |
| Darstellung und Sprache | Ruhiges Material-3-Design, persistentes Hell/Dunkel nach einmaliger Systemvorgabe, Deutsch und Englisch über Android-App-Sprache | Beide Modi und deutsche App-Locale im Emulator geprüft |
| Export und Import | Selektives `.billcheck`-Vollarchiv inklusive Bildern/Zuordnungen, wieder importierbares Übersichts-CSV, PDF-Bericht, Kollisionsnamen und strikte Validierung gegen ungültige Geld-, Datums- und Textwerte | Vollarchiv-Roundtrip mit zwei Reisen und Bild; `CsvTransferCodecTest`, `TransferValidatorTest`, `BillCheckMigrationTest` |
| Homescreen-Widget | Reise, aufgerundete und genaue Summe, Belegzahl sowie vier direkte Aktionen | Widget im Android-16-Launcher; alle Aktionen geöffnet |
| Updates und Releases | 24-h-Prüfung, manueller Abruf, Release Notes, private Teildatei, Größe/SHA-256, Systeminstaller und signierter Tag-Workflow | Update-JVM-Tests, echte GitHub-Abfrage, signierte Release-APK installiert |
| Open Source und Datenschutz | Öffentlicher Code unter `GPL-3.0-only`; reale Testbilder und Geheimnisse bleiben privat | `LICENSE`, Git-/Ignore- und Repository-Prüfung |

## Bewusste spätere Option

Eine speicheroptimierte Kopie von Kameraaufnahmen ist in Version 0.1.0 nicht
aktiv. Das ist in der Produktspezifikation ausdrücklich als spätere,
optionale Einstellung festgehalten. Aktuell bleibt das Original erhalten,
damit OCR- und KI-Auswertung die höchstmögliche Bildqualität bekommen.

## Ausgeführte Freigabeprüfung

Für den Abschlussstand liefen 59 JVM-Unit-Tests und 5 instrumentierte
Android-16-Tests ohne Fehler. Android-Lint und der minifizierte Release-Build
waren erfolgreich. Die APK wurde mit dem privaten Produktionszertifikat
verifiziert, im Android-16-Emulator sowie auf einem Galaxy S24 Ultra
installiert und ohne `AndroidRuntime`-Absturz gestartet. Der reale
Utopia-Testfall enthält alle 11 Rechnungszeilen mit einer Kontrollsumme von
7.404,20 EGP und weist offene oder außerhalb liegende Belege sichtbar aus.
Der öffentliche CI-Lauf muss nach dem Push für denselben Commit ebenfalls grün
sein.
