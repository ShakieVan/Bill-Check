# Android- und Datenfundament

Status: verbindlich

## Entscheidung

Bill Check wird als native Kotlin-/Compose-App ausschließlich für Android 16
(API 36) entwickelt. Room ist die lokale Quelle der Wahrheit. Öffentliche
Galeriebilder werden über stabile `content://`-Referenzen mit Datensätzen
verbunden; eine Datensatzlöschung löscht keine Galerieinhalte implizit.

Geldwerte liegen in ganzzahligen Minor Units vor. Jeder Beleg hält
Fremdwährung, Wechselkurs und das daraus berechnete centgenaue Euroergebnis
als unveränderlichen Snapshot fest. Wechselkurse werden als Dezimaltext
persistiert und mit `BigDecimal` verarbeitet.

## Gründe

- Das Galaxy S23 Ultra und neuere Zielgeräte erhalten Android 16.
- Ein einzelnes API-Level reduziert den Kompatibilitäts- und Testumfang.
- Room bietet überprüfbare Relationen und kaskadierende Löschregeln für Reisen,
  Belege, Posten und Abgleichsläufe.
- Ganzzahlige Geldwerte verhindern binäre Gleitkommafehler.
- Galerieoriginale bleiben als unabhängiges Sicherheitsnetz erhalten.

## Grenzen

- Android 15 und älter werden bewusst nicht unterstützt.
- Eine gelöschte Galerieaufnahme kann von Bill Check nicht wiederhergestellt
  werden, sofern keine `.billcheck`-Sicherung existiert.
- Reale Testbilder sind keine Build-Abhängigkeit und dürfen öffentliche CI
  niemals erreichen.
