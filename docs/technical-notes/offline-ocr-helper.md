# Lokale OCR-Bausteinhilfe

## Umsetzung

Die gebündelte lateinische ML-Kit-Texterkennung läuft vollständig lokal und
benötigt nach Installation keinen Modelldownload. Sie zerlegt erkannte Zeilen
in eindeutige Textbausteine. Im Belegeditor wird zunächst das Zielfeld gewählt
(Ort, Checknummer, Gesamtbetrag oder ein Posten); anschließend fügt ein Tipp
auf einen Baustein dessen Text dort ein. Mehrere Wörter werden bei Textfeldern
mit Leerzeichen aneinandergefügt.

Das ist bewusst eine Bausteinhilfe, keine automatische fachliche Zuordnung.
Sie bleibt bei fehlendem Internet nutzbar und lässt den Menschen über jedes
übernommene Wort beziehungsweise jeden Betrag entscheiden. Drag-and-drop kann
später zusätzlich angeboten werden; Antippen ist auf kleinen Displays der
robustere erste Bedienweg.

## Regressionstest

Mit einem privaten Belegbild im Android-16-Emulator wurden Textblöcke lokal
erkannt, Zielchips gewechselt und einzelne Wörter in das Ortsfeld übernommen.
Der Test benötigt weder Gemini-Schlüssel noch Netzwerkzugriff. Private
Belegbilder bleiben außerhalb dieses öffentlichen Repositorys.

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
minifizierten Release-APK auf demselben S24 Ultra erneut getestet: erkannte
Textbausteine und Zielfelder erschienen, ohne Fehlerprotokoll. Es wurde kein
Baustein übernommen und der Beleg nicht gespeichert.
