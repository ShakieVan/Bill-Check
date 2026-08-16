# Eingabedialoge und Bildschirmtastatur

## Problem

Das Standard-`AlertDialog` passt längere Formulare zwar teilweise an die
Bildschirmtastatur an, stellt aber nicht sicher, dass alle verdeckten Felder
frei erreichbar bleiben. Der Fokus scrollt ein aktives Feld höchstens bis an
den sichtbaren Rand; weitere Inhalte unterhalb bleiben dabei unzugänglich.

## Verbindlicher Aufbau

Eingabedialoge verwenden `ScrollableEditorDialog` mit drei getrennten
Bereichen:

1. fester Titel,
2. eine mit Gewicht begrenzte und unabhängig vertikal scrollbare Feldfläche,
3. fester Aktionsbereich für Abbrechen und Speichern.

Die Dialogoberfläche verwendet zusätzlich `imePadding()`. Dadurch kann die
Feldfläche auch bei sichtbarer Bildschirmtastatur unabhängig vom gerade
fokussierten Feld vollständig gescrollt werden. Reise- und Belegeditor teilen
sich diese Implementierung.

Kurze Hinweis-, Auswahl- oder Bestätigungsdialoge ohne Texteingaben dürfen
weiterhin `AlertDialog` verwenden. Neue Formulare sollen dagegen die
gemeinsame Editor-Komponente erweitern, statt eine eigene Dialogstruktur zu
duplizieren.

## Cursor bei ergänzten Texten

Textfelder mit Verlauf verwalten neben dem Inhalt auch ihre Auswahl als
`TextFieldValue`. Wird ihr Wert von außen ergänzt, etwa durch die lokale
Texthilfe oder einen Verlaufseintrag, springt die Auswahl ans neue Textende.
Eine vom Menschen gesetzte Auswahl bleibt bei gewöhnlicher Tastatureingabe
dagegen unverändert.

Die mobilen Compose-Auswahlgriffe scrollen einzeilige Felder beim Ziehen am
linken oder rechten Rand nicht auf allen Geräten zuverlässig weiter. Orts- und
Postenfelder werden daher als logisch einzeilige, visuell mehrzeilige Eingaben
dargestellt. Lange Inhalte brechen vollständig um und bleiben für direktes
Antippen oder Ziehen erreichbar; der umgebende Editor übernimmt bei Bedarf das
vertikale Scrollen. Die IME bietet weiterhin „Fertig“ statt eines
Zeilenumbruchs an.

## Geräteinstallation

Debug- und Release-Paket besitzen absichtlich getrennte App-IDs, damit ein
Entwicklungsbuild niemals die persönlichen Daten der regulären Installation
überschreibt. Der Debug-Launchername lautet deshalb sichtbar `Bill Check
Debug`. Nach einem Test auf einem persönlichen Gerät wird das Debug-Paket
wieder entfernt; die signierte Release-App bleibt installiert.
