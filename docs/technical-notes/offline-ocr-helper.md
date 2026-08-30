# Räumliche OCR-Textauswahl

## Umsetzung

Die gebündelte lateinische ML-Kit-Texterkennung läuft vollständig lokal und
benötigt nach Installation keinen Modelldownload. Statt einer deduplizierten
Wortliste speichert sie die Hierarchie aus Seite, Blöcken, Zeilen, Wörtern und
Zeichen samt Pixel- und normalisierten Koordinaten.

„Text im Bild auswählen“ zeigt den vollständigen Beleg. Ein Tipp oder langer
Druck wählt ein Wort. Die beiden blauen Anfangs- und Endgriffe lassen sich
anschließend wie bei der nativen Textauswahl verschieben und verändern die
Auswahl bis auf Zeichenebene. Während des Ziehens vergrößert eine Lupe den
Bereich unter dem Finger. Zoom und Verschieben bleiben möglich. Nach dem Haken
kehrt die App in den Editor zurück; alle geeigneten Zielfelder sind
hervorgehoben. Erst der bewusste Tipp auf ein Feld ersetzt dessen Inhalt. Die
frühere Wortfetzen-/Zielchip-Oberfläche wurde vollständig entfernt.

Die normale Aktion „Bild auswerten“ extrahiert nur die fachlichen Belegdaten.
Erst „Text im Bild auswählen“ startet parallel die lokale Erkennung und einen
separaten räumlichen KI-Transkriptlauf. Dessen meist besserer Text wird mit der
lokalen Geometrie fusioniert. Passende Zeilen behalten die präzisere
ML-Kit-Position; weitere sichtbare KI-Zeilen werden mit ihrer groben Box
ergänzt. Während einer langsamen oder nicht erreichbaren KI kann der Nutzer
bewusst sofort mit der lokalen Erkennung fortfahren. Ergebnisse sind an den
aktuellen Bildauftrag gebunden, damit verspätete Antworten nicht in einen
anderen Editor geraten. Ergänzte Zeichenpositionen bleiben geometrische
Näherungen.

## Regressionstest

Mit privaten Belegen im Android-16-Emulator wurden Wort-, Zeichen- und
Mehrwortauswahl, beide verschiebbaren Auswahlgriffe, die Lupe während einer
laufenden Geste, Haken/Abbrechen und die gezielte Übernahme in hervorgehobene
Felder geprüft. Synthetische Unit-Tests prüfen Geometrie, Leserichtung und die
Fusion von lokalem sowie KI-Text. Private Belegbilder bleiben außerhalb dieses
öffentlichen Repositorys.

## Android-16-Releasefehler

Auf einem Galaxy S24 Ultra schlug die Erkennung ausschließlich in der
minifizierten Release-APK mit einer Nullreferenz in ML Kit fehl. Der per R8-
Mapping zurückgeführte Stack zeigte eine fehlerhafte horizontale
Klassenzusammenführung interner ML-Kit-Lazy-Instance- und Telemetrieklassen mit
unabhängigen generierten Klassen. Die internen ML-Kit-Pakete werden deshalb in
`proguard-rules.pro` gezielt unverändert gehalten.

Zusätzlich dekodiert die App Bilder nun selbst über Android `ImageDecoder` und
begrenzt die längste Kante für die OCR auf 3072 Pixel. Das unterstützt große
Samsung-Kamerabilder zuverlässig, hält den Speicherbedarf begrenzt und wahrt
mehr Textdetail als eine kleine Vorschaudatei.

Der ursprüngliche private Beleg wurde anschließend mit der signierten,
minifizierten Release-APK auf demselben S24 Ultra erneut getestet: die
Erkennung lief ohne Fehlerprotokoll. Es wurde kein Beleg gespeichert.
