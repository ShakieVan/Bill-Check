# Private Testbilder im Android-Emulator

Reale Belegbilder dürfen nicht in das öffentliche Repository oder die APK
aufgenommen werden. Für lokale Tests können sie dennoch per ADB in den
virtuellen Gerätespeicher kopiert werden.

## Vorgehen

1. Im Emulator ein ausschließlich für Tests vorgesehenes Verzeichnis wie
   `/sdcard/Pictures/Bill Check Test` anlegen.
2. Bilder aus dem privaten Nachbar-Repository einzeln mit `adb push` dorthin
   kopieren.
3. Für jede kopierte Datei einen Media-Scan über
   `android.intent.action.MEDIA_SCANNER_SCAN_FILE` auslösen.
4. Danach erscheinen die Bilder im Android Photo Picker und können ohne
   besondere App-Berechtigung ausgewählt werden.

Das Verfahren kopiert nur in den lokalen Emulator. Die Quelldateien bleiben
unverändert und Git sieht weder Originale noch Emulator-Kopien. Vor einem
Import muss die Ziel-Seriennummer ausdrücklich auf `emulator-*` geprüft
werden, damit reale Telefone nicht versehentlich mit Testmaterial gefüllt
werden.
