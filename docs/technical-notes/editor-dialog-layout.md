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
