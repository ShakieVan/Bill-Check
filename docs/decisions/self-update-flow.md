# Integrierte GitHub-Updateverwaltung

## Verteilung

Bill Check wird zunächst als signierte Universal-APK über GitHub Releases
verteilt. Die App prüft den öffentlichen Endpunkt
`/repos/ShakieVan/Bill-Check/releases/latest` ohne Anmeldung.

Der Ablauf bleibt vollständig nutzergesteuert:

1. Veröffentlichung und Versionsnummer feststellen,
2. Universal-APK auswählen,
3. erst auf Tastendruck herunterladen,
4. Größe und SHA-256-Digest prüfen,
5. Android-Systeminstaller öffnen.

Die App installiert nichts still. Die Android-Freigabe „Unbekannte Apps
installieren“ gilt nur für Bill Check und kann nach dem Update wieder
deaktiviert werden.

## Prüfzeitpunkt

Beim Öffnen der App wird höchstens einmal innerhalb von 24 Stunden geprüft.
Es gibt absichtlich weder einen permanenten Hintergrunddienst noch einen
periodischen WorkManager-Job. Die Updateverwaltung kann die Prüfung jederzeit
manuell und ohne Zeitbegrenzung wiederholen.

## Integrität

Die GitHub-Release-API liefert für hochgeladene Assets einen Digest im Format
`sha256:<hex>`. Bill Check verlangt diesen Digest, lädt zunächst in eine
private `.part`-Datei und macht die APK erst nach folgenden Prüfungen
installierbar:

- vollständig geladene, von GitHub gemeldete Dateigröße,
- exakt passender SHA-256-Digest,
- eindeutige Universal-APK.

Eine unvollständige, veränderte oder nicht mit Digest veröffentlichte APK wird
gelöscht und nicht an den Installer gegeben. Android prüft beim eigentlichen
Update zusätzlich die Übereinstimmung der App-Signatur.

## Bedienung

Die Updateverwaltung ist über das Dreipunktmenü erreichbar und zeigt
installierte sowie verfügbare Version, Status, Release Notes, Downloadfortschritt
und die jeweils sinnvolle Hauptaktion. Kopf und Abschlussaktion bleiben fest,
der Inhalt ist auch bei großer Schrift vollständig scrollbar.

