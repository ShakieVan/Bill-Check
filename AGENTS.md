# AGENTS.md – Bill Check

## Zweck

Bill Check ist eine native Android-App zum Erfassen von Hotelbelegen und zum
Abgleichen von Zwischen- und Endrechnungen. Die App ist offline nutzbar;
Cloud-KI ist eine optionale Auswertungshilfe, niemals Voraussetzung für den
Zugriff auf bereits gespeicherte Daten.

## Verbindliche Produktentscheidungen

- Native Kotlin-App mit Jetpack Compose, keine WebView und keine eingebettete
  HTML-Anwendung.
- Min-, Compile- und Target-SDK: Android 16 / API 36.
- App-ID: `de.shakie.billcheck`; Debug-Builds verwenden den Suffix `.debug`.
- Lokale Datenhaltung. Vollständige Sicherungen erfolgen über ein eigenes
  `.billcheck`-Format; CSV und PDF dienen dem Austausch und Bericht.
- Beträge werden centgenau berechnet. Prominente Übersichten runden positive
  Eurobeträge immer auf den nächsten vollen Euro auf.
- Eine Gesamtrundung erfolgt erst nach Addition der centgenauen Beträge.
- Jeder Beleg speichert den bei seiner Erfassung geltenden Wechselkurs.
- Abgleiche sind eigene, lösch- und zurücksetzbare Datensätze. Das Löschen
  eines Abgleichs verändert keine Belegstammdaten.
- In der App aufgenommene Bilder werden über MediaStore im Galeriealbum
  `Bill Check` gespeichert. Das Löschen eines App-Datensatzes löscht das
  Galerieoriginal nicht automatisch.
- Deutsch und Englisch werden über Android-Stringressourcen unterstützt.
- Heller und dunkler Modus sind explizit wählbar; beim ersten Start wird nur
  die aktuelle Systemeinstellung als Anfangswert übernommen.

## Datenschutz und Repository-Grenze

- Das öffentliche Repository enthält niemals reale Beleg- oder
  Rechnungsbilder, API-Schlüssel, Keystores oder `.billcheck`-Backups.
- Reale Testbilder liegen ausschließlich im privaten Nachbar-Repository
  `ShakieVan/Bill-Check-Data`.
- Öffentliche Tests verwenden synthetische oder vollständig anonymisierte
  Daten und dürfen nicht vom privaten Repository abhängen.
- API-Schlüssel werden nicht in BuildConfig, Ressourcen, Quellcode oder Git
  gespeichert.

## Technische Leitplanken

- UI: Jetpack Compose und Material 3.
- Persistenz: Room mit Foreign Keys und kaskadierenden Löschregeln.
- Geldwerte: ganzzahlige Minor Units; Wechselkurse und Berechnungen mit
  `BigDecimal`, niemals `Double` oder `Float`.
- Zustände aus KI/OCR müssen als Vorschläge erkennbar und manuell korrigierbar
  bleiben.
- Eingabedialoge verwenden den gemeinsamen Editor-Aufbau mit festem Kopf und
  Aktionsbereich, unabhängig scrollbarer Feldfläche und IME-Abstand. Ein
  einfaches `AlertDialog` ist nur für kurze Dialoge ohne Texteingaben gedacht.
- Eine Cloud-Provider-Abstraktion erlaubt Gemini zuerst und weitere Anbieter
  später. Provider-spezifische DTOs dürfen nicht das Domänenmodell bestimmen.
- Ein Abgleich ist im Normalfall eine 1:1-Zuordnung zwischen Rechnungszeile
  und Beleg. Manuelle Zuordnung und „bekannt/akzeptiert“ bleiben möglich.

## Arbeitsweise und Dokumentation

- Einstiegspunkt ist `docs/README.md`.
- Dauerhafte Entscheidungen gehören nach `docs/decisions/`.
- Technische Erkenntnisse, Fehlschläge und Regressionstests gehören nach
  `docs/technical-notes/` oder in `docs/development-log.md`.
- Nutzerrelevante Änderungen jeder veröffentlichten Version werden unter
  `docs/releases/` dokumentiert.
- Vor Commits Build und relevante Tests ausführen.
- Bestehende Nutzeränderungen und private Dateien nicht überschreiben oder
  versehentlich versionieren.
