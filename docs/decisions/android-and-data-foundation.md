# Android- und Datenfundament

Status: verbindlich

## Entscheidung

Bill Check wird als native Kotlin-/Compose-App ausschließlich für Android 16
(API 36) entwickelt. Room ist die lokale Quelle der Wahrheit. Öffentliche
Galeriebilder werden über stabile `content://`-Referenzen mit Datensätzen
verbunden; eine Datensatzlöschung löscht keine Galerieinhalte implizit.

Geldwerte liegen in ganzzahligen, währungsspezifischen Minor Units vor. Jeder
Beleg hält Währung, Kurs und das daraus berechnete exakte Ergebnis in der
Heimatwährung seiner Reise als unveränderlichen Snapshot fest. Ein Trinkgeld
hat einen unabhängigen Kurs-Snapshot. Wechselkurse werden als Dezimaltext
persistiert und mit `BigDecimal` verarbeitet.

Schemaänderungen werden über explizite, verlustfreie Room-Migrationen
ausgeliefert. Ein Versionswechsel darf die lokale Datenbank nicht destruktiv
neu anlegen. Die persistente Stapelwarteschlange ist ab Schema 7 Teil derselben
lokalen Quelle der Wahrheit und wird mit der jeweiligen Reise kaskadierend
gelöscht.

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
