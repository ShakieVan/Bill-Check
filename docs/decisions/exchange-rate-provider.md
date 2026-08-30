# Online-Wechselkurse

Status: verbindlich für den ersten Anbieter

## Entscheidung

Bill Check verwendet zunächst den offenen, schlüssellosen Endpunkt von
[ExchangeRate-API](https://www.exchangerate-api.com/docs/free). Er unterstützt
frei wählbare Basis- und Zielwährungen, liefert einen täglichen
Aktualisierungszeitpunkt und erlaubt das lokale Zwischenspeichern der Antwort.

Die Anbietergrenze ist durch `ExchangeRateProvider` vom restlichen
Domänenmodell getrennt. Damit können später weitere oder selbst gehostete
Quellen ergänzt werden.

## Verhalten

- Beim Hinzufügen einer Reisewährung wird der Kurs automatisch abgefragt und bleibt
  manuell überschreibbar.
- Im Modus `DAILY` wird vor jedem neuen Beleg ein aktueller Tageskurs
  angefordert.
- Der Endpunkt wird pro Basis-/Zielpaar bis zu seinem gelieferten nächsten
  Aktualisierungszeitpunkt gecacht.
- Netzwerk- und Anbieterfehler verhindern keine Erfassung; dann gilt der
  letzte zwischengespeicherte Online-Kurs oder, falls keiner existiert, der
  manuell gespeicherte Reisekurs.
- Jeder Beleg speichert den tatsächlich verwendeten Kurs als Snapshot.
- Die vom offenen Endpunkt verlangte Attribution wird im Kursdialog als Link
  angezeigt.

## Einschränkung

Der kostenlose Endpunkt liefert indikative Tagesmittelkurse und keinen
konkreten Karten-, Bargeld- oder Hotelabrechnungskurs. Deshalb bleibt die
manuelle Korrektur fachlich unverzichtbar.
