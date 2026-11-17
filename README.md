# Planora: Kalender, Countdowns, Checklisten und Notizen für Android

Native Android-App (Kotlin, Jetpack Compose, Room, DataStore, WorkManager/AlarmManager, Glance-Widgets).
Sie funktioniert komplett offline und braucht keine Anmeldung.

## APK erzeugen

### Variante A: automatisch über GitHub (ohne eigene Installation)
1. Neues (gern privates) Repository auf github.com anlegen.
2. Den Inhalt dieses Ordners hochladen, per „Add file → Upload files“ oder mit `git push`.
3. Im Reiter **Actions** startet der Workflow „APK bauen“ automatisch (oder manuell über „Run workflow“).
4. Nach ca. 5–8 Minuten erscheint unter dem Workflow-Lauf das Artefakt **Planora-APK**.
   Darin liegen `app-release.apk` (installierbar, signiert mit Debug-Schlüssel) und `app-debug.apk`.

### Variante B: Android Studio
1. Android Studio (Koala oder neuer) installieren.
2. „Open“ → diesen Ordner wählen und den Gradle-Sync abwarten.
3. Menü **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
4. Die APK liegt danach unter `app/build/outputs/apk/`.

### Variante C: Kommandozeile (JDK 17 und Android SDK nötig)
```
./gradlew assembleRelease
```

Installation auf dem Handy: APK aufs Gerät kopieren, öffnen und „Installation aus unbekannten Quellen“ erlauben.

## Funktionen

| Bereich | Inhalt |
|---|---|
| **Navigation** | 5 Tabs: Kalender, Countdown, **Favoriten**, Checklisten, Notizen. Dazu Übersicht (Dashboard), globale Suche, Tag-Verwaltung und Einstellungen |
| **Kalender** | Monats-, Wochen-, Tages- und Listenansicht; Termine mit Emoji, Farbe, Beschreibung, Kategorie, Tags, Start/Ende, ganztägig; mehrere Erinnerungen; Wiederholungen (täglich/wöchentlich/monatlich/jährlich/benutzerdefiniert, optional mit Enddatum); bearbeiten, duplizieren, löschen; eigene Designs pro Termin (gefüllt, Umriss, Verlauf, Bild, Muster); Hintergrundbild, Farbe und Emoji für einzelne Tage (Tag lange drücken) und ganze Monate (🖌️) |
| **Countdowns** | Karten-Design nach Referenzbild; **der nächste Countdown steht immer oben** (angeheftete zuerst, dann „heute“, dann nach Datum, danach Hochzähler und vergangene); weitere Sortierungen: Erstellung, alphabetisch, manuell per Drag & Drop, nach Tags; Typen: Countdown, Hochzählen seit, wiederkehrend; Live-Detailansicht mit Sekunden; teilen |
| **Countdown-Design** | Farbe, Farbverlauf, eigenes Bild (Zoom, Position, Helligkeit), 8 vorgefertigte Muster, 12 Design-Vorlagen, Deckkraft, Farbfilter, transparenter Verlauf (Richtung/Intensität/Farbe), frei wählbare Zeiteinheiten (Jahre bis Sekunden), Schriftfarbe/-art/-größe, Kartenhöhe/-breite/-radius/-form, Abstände, Emoji-, Titel- und Countdown-Position, 4 Layouts, eigene Symbole und Vorschaubild |
| **Checklisten** | Beliebig viele Listen mit Emoji, Farbe, Schriftfarbe und 4 Designs; Fortschritt in %; Aufgaben mit Priorität, Fälligkeit, Erinnerung, Wiederholung (erscheint nach dem Abhaken mit neuem Datum wieder), Tags, Notiz; sortieren, per Drag & Drop verschieben, in andere Listen verschieben |
| **Notizen** | Formatierung: Überschriften, fett, kursiv, Aufzählungen, Nummerierung, Textfarben, Schriftgrößen, Emojis (Live-Darstellung im Editor plus Vorschau); 8 Designs, eigene Farben, Hintergrundbild; Suche, Favoriten, Anheften, Sortierung nach Datum/Kategorie/Titel; Raster- oder Listenansicht |
| **Tags** | Bereichsübergreifend mit Name, Emoji, Farbe und Beschreibung; Tag antippen zeigt alle Termine, Countdowns, Checklisten, Aufgaben und Notizen mit diesem Tag |
| **Benachrichtigungen** | AlarmManager (exakt, auch im Doze-Modus), Wiederherstellung nach Neustart und Zeitumstellung, Schutz vor Duplikaten, Android-13-Berechtigung, Ton, Vibration, Vorschau, Ruhezeiten, tägliche Erinnerung an offene Aufgaben, Testbenachrichtigung |
| **Widgets** | Countdown (einzeln), Nächste Countdowns, Kalender (Monat/Woche/Heute/Nächste), Checkliste (direkt abhaken), Notiz und Schnellnotiz. Jedes Widget ist konfigurierbar (Eintrag, Farbe, Deckkraft, Schrift, Ecken, Einheiten). Aktualisierung bei Änderungen und alle 15 Minuten |
| **Personalisierung** | 9 Themes (Colorful, Pastell, Pink, Lila, Minimalistisch, Dark, Nature, Aesthetic, Modern), Hell/Dunkel/System, Akzent-, Hintergrund- und Kartenfarbe, 5 Schriftarten, Schriftgröße, Rundungen, Abstände, Icon-Stil, App-Hintergrundbild, Animationen, Startbildschirm, anpassbare Übersicht |
| **Datensicherung** | JSON-Export und -Import (inkl. Bilder) für den Gerätewechsel, tägliche automatische lokale Sicherung (die letzten 7 bleiben erhalten), Android Auto-Backup |

## Projektstruktur
```
app/src/main/java/com/planora/app/
├── data/      Room-Entitäten & DAOs, Repository, DataStore-Einstellungen, Bilder, Backup
├── domain/    Datums-, Wiederholungs- und Countdown-Logik (reines Kotlin, getestet)
├── notify/    Benachrichtigungen, AlarmManager, Boot-Receiver, WorkManager
├── widget/    Glance-Widgets, Widget-Konfiguration, Bitmap-Renderer für Karten
└── ui/        Compose-Screens, Komponenten, Theme, Navigation
app/src/test/  Unit-Tests für Datumsberechnungen (Monatsenden, Schaltjahre, Sortierung)
```
