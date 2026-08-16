# Abschlussaudit v0.1.0

Stand: 16.08.2026

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
| Zwischen-/Endrechnung | Unabhängige Läufe, 1:1-Automatik, vier Statusfarben, gerankte manuelle Zuordnung, Akzeptieren, Zurücksetzen, Neuauswertung und Löschen | `ReconciliationMatcherTest` und vollständiger manueller Abgleich |
| Cloud-KI | Providergrenze, verschlüsselter Gemini-Schlüssel, Modelle/Kontext, AI-Studio-Link, bestätigungspflichtiges Ergebnis, präziser Bewirtungsort | Ein echter Gemini-Durchstich; `GeminiPromptFactoryTest` |
| Offline-OCR | Gebündelte lokale Erkennung mit Zielfeld und einzeln antippbaren Text-/Betragsbausteinen | Offline mit privaten Emulatorbildern geprüft |
| Darstellung und Sprache | Ruhiges Material-3-Design, persistentes Hell/Dunkel nach einmaliger Systemvorgabe, Deutsch und Englisch über Android-App-Sprache | Beide Modi und deutsche App-Locale im Emulator geprüft |
| Export und Import | Selektives `.billcheck`-Vollarchiv inklusive Bildern/Zuordnungen, wieder importierbares Übersichts-CSV, PDF-Bericht und Kollisionsnamen | Vollarchiv-Roundtrip mit zwei Reisen und Bild; `CsvTransferCodecTest` |
| Homescreen-Widget | Reise, aufgerundete und genaue Summe, Belegzahl sowie vier direkte Aktionen | Widget im Android-16-Launcher; alle Aktionen geöffnet |
| Updates und Releases | 24-h-Prüfung, manueller Abruf, Release Notes, private Teildatei, Größe/SHA-256, Systeminstaller und signierter Tag-Workflow | Update-JVM-Tests, echte GitHub-Abfrage, signierte Release-APK installiert |
| Open Source und Datenschutz | Öffentlicher Code unter `GPL-3.0-only`; reale Testbilder und Geheimnisse bleiben privat | `LICENSE`, Git-/Ignore- und Repository-Prüfung |

## Bewusste spätere Option

Eine speicheroptimierte Kopie von Kameraaufnahmen ist in Version 0.1.0 nicht
aktiv. Das ist in der Produktspezifikation ausdrücklich als spätere,
optionale Einstellung festgehalten. Aktuell bleibt das Original erhalten,
damit OCR- und KI-Auswertung die höchstmögliche Bildqualität bekommen.

## Ausgeführte Freigabeprüfung

Für den Abschlussstand liefen 26 Unit-Tests ohne Fehler. Android-Lint und der
minifizierte Release-Build waren erfolgreich. Die APK wurde mit dem privaten
Produktionszertifikat verifiziert, auf dem Android-16-Emulator installiert und
ohne `AndroidRuntime`-Absturz gestartet. Der öffentliche CI-Lauf muss nach dem
Push für denselben Commit ebenfalls grün sein.
