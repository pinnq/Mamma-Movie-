# Mamma Movie!

## Projektbeschreibung

**Mamma Movie!** ist eine lokale Movie-Recommendation-Web-App. Sie hilft dabei, aus einer selbst gepflegten Filmsammlung einen Film passend zu Genre, maximaler Laufzeit und Stimmung auszuwählen. Das Projekt ist für den persönlichen Filmabend gedacht und läuft als Browseroberfläche mit einem Java-Backend unter <http://localhost:8080>.

Der Katalog umfasst aktuell **400 Filme**. Die Empfehlungslogik filtert nach allen drei ausgewählten Kriterien und schließt lokal als gesehen gespeicherte Filme aus. „Noch einen Film“ schlägt einen weiteren Treffer zu denselben Filtern vor; „Überrasch mich“ wählt zufällig aus dem gesamten ungesehenen Katalog.

## Technologien

- Java 17 und Maven
- Java `HttpServer` als lokaler Webserver, Jackson für JSON
- HTML, CSS und Vanilla JavaScript für die Browseroberfläche
- JSON-Datei als lokaler Filmkatalog; keine Datenbank
- TMDB für Poster, Filmdaten und österreichische Streaminganbieter; OMDb optional für externe Bewertungen

## Hauptfunktionen

- Genre, maximale Laufzeit und Stimmung wählen und passende Filme empfehlen lassen
- Weitere Vorschläge mit denselben Filtern anfordern oder einen zufälligen ungesehenen Film entdecken
- Filme nach Bestätigung als gesehen markieren; der Status bleibt in diesem Browser gespeichert und wird bei Empfehlungen berücksichtigt
- Filme unabhängig vom Gesehen-Status zu Favoriten hinzufügen; „Meine Favoriten“ bleibt nach einem Refresh erhalten und erlaubt das Entfernen
- TMDB-Poster, Bewertungen nach Quelle und – falls TMDB Daten für Österreich liefert – Streaming-, Leih- und Kaufanbieter anzeigen
- Filter zurücksetzen, ohne Gesehen-Status oder Favoriten zu löschen
- Verständliche Meldung bei fehlenden Treffern oder wenn alle passenden Filme gesehen wurden

## Funktionsweise

`Main` startet den Server auf `localhost:8080`, liefert die Webdateien aus und stellt Katalog-, Statistik-, Empfehlungs- und Überraschungsrouten bereit. `FilmSammlung` liest `src/data/films.json` und wendet die lokalen Filter an. `FilmMetadatenService` ergänzt auf Anfrage optionale TMDB-/OMDb-Daten und cached Metadaten lokal. Wenn externe Dienste nicht erreichbar oder nicht konfiguriert sind, bleiben die lokalen Filmangaben und Empfehlungen verfügbar.

Die Browseroberfläche speichert Gesehen-IDs unter `mammaMovie.seenMovieIds` und Favoriten-Snapshots getrennt davon unter `mammaMovie.favoriteMovies` in `localStorage`. Die Filter werden im Backend mit dem lokalen Katalog ausgewertet; zuletzt vorgeschlagene Filme werden, soweit Alternativen bestehen, vermieden.

## Projekt in IntelliJ starten

1. Öffne den bestehenden Projektordner in IntelliJ IDEA.
2. Öffne `pom.xml` und wähle **Add as Maven Project**, falls IntelliJ das nicht automatisch anbietet. Maven lädt Jackson; JUnit wird für Tests verwendet.
3. Optional: Öffne **Run → Edit Configurations… → Main** und trage unter **Environment variables** die gewünschten API-Zugangsdaten ein.
4. Starte `Main.main()` (JDK 17 oder neuer) und öffne anschließend <http://localhost:8080> in Chrome oder Chromium.

Ohne die jeweils benötigten API-Zugangsdaten funktionieren Katalog, Filter, Überraschung, Favoriten und der lokal gespeicherte Gesehen-Status weiterhin. Externe Poster, Ratings und österreichische Streaminginformationen sind optional; wenn keine Bildquelle verfügbar ist, zeigt die App den lokalen Platzhalter.

## API-Schlüssel

Die App liest Schlüssel ausschließlich im Java-Backend aus Umgebungsvariablen. Schlüssel nicht in HTML, JavaScript, versionierte Konfigurationsdateien oder Git eintragen. Beide Variablen sind optional; ohne sie funktioniert die lokale Empfehlung weiter, externe Daten bleiben dann gegebenenfalls leer.

- `TMDB_API_KEY`: TMDB **API Read Access Token** (Bearer Token) aus den API-Einstellungen deines TMDB-Kontos.
- `OMDB_API_KEY`: persönlicher Schlüssel von OMDb. OMDb liefert je nach Film IMDb-, Metacritic- und Rotten-Tomatoes-Werte; fehlende Quellen werden nicht ersetzt oder geschätzt.

In IntelliJ können die Variablen in der Run Configuration gesetzt werden, zum Beispiel `TMDB_API_KEY=…;OMDB_API_KEY=…` (Feldformat abhängig von IntelliJ-Version und Betriebssystem). Danach `Main` neu starten. Die Anwendung gibt die Schlüssel nicht aus und sendet sie nicht an den Browser.

## Daten und Cache

- Der lokale Filmkatalog liegt in `src/data/films.json` und enthält 400 Filme mit stabiler lokaler ID, Titel, Jahr, Genres, Laufzeit und kuratierten Stimmungen.
- Gesehene IDs werden im Browser unter `localStorage`-Schlüssel `mammaMovie.seenMovieIds` gespeichert. **Gesehene Filme zurücksetzen** löscht diese lokale Liste nach Bestätigung.
- Favoriten-Filmsnapshots werden unabhängig vom Gesehen-Status unter `mammaMovie.favoriteMovies` lokal im Browser gespeichert.
- TMDB-/OMDb-Metadaten werden 30 Tage, österreichische Watch-Provider-Daten 24 Stunden in `.mamma-movie-cache/metadata-cache.json` gecacht. Dieser Ordner ist in `.gitignore` eingetragen.
- Poster und Backdrops werden über von TMDB zurückgegebene Bildpfade geladen; Posterdateien werden nicht ins Projekt kopiert.
- Streaming-Verfügbarkeit kommt ausschließlich aus TMDBs Region `AT`; JustWatch wird als Quelle ausgewiesen.

TMDB verlangt bei Nutzung der API eine Attribution und den Hinweis, dass TMDB das Produkt nicht unterstützt oder zertifiziert. Der Hinweis und der TMDB-Link stehen im Fußbereich der Web-App. Siehe [TMDB Attribution](https://developer.themoviedb.org/docs/faq). Ratingquellen erscheinen getrennt; Kritiker-/Publikumslabels nur, wenn die Quelle diese Einordnung liefert.

## Tests

`mvn test` führt die Katalog-, Filter-, Überraschungs-, Empfehlungs-, Cache-, Watch-Provider- und API-Fallback-Tests aus.

## Autor

Jan Magbual
