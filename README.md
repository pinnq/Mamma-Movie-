# Mamma Movie!

## Projektbeschreibung

**Mamma Movie!** ist eine Movie-Recommendation-Web-App für alle, die für den nächsten Filmabend schnell einen passenden Film finden möchten. Nutzerinnen und Nutzer wählen Genre, maximale Laufzeit und Stimmung; das Java-Backend sucht dann passende Filme aus einem lokalen Katalog mit rund **600 Filmen**. Die Auswahl enthält viele neuere bekannte Filme sowie populäre Kultfilme und Klassiker.

Das Projekt richtet sich an Filmfans und gelegentliche Zuschauer, die ihre Filmsuche nach ihrer aktuellen Stimmung und verfügbaren Zeit filtern möchten. Die Web-App läuft öffentlich und kann ohne IDE, Java-Installation oder lokalen Server im Browser verwendet werden.

## Online und lokal

| Betrieb | Adresse | Verwendung |
| --- | --- | --- |
| **Online** | <https://mamma-movie.onrender.com> | Öffentliche Render-Web-App; im Browser öffnen, keine lokale Installation nötig. |
| **Lokal** | <http://localhost:8080> | Entwicklungsbetrieb über IntelliJ IDEA und den lokalen Java-Server. |

## Technologien

- Java 17, Maven und Java `HttpServer` für das Backend
- Jackson für JSON-Verarbeitung
- HTML, CSS und Vanilla JavaScript für das Browser-Frontend
- JSON-Datei als lokaler Filmkatalog; keine Datenbank
- TMDB für Filmdaten, Poster, TMDB-Bewertungen und österreichische Watch-Provider-Daten
- OMDb optional für weitere Filmbewertungen
- Docker und Render für den öffentlichen Betrieb
- Web App Manifest und lokale Icons für die PWA-Installation

## Funktionen

- Filme nach Genre, maximaler Laufzeit und Stimmung filtern
- Einen passenden Film finden, weitere Treffer mit denselben Filtern anfordern oder mit „Überrasch mich“ einen zufälligen ungesehenen Film entdecken
- Filme nach Bestätigung als gesehen markieren; gesehene Filme werden bei Empfehlungen ausgeschlossen
- Favoriten unabhängig vom Gesehen-Status speichern, in „Meine Favoriten“ ansehen und wieder entfernen
- Poster, Metadaten und verfügbare TMDB-Bewertungen anzeigen; OMDb-Bewertungen werden angezeigt, sofern verfügbar
- Streaming-, Leih- und Kaufoptionen für Österreich anzeigen, wenn TMDB dafür Daten liefert
- Filter zurücksetzen, ohne Favoriten oder Gesehen-Status zu löschen
- Responsive Oberfläche für Smartphones, Tablets, Laptops und Desktop-Browser
- PWA-Manifest und Icons zum Hinzufügen der Website zum Home-Bildschirm

Favoriten und Gesehen-IDs werden getrennt voneinander in `localStorage` gespeichert. Sie bleiben in dem jeweiligen Browser und auf dem jeweiligen Gerät erhalten und werden nicht zwischen Geräten synchronisiert. Die PWA besitzt keinen Offline-Service-Worker; für Empfehlungen und externe Filmdaten ist eine Netzwerkverbindung erforderlich.

## Funktionsweise

`Main` startet den Java-Server lokal auf Port `8080` oder verwendet online den durch Render gesetzten `PORT`. Der Server bindet an `0.0.0.0`, liefert die Browserdateien aus und stellt API-Routen für Katalog, Statistik, Empfehlungen, Überraschung und österreichische Anbieter bereit. `FilmSammlung` liest `src/data/films.json` und wendet die lokalen Filter an. `FilmMetadatenService` lädt optionale TMDB- und OMDb-Daten und verwendet einen lokalen Cache.

Poster und Metadaten stammen aus TMDB, sofern sie für den jeweiligen Film verfügbar sind. Streamingdaten werden nur für Österreich (`AT`) angefragt und nur angezeigt, wenn TMDB Anbieter zurückliefert. Die Bewertungsquellen werden getrennt dargestellt. Bei fehlenden API-Schlüsseln oder externen API-Fehlern funktionieren lokale Filmdaten und Empfehlungen weiterhin; nicht verfügbare externe Angaben werden ausgelassen oder durch den lokalen Poster-Fallback ersetzt.

## Lokal in IntelliJ starten

1. Öffne den Projektordner in IntelliJ IDEA und lade `pom.xml` als Maven-Projekt.
2. Optional: Setze `TMDB_API_KEY` und/oder `OMDB_API_KEY` in **Run → Edit Configurations… → Main → Environment variables**.
3. Starte `Main.main()` mit JDK 17 oder neuer.
4. Öffne <http://localhost:8080> im Browser.

Ohne API-Zugangsdaten funktionieren der Filmkatalog und die lokale Empfehlungslogik weiterhin; externe Poster, Bewertungen und Streaminginformationen können dann fehlen.

## Environment Variables und Secrets

API-Schlüssel werden ausschließlich serverseitig über Environment Variables eingebunden. Echte Schlüssel gehören weder in den Quellcode noch in HTML, CSS, JavaScript, Dokumentation oder Git.

- `TMDB_API_KEY`: erforderlich, um TMDB-Daten wie Poster, Metadaten, Bewertungen und österreichische Anbieter abzurufen.
- `OMDB_API_KEY`: optional; liefert zusätzliche Bewertungen, sofern für den Film verfügbar.

Für den Online-Betrieb werden die Variablen in der Render-Service-Konfiguration als Secrets gesetzt. Lokal können sie in der IntelliJ-Run-Konfiguration oder in der Shell gesetzt werden.

## Render-Betrieb

Die öffentliche Web-App wird über Render bereitgestellt und ist unter **<https://mamma-movie.onrender.com>** erreichbar. Nutzer benötigen nur einen aktuellen Browser. Der Render-Dienst verwendet den im Container bereitgestellten `PORT`; lokal bleibt Port `8080` der Standard. Für den Render-Build liegen `Dockerfile` und `render.yaml` im Projekt.

## Tests

`mvn clean verify` führt die Maven-Tests aus und prüft unter anderem den Filmkatalog, Filterlogik, Cache, Watch Provider und Fehlerfälle externer Daten. Für die JavaScript-Syntaxprüfung kann zusätzlich `node --check src/web/app.js` verwendet werden.

## Autor

Jan Magbual
