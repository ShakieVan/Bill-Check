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

## Lokale Pflichtprüfung

Vor Tag und Veröffentlichung:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleRelease --console=plain
git diff --check
```

Danach mit den neuesten Android Build Tools mindestens prüfen:

```powershell
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
aapt2 dump badging app\build\outputs\apk\release\app-release.apk
```

Erwartet sind Paket `de.shakie.billcheck`, Android 16 als Minimum/Ziel und die
beabsichtigte stabile Versionsnummer. Der Zertifikat-Fingerprint muss dem
vorherigen Release entsprechen.

