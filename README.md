# Mamma Movie!

## Projektbeschreibung

**Mamma Movie!** ist eine Movie-Recommendation-Web-App für den persönlichen Filmabend. Sie schlägt aus einer lokalen Filmsammlung einen Film passend zu Genre, maximaler Laufzeit und Stimmung vor. Das Java-Backend kann lokal unter <http://localhost:8080> oder als öffentlicher Render Web Service laufen. Nutzer benötigen zum Verwenden nur einen Browser; es ist keine lokale Java-Installation erforderlich.

Der Katalog umfasst aktuell **600 Filme**. Die 200 Ergänzungen sind überwiegend populäre Filme: mindestens 140 stammen aus 2010 oder später, 60 sind bekannte ältere Publikumserfolge oder Klassiker; mindestens 40 der Ergänzungen stammen aus 2020 oder später. Die Empfehlungslogik filtert Genre, Stimmung und maximale Laufzeit gemeinsam und schließt lokal als gesehen gespeicherte Filme aus. „Noch einen Film“ schlägt einen weiteren Treffer zu denselben Filtern vor; „Überrasch mich“ wählt zufällig aus dem gesamten ungesehenen Katalog.

## Technologien

- Java 17 und Maven
- Java `HttpServer` als lokaler Webserver, Jackson für JSON
- HTML, CSS und Vanilla JavaScript für die Browseroberfläche
- JSON-Datei als lokaler Filmkatalog; keine Datenbank
- TMDB für Poster, Filmdaten und österreichische Streaminganbieter; OMDb optional für externe Bewertungen
- Docker und Render Blueprint für die öffentliche Bereitstellung
- Web App Manifest und lokale App-Icons für die Installation auf unterstützten Geräten

## Hauptfunktionen

- Genre, maximale Laufzeit und Stimmung wählen und passende Filme empfehlen lassen
- Weitere Vorschläge mit denselben Filtern anfordern oder einen zufälligen ungesehenen Film entdecken
- Filme nach Bestätigung als gesehen markieren; der Status bleibt in diesem Browser gespeichert und wird bei Empfehlungen berücksichtigt
- Filme unabhängig vom Gesehen-Status zu Favoriten hinzufügen; „Meine Favoriten“ bleibt nach einem Refresh erhalten und erlaubt das Entfernen
- TMDB-Poster, Bewertungen nach Quelle und – falls TMDB Daten für Österreich liefert – Streaming-, Leih- und Kaufanbieter anzeigen
- Filter zurücksetzen, ohne Gesehen-Status oder Favoriten zu löschen
- Verständliche Meldung bei fehlenden Treffern oder wenn alle passenden Filme gesehen wurden
- Responsive Oberfläche für Mobiltelefone, Tablets, Laptops und Desktop-Browser
- Installierbare PWA-Oberfläche; Filmempfehlungen und externe Daten benötigen weiterhin eine Netzwerkverbindung

## Funktionsweise

`Main` startet den Server lokal standardmäßig auf Port 8080 oder online auf dem über `PORT` gesetzten Port. Er lauscht auf `0.0.0.0`, liefert die Webdateien aus und stellt Katalog-, Statistik-, Empfehlungs-, Überraschungs- und Favoriten-Provider-Routen bereit. `FilmSammlung` liest `src/data/films.json` und wendet die lokalen Filter an. `FilmMetadatenService` ergänzt auf Anfrage optionale TMDB-/OMDb-Daten und cached Metadaten lokal. Wenn externe Dienste nicht erreichbar oder nicht konfiguriert sind, bleiben lokale Filmangaben und Empfehlungen verfügbar.

Die Browseroberfläche speichert Gesehen-IDs unter `mammaMovie.seenMovieIds` und Favoriten-Snapshots getrennt davon unter `mammaMovie.favoriteMovies` in `localStorage`. Diese Daten bleiben lokal je Browser und Gerät und werden nicht zwischen Geräten synchronisiert. Die Filter werden im Backend mit dem lokalen Katalog ausgewertet; zuletzt vorgeschlagene Filme werden, soweit Alternativen bestehen, vermieden. Streamingdaten werden nur aus TMDB-Ergebnissen für Österreich (`AT`) dargestellt. Für bestehende Favoriten ohne gespeicherte Streamingdaten fragt die Oberfläche die Backend-Route `/api/watch-providers` ab.

## Projekt in IntelliJ starten

1. Öffne den bestehenden Projektordner in IntelliJ IDEA.
2. Öffne `pom.xml` und wähle **Add as Maven Project**, falls IntelliJ das nicht automatisch anbietet. Maven lädt Jackson; JUnit wird für Tests verwendet.
3. Optional: Öffne **Run → Edit Configurations… → Main** und trage unter **Environment variables** die gewünschten API-Zugangsdaten ein.
4. Starte `Main.main()` (JDK 17 oder neuer) und öffne anschließend <http://localhost:8080> in Safari, Chrome, Chromium, Firefox oder Edge.

Ohne die jeweils benötigten API-Zugangsdaten funktionieren Katalog, Filter, Überraschung, Favoriten und der lokal gespeicherte Gesehen-Status weiterhin. Externe Poster, Ratings und österreichische Streaminginformationen sind optional; wenn keine Bildquelle verfügbar ist, zeigt die App den lokalen Platzhalter.

## API-Schlüssel

Die App liest Schlüssel ausschließlich im Java-Backend aus Umgebungsvariablen. Schlüssel nicht in HTML, JavaScript, versionierte Konfigurationsdateien oder Git eintragen. Beide Variablen sind optional; ohne sie funktioniert die lokale Empfehlung weiter, externe Daten bleiben dann gegebenenfalls leer.

- `TMDB_API_KEY`: TMDB **API Read Access Token** (Bearer Token) aus den API-Einstellungen deines TMDB-Kontos.
- `OMDB_API_KEY`: persönlicher Schlüssel von OMDb. OMDb liefert je nach Film IMDb-, Metacritic- und Rotten-Tomatoes-Werte; fehlende Quellen werden nicht ersetzt oder geschätzt.

In IntelliJ können die Variablen in der Run Configuration gesetzt werden, zum Beispiel `TMDB_API_KEY=…;OMDB_API_KEY=…` (Feldformat abhängig von IntelliJ-Version und Betriebssystem). Danach `Main` neu starten. Die Anwendung gibt die Schlüssel nicht aus und sendet sie nicht an den Browser.

## Daten und Cache

- Der lokale Filmkatalog liegt in `src/data/films.json` und enthält 600 Filme mit stabiler lokaler ID, Titel, Jahr, Genres, Laufzeit und kuratierten Stimmungen.
- Gesehene IDs werden im Browser unter `localStorage`-Schlüssel `mammaMovie.seenMovieIds` gespeichert. **Gesehene Filme zurücksetzen** löscht diese lokale Liste nach Bestätigung.
- Favoriten-Filmsnapshots werden unabhängig vom Gesehen-Status unter `mammaMovie.favoriteMovies` lokal im Browser gespeichert.
- TMDB-/OMDb-Metadaten werden 30 Tage, österreichische Watch-Provider-Daten 24 Stunden in `.mamma-movie-cache/metadata-cache.json` gecacht. Dieser Ordner ist in `.gitignore` eingetragen.
- Poster und Backdrops werden über von TMDB zurückgegebene Bildpfade geladen; Posterdateien werden nicht ins Projekt kopiert.
- Streaming-Verfügbarkeit kommt ausschließlich aus TMDBs Region `AT`; JustWatch wird als Quelle ausgewiesen.

TMDB verlangt bei Nutzung der API eine Attribution und den Hinweis, dass TMDB das Produkt nicht unterstützt oder zertifiziert. Der Hinweis und der TMDB-Link stehen im Fußbereich der Web-App. Siehe [TMDB Attribution](https://developer.themoviedb.org/docs/faq). Ratingquellen erscheinen getrennt; Kritiker-/Publikumslabels nur, wenn die Quelle diese Einordnung liefert.

## Tests

`mvn clean verify` prüft den 600-Film-Katalog, Filterabdeckung, Überraschungslogik, Metadaten-Cache, Watch Providers und API-Fallbacks. `node --check src/web/app.js` prüft zusätzlich die JavaScript-Syntax.

## Render-Deployment

Im Repository liegen `Dockerfile` und `render.yaml`. Render kann den Blueprint verwenden; eine Veröffentlichung wurde durch diese Projektänderung nicht ausgelöst. Die Render-Konfiguration fragt API-Schlüssel beim Einrichten als Secrets ab und schaltet automatische Deployments aus. Setze im Render-Dashboard:

- `TMDB_API_KEY` (für TMDB-Metadaten, Poster und Streamingdaten)
- `OMDB_API_KEY` (optional, für zusätzliche Bewertungen)

Der Container verwendet Java 17. Render stellt `PORT` bereit; lokal fällt die Anwendung auf Port 8080 zurück. `healthCheckPath` ist `/api/catalog`.

**Build im Dockerfile:**

```sh
mvn -B clean verify dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
```

**Start im Container:**

```sh
java -cp 'target/classes:target/dependency/*' Main
```

Für einen lokalen Container-Test kannst du Docker installieren und ausführen:

```sh
docker build -t mamma-movie .
docker run --rm -p 8080:8080 -e PORT=8080 -e TMDB_API_KEY mamma-movie
```

## Autor

Jan Magbual
