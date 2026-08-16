# Projektdokumentation

Diese Dokumentation hält Produktentscheidungen, technische Erkenntnisse und
Fehlschläge dauerhaft außerhalb einzelner Chats fest.

## Einstieg

- [`product-spec.md`](product-spec.md): fachlicher Zielzustand
- [`development-log.md`](development-log.md): chronologischer Arbeitsstand
- [`decisions/android-and-data-foundation.md`](decisions/android-and-data-foundation.md):
  verbindliche Plattform- und Datenbasis

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

