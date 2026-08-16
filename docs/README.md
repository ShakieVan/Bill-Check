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
