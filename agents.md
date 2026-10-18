# Coding-Agent-Anweisungen: Mamma Movie!

## Projekt und Betrieb

Mamma Movie! ist eine Java-Web-App mit Browser-Frontend. Die öffentliche Render-Web-App ist unter <https://mamma-movie.onrender.com> erreichbar. Lokal wird sie aus IntelliJ IDEA gestartet und ist standardmäßig unter <http://localhost:8080> verfügbar. Für die Nutzung der öffentlichen Website benötigen Besucher keine Java-Installation oder IDE.

Das Backend verwendet Java 17, Maven, Jackson und den Java-`HttpServer`; das Frontend besteht aus HTML, CSS und Vanilla JavaScript. JUnit 5 wird für Tests verwendet. Der Server liest online den Port aus `PORT`, fällt lokal auf Port 8080 zurück und lauscht auf `0.0.0.0`.

## Projektstruktur

- `src/Main.java`: Programmeinstieg, HTTP-Server, API-Routen und Auslieferung der statischen Webdateien, des Manifests und der Icons.
- `src/Film.java`, `src/FilmSammlung.java`: Filmtyp, lokaler Katalog sowie Filter- und Empfehlungslogik.
- `src/FilmMetadaten.java`, `src/FilmBewertung.java`, `src/FilmWatchProvider.java`, `src/FilmMetadatenService.java`: externe Metadaten, Bewertungen, österreichische Anbieter und Cache.
- `src/data/films.json`: lokaler Katalog mit 600 Filmen. Vorhandene Filme und ihre IDs bei Katalogänderungen erhalten.
- `src/web/index.html`, `src/web/styles.css`, `src/web/app.js`: Browseroberfläche, responsive Darstellung und Browserzustände.
- `src/web/manifest.webmanifest`, `src/web/icons/`: PWA-Manifest und App-Icons. Es gibt keinen Service Worker.
- `test/FilmSammlungTest.java`, `test/FilmMetadatenServiceTest.java`: Tests für Katalog, Empfehlungen, Metadaten-Cache und API-Fallbacks.
- `pom.xml`: Maven-Abhängigkeiten, Java-Version und Build-Konfiguration.
- `Dockerfile`, `render.yaml`, `.dockerignore`: Render-Containerkonfiguration und Ausschlüsse für lokale Dateien.

## Backend, Frontend und Datenfluss

Der Browser lädt Oberfläche und statische Dateien vom Java-Server. Filmfilter und Empfehlungen laufen im Backend gegen den lokalen JSON-Katalog. Die vorhandenen API-Routen bedienen Katalog, Statistik, Empfehlungen, Überraschung und österreichische Watch Provider. Gesehene Film-IDs (`mammaMovie.seenMovieIds`) und Favoriten (`mammaMovie.favoriteMovies`) liegen unabhängig voneinander im `localStorage` des jeweiligen Browsers und Geräts.

TMDB liefert optionale Poster, Metadaten, Bewertungen und Watch Provider für Österreich (`AT`). Das Frontend darf nur Provider anzeigen, die das Backend von TMDB für diese Region erhält. Für bestehende Favoriten ohne gespeicherte Providerdaten kann es `/api/watch-providers?movieId=<lokale Film-ID>` verwenden. OMDb ist optional und liefert weitere Bewertungen. Externe Fehler dürfen Katalog und lokale Empfehlungen nicht verhindern.

## Environment Variables und Secrets

- `TMDB_API_KEY`: serverseitiger TMDB API Read Access Token für Metadaten und Provider.
- `OMDB_API_KEY`: optionaler Schlüssel für zusätzliche Bewertungen.
- Lokal werden die Variablen über IntelliJ Run Configuration oder Shell-Environment gesetzt. Online liegen sie als Render Environment Variables vor.
- Niemals Schlüsselwerte in Quellcode, Web-Frontend, JSON-Katalog, Dokumentation oder Git schreiben. In Dokumentation nur Variablennamen nennen.
- `.mamma-movie-cache/metadata-cache.json` enthält lokale Cache-Daten; keine Cache- oder privaten Konfigurationsdateien committen.
- `.dockerignore` muss `.idea`, Cache, Build-Ausgaben und Env-Dateien aus dem Docker-Kontext heraushalten.

## Render und PWA

Die öffentliche App läuft unter <https://mamma-movie.onrender.com>. `render.yaml` und `Dockerfile` konfigurieren den Render Web Service. Serveränderungen müssen weiterhin `PORT` verwenden und an `0.0.0.0` binden. Änderungen an Deployment- oder Render-Service-Einstellungen nur im Rahmen eines ausdrücklichen Auftrags vornehmen; keine Secrets in Blueprint-Dateien eintragen. Die responsive Oberfläche und die PWA-Metadaten/Icons in den bestehenden Webdateien pflegen. Ein Offline-Service-Worker ist nicht implementiert.

## Änderungs- und Testregeln

- Bestehende Empfehlungslogik, Filter, Favoriten, Gesehen-Status, Fallbacks und API-Routen erhalten; nichts davon ohne konkreten Grund entfernen.
- Keine neuen Frameworks, Datenbanken oder externen APIs ohne ausdrücklichen Projektauftrag hinzufügen.
- UI-Änderungen in `src/web/index.html`, `src/web/styles.css` und `src/web/app.js` halten.
- Katalogänderungen in `src/data/films.json` vornehmen, alle bisherigen Filme behalten und eindeutige IDs sowie vollständige Pflichtdaten sicherstellen.
- Nach Codeänderungen `mvn test` ausführen; für vollständige Prüfung `mvn clean verify`. Bei Änderungen an `src/web/app.js` zusätzlich `node --check src/web/app.js` ausführen.
- `README.md`, `features.md` und `agents.md` mit dem tatsächlichen Projektstand synchron halten.
- Das Prompt Diary gehört dem Nutzer: nicht überschreiben, löschen, rekonstruieren oder neu erstellen, außer der Nutzer beauftragt dies ausdrücklich.
- Keine Secrets, lokalen IntelliJ-Zugangsdaten oder unnötigen Cache-/Build-Dateien dokumentieren oder einchecken.
