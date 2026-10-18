# Coding-Agent-Anweisungen: Mamma Movie!

## Projekt

Mamma Movie! ist eine Java-Web-App, die lokal oder öffentlich auf Render laufen kann. Das Backend verwendet Java 17, Maven, Jackson und den Java-`HttpServer`; das Frontend verwendet HTML, CSS und Vanilla JavaScript. JUnit 5 wird für Tests verwendet. Lokal ist Port 8080 der Standard; online wird `PORT` gelesen. Der Server muss auf `0.0.0.0` lauschen.

## Struktur

- `src/Main.java`: Einstiegspunkt, konfigurierbarer HTTP-Server und Routen (`/api/catalog`, `/api/stats`, `/api/recommend`, `/api/surprise`, `/api/watch-providers`). Liefert außerdem Webdateien, Manifest und Icons.
- `src/Film.java`, `src/FilmSammlung.java`: Filmtyp, lokaler Katalog und Empfehlungs-/Filterlogik.
- `src/FilmMetadaten.java`, `src/FilmBewertung.java`, `src/FilmWatchProvider.java`, `src/FilmMetadatenService.java`: optionale externe Filmdaten, Bewertungen, österreichische Anbieter und lokaler Metadaten-Cache.
- `src/data/films.json`: lokale Filmsammlung mit 600 Filmen; bestehende 400 Filme und ihre IDs erhalten.
- `src/web/index.html`, `src/web/styles.css`, `src/web/app.js`: Browseroberfläche und lokale Browserzustände.
- `src/web/manifest.webmanifest`, `src/web/icons/`: PWA-Metadaten und lokale App-Icons; kein Service Worker.
- `test/FilmSammlungTest.java`, `test/FilmMetadatenServiceTest.java`: Filter-, Katalog-, Cache-, API- und Fehlerfalltests.
- `pom.xml`: Java-Version, Maven-Abhängigkeiten, Quellen- und Ressourcenstruktur.
- `Dockerfile`, `render.yaml`, `.dockerignore`: Container-Build, Render Blueprint und Ausschluss lokaler Secrets aus dem Build-Kontext.

## Backend und Frontend

Der Browser lädt HTML, CSS, JavaScript, Manifest und Icons vom Java-Server. Die Filmfilter werden im Backend gegen `src/data/films.json` ausgewertet. Gesehene IDs und Favoriten werden unabhängig voneinander im Browser-`localStorage` je Browser/Gerät gehalten. Das Backend liefert Katalogstatistik, optionale TMDB-/OMDb-Metadaten und AT-Providerdaten an die Oberfläche. Für ältere Favoriten ohne gespeicherte Providerdaten verwendet das Frontend `/api/watch-providers?movieId=<lokale Film-ID>`.

## Externe Dienste und Zugangsdaten

- `TMDB_API_KEY` ist der serverseitige TMDB API Read Access Token. Er wird für Poster, Metadaten, Bewertungen von TMDB und Watch Providers für Region `AT` verwendet.
- `OMDB_API_KEY` ist der optionale OMDb-Schlüssel für externe Ratings und gegebenenfalls Poster-Fallback.
- Schlüssel ausschließlich über Umgebungsvariablen in der IntelliJ-Run-Konfiguration oder der lokalen Shell setzen. Niemals Schlüssel in JavaScript, HTML, CSS, versionierte Konfigurationsdateien oder Dokumentationsbeispiele mit echtem Wert schreiben.
- `.mamma-movie-cache/metadata-cache.json` enthält lokale Cache-Daten und ist ignoriert. Keine Cache-Datei mit persönlichen oder geheimen Daten committen.
- `render.yaml` enthält nur Variablennamen mit `sync: false`, keine Werte. Automatische Deployments sind ausgeschaltet; Veröffentlichung nicht ohne ausdrücklichen Auftrag aktivieren.
- `.dockerignore` muss IntelliJ-Dateien, Cache, Build-Ausgabe und Env-Dateien vom Docker Build-Kontext ausschließen.
- Externe Daten sind optional: API-Fehler dürfen lokale Filmempfehlungen nicht verhindern.

## Änderungsregeln

- Bestehende Empfehlung, Filter, Favoriten, Gesehen-Status, Fallbacks und API-Routen erhalten; Funktionen nur mit konkretem Grund ändern oder entfernen.
- Keine neuen Frameworks, Datenbanken oder externen APIs ohne ausdrücklichen Projektauftrag hinzufügen.
- Änderungen an der UI in den vorhandenen Dateien halten und die bestehende Struktur wiederverwenden.
- Katalogänderungen in `src/data/films.json` vornehmen; eindeutige IDs und realistische Pflichtangaben sicherstellen.
- Bei Katalogpflege alle bisherigen Filme behalten. Die 200 Ergänzungen sollen TMDB-geprüft und populär sein; Filterabdeckung für alle auswählbaren Genre-/Stimmungs-Kombinationen erhalten.
- Nach Änderungen `mvn test` ausführen. Für einen vollständigen Build `mvn clean verify` verwenden. Bei Änderungen an `src/web/app.js` zusätzlich `node --check src/web/app.js` ausführen.
- Änderungen an `README.md`, `features.md` und `agents.md` mit dem tatsächlichen Projektstand synchron halten.
- Das Prompt Diary ist vom Nutzer verwaltet: nicht ersetzen, rekonstruieren, neu erstellen oder ohne ausdrücklichen Auftrag bearbeiten.
- Keine Secrets, lokalen IntelliJ-Zugangsdaten oder unnötigen Cache-/Build-Dateien in die Dokumentation oder ins Git aufnehmen.
