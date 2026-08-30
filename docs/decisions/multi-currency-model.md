# Mehrwährungsmodell

Status: verbindlich

## Entscheidung

Die App-Einstellung `Heimatwährung` ist ausschließlich die Vorgabe für neue
Reisen. Jede Reise übernimmt beim Anlegen einen unveränderlichen Snapshot
dieser Heimat- und Berichtswährung. Eine spätere Einstellungsänderung verändert
keine bestehende Reise und keine historische Summe.

Eine Reise enthält mindestens ihre Heimatwährung und kann beliebig viele
aktuelle ISO-4217-Zahlungsmittel enthalten. Genau eine davon ist die
Standardwährung für neue Belege. Die Heimatwährung hat immer den festen Kurs
`1`; andere Kurse bedeuten eindeutig `1 Heimatwährung = x Reisewährung` und
können fest oder täglich aktualisiert sein. Bereits von Belegen oder
Trinkgeldern verwendete Währungen dürfen nicht aus der Reise entfernt werden.

## Beträge und Snapshots

- Geldwerte werden als ganzzahlige Minor Units mit den ISO-Nachkommastellen
  der jeweiligen Währung gespeichert, etwa JPY 0, EUR/EGP 2 und KWD 3.
- Jeder Beleg speichert Betrag, Währung, Kurs-Snapshot und den daraus
  berechneten exakten Betrag in der Heimatwährung der Reise.
- Ein Trinkgeld besitzt einen eigenen Währungs- und Kurs-Snapshot. Der
  Belegkurs darf für ein Trinkgeld in einer dritten Währung nicht
  wiederverwendet werden.
- Ohne Trinkgeld werden kanonisch die Heimatwährung und Kurs `1` gespeichert.
  Nur tatsächlich verwendete Trinkgeldwährungen schützen eine Reisewährung vor
  dem Entfernen. Bei vorhandenem Trinkgeld zeigt die Belegkarte Betrag und
  separaten Kurs-Snapshot; der PDF-Bericht führt beides ebenfalls auf.
- Bei normaler Bearbeitung bleiben unveränderte Währungs-Snapshots erhalten.
  Ein expliziter Währungswechsel verwendet den aktuellen konfigurierten Kurs.
- Erst die exakten Heimatwährungsbeträge werden addiert; nur die prominente
  Anzeige wird danach auf die nächste volle Haupteinheit aufgerundet.

## Bedienung und Validierung

Der Offline-Währungskatalog bietet aktuelle Zahlungsmittel mit Kürzel,
lokalisiertem Namen und Land/Region. Beleg- und Rechnungseditoren zeigen zuerst
die Währungen der Reise; „Weitere Währung hinzufügen“ führt über denselben
durchsuchbaren Katalog und anschließend zwingend zur Kurskonfiguration.

Eine von der KI gedruckt erkannte Währung ist gegenüber der erwarteten
Standardwährung autoritativ. Ist sie bereits Teil der Reise, wird sie als
Vorschlag übernommen. Andernfalls muss sie zuerst sichtbar zur Reise
hinzugefügt werden; ungültige oder erfundene Codes und still erfundene Kurse
werden nicht gespeichert.

Die Vorabversion vor diesem Modell war noch nicht produktiv. Der Schemawechsel
auf Datenbankversion 6 verwirft deshalb den lokalen Entwicklungsbestand und
alte Backup-/CSV-Versionen, statt dauerhaft mehrdeutige Euro-Altspalten
mitzuführen.
