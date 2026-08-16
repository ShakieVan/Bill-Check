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

