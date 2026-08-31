# Signierter GitHub-Release

## Schlüssel und GitHub-Secrets

Der eigene Bill-Check-Produktionsschlüssel liegt ausschließlich außerhalb des
öffentlichen Repositorys unter:

`C:\Users\Shakie\Documents\Programmierung\Bill-Check-Private\signing`

Keystore und `RECOVERY.txt` müssen gemeinsam privat gesichert werden. Ein
verlorener Schlüssel kann bestehende Installationen nicht mehr aktualisieren.
Die lokale `key.properties` ist ignoriert. GitHub Actions erhält denselben
Schlüssel nur über diese Repository-Secrets:

- `BILL_CHECK_KEYSTORE_BASE64`
- `BILL_CHECK_KEYSTORE_PASSWORD`
- `BILL_CHECK_KEY_ALIAS`
- `BILL_CHECK_KEY_PASSWORD`

Schlüssel, Kennwörter und der Inhalt der lokalen Dateien dürfen nie in Logs,
Commits, Issues oder Release Notes erscheinen.

## Release-Vertrag

Für Version `X.Y.Z` müssen gemeinsam gelten:

- `versionCode` wurde strikt erhöht,
- `versionName` ist exakt `X.Y.Z`,
- der signierte Commit trägt den Tag `vX.Y.Z`,
- `docs/releases/vX.Y.Z.md` existiert,
- der Assetname ist `Bill-Check-vX.Y.Z-universal.apk`,
- `SHA256SUMS-vX.Y.Z.txt` liegt daneben.

Der Tag startet `.github/workflows/release.yml`. Die Action prüft Unit-Tests,
Lint, Release-Signierung, Tag/Versionsgleichheit und APK-Signatur. Erst danach
legt sie den öffentlichen GitHub-Release samt APK, Prüfsummen und Release Notes
an. Drafts oder Prereleases werden nicht von der App als `latest` angeboten.

## Einmaliger Release-Build

Der reguläre Release-Prozess baut die signierte APK ausschließlich auf GitHub.
Lokal werden vor dem Tag Unit-Tests, Lint und die Git-Konsistenz geprüft, aber
kein zweiter `assembleRelease` ausgeführt. So bleibt gewährleistet, dass das
veröffentlichte Artefakt exakt aus dem getaggten Commit entsteht, ohne denselben
Release-Build lokal und auf GitHub doppelt auszuführen.

Ein lokaler Produktionsbuild bleibt für gezielte Diagnosefälle möglich, ist
aber keine Pflichtprüfung vor jeder Veröffentlichung.

## Lokale Pflichtprüfung

Vor Tag und Veröffentlichung:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug --console=plain
git diff --check
```

Nach erfolgreicher Veröffentlichung wird die tatsächlich von GitHub
ausgelieferte APK zusammen mit `SHA256SUMS-vX.Y.Z.txt` heruntergeladen. An
diesem öffentlichen Artefakt werden mindestens geprüft:

- SHA-256-Prüfsumme gegen `SHA256SUMS-vX.Y.Z.txt`,
- APK-Signatur und Zertifikat-Fingerprint,
- Paketname, `versionCode`, `versionName`, Minimum- und Ziel-API.

Erwartet sind Paket `de.shakie.billcheck`, Android 16 als Minimum/Ziel und die
beabsichtigte stabile Versionsnummer. Der Zertifikat-Fingerprint muss dem
vorherigen Release entsprechen.

