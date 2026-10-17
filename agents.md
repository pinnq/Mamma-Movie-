# Coding-Agent-Anweisungen: Mamma Movie!

## Projekt

Mamma Movie! ist eine lokale Movie-Recommendation-Web-App. Das Backend ist Java 17 mit Maven; die Browseroberfläche verwendet HTML, CSS und Vanilla JavaScript. Der Java-`HttpServer` startet lokal auf Port 8080. Jackson verarbeitet JSON; JUnit 5 wird für Tests verwendet.

## Struktur

- `src/Main.java`: Einstiegspunkt, lokaler HTTP-Server und JSON-Endpunkte (`/api/catalog`, `/api/stats`, `/api/recommend`, `/api/surprise`).
- `src/Film.java`, `src/FilmSammlung.java`: Filmtyp, lokaler Katalog und Empfehlungs-/Filterlogik.
- `src/FilmMetadaten.java`, `src/FilmBewertung.java`, `src/FilmWatchProvider.java`, `src/FilmMetadatenService.java`: optionale externe Filmdaten, Bewertungen, österreichische Anbieter und lokaler Metadaten-Cache.
- `src/data/films.json`: lokale Filmsammlung mit 400 Filmen.
- `src/web/index.html`, `src/web/styles.css`, `src/web/app.js`: Browseroberfläche und lokale Browserzustände.
- `test/FilmSammlungTest.java`, `test/FilmMetadatenServiceTest.java`: Filter-, Katalog-, Cache-, API- und Fehlerfalltests.
- `pom.xml`: Java-Version, Maven-Abhängigkeiten, Quellen- und Ressourcenstruktur.

## Backend und Frontend

Der Browser lädt HTML, CSS und JavaScript vom lokalen Java-Server. Die Filmfilter werden im Backend gegen `src/data/films.json` ausgewertet. Gesehene IDs und Favoriten werden unabhängig voneinander im Browser-`localStorage` gehalten. Das Backend liefert Katalogstatistik und optionale TMDB-/OMDb-Metadaten an die Oberfläche.

## Externe Dienste und Zugangsdaten

- `TMDB_API_KEY` ist der serverseitige TMDB API Read Access Token. Er wird für Poster, Metadaten, Bewertungen von TMDB und Watch Providers für Region `AT` verwendet.
- `OMDB_API_KEY` ist der optionale OMDb-Schlüssel für externe Ratings und gegebenenfalls Poster-Fallback.
- Schlüssel ausschließlich über Umgebungsvariablen in der IntelliJ-Run-Konfiguration oder der lokalen Shell setzen. Niemals Schlüssel in JavaScript, HTML, CSS, versionierte Konfigurationsdateien oder Dokumentationsbeispiele mit echtem Wert schreiben.
- `.mamma-movie-cache/metadata-cache.json` enthält lokale Cache-Daten und ist ignoriert. Keine Cache-Datei mit persönlichen oder geheimen Daten committen.
- Externe Daten sind optional: API-Fehler dürfen lokale Filmempfehlungen nicht verhindern.

## Änderungsregeln

- Bestehende Empfehlung, Filter, Favoriten, Gesehen-Status, Fallbacks und API-Routen erhalten; Funktionen nur mit konkretem Grund ändern oder entfernen.
- Keine neuen Frameworks, Datenbanken oder externen APIs ohne ausdrücklichen Projektauftrag hinzufügen.
- Änderungen an der UI in den vorhandenen Dateien halten und die bestehende Struktur wiederverwenden.
- Katalogänderungen in `src/data/films.json` vornehmen; eindeutige IDs und realistische Pflichtangaben sicherstellen.
- Nach Änderungen `mvn test` ausführen. Für einen vollständigen Build `mvn clean verify` verwenden. Bei Änderungen an `src/web/app.js` zusätzlich `node --check src/web/app.js` ausführen.
- Keine Secrets, lokalen IntelliJ-Zugangsdaten oder unnötigen Cache-/Build-Dateien in die Dokumentation oder ins Git aufnehmen.
