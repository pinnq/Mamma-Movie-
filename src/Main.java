import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/** Startpunkt der lokalen Webanwendung Mamma Movie! */
public class Main {
    private static final int DEFAULT_PORT = 8080;
    private static final List<String> GENRES = List.of(
            "Action", "Comedy", "Drama", "Horror", "Thriller", "Science-Fiction",
            "Fantasy", "Romance", "Adventure", "Animation");
    private static final List<String> STIMMUNGEN = List.of(
            "lustig", "spannend", "traurig", "entspannt", "düster", "romantisch");
    private static final ObjectMapper JSON = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        FilmSammlung sammlung = new FilmSammlung();
        FilmMetadatenService metadatenService = new FilmMetadatenService();
        int port = lesePort();
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/", exchange -> bearbeiteAnfrage(exchange, sammlung, metadatenService));
        server.start();
        System.out.println("Mamma Movie! läuft auf Port " + port);
        if (System.getenv("TMDB_API_KEY") == null || System.getenv("TMDB_API_KEY").isBlank()) {
            System.out.println("TMDB_API_KEY ist nicht gesetzt. Poster und externe Metadaten bleiben optional.");
        }
        if (System.getenv("OMDB_API_KEY") == null || System.getenv("OMDB_API_KEY").isBlank()) {
            System.out.println("OMDB_API_KEY ist nicht gesetzt. OMDb-Bewertungen bleiben optional.");
        }
    }

    private static void bearbeiteAnfrage(HttpExchange exchange, FilmSammlung sammlung,
                                         FilmMetadatenService metadatenService) throws IOException {
        String pfad = exchange.getRequestURI().getPath();
        switch (pfad) {
            case "/api/catalog" -> antworteKatalog(exchange, sammlung);
            case "/api/stats" -> antworteStatistik(exchange, sammlung);
            case "/api/recommend" -> bearbeiteEmpfehlung(exchange, sammlung, metadatenService);
            case "/api/surprise" -> bearbeiteUeberraschung(exchange, sammlung, metadatenService);
            case "/api/watch-providers" -> liefereWatchProviders(exchange, sammlung, metadatenService);
            default -> liefereWebdatei(exchange, pfad);
        }
    }

    private static void liefereWebdatei(HttpExchange exchange, String pfad) throws IOException {
        String dateiname;
        String inhaltstyp;
        switch (pfad) {
            case "/", "/index.html" -> {
                dateiname = "index.html";
                inhaltstyp = "text/html; charset=utf-8";
            }
            case "/styles.css" -> {
                dateiname = "styles.css";
                inhaltstyp = "text/css; charset=utf-8";
            }
            case "/app.js" -> {
                dateiname = "app.js";
                inhaltstyp = "text/javascript; charset=utf-8";
            }
            case "/manifest.webmanifest" -> {
                dateiname = "manifest.webmanifest";
                inhaltstyp = "application/manifest+json; charset=utf-8";
            }
            case "/favicon.svg", "/icons/app-icon.svg" -> {
                dateiname = "icons/app-icon.svg";
                inhaltstyp = "image/svg+xml; charset=utf-8";
            }
            case "/icons/icon-180.png" -> {
                dateiname = "icons/icon-180.png";
                inhaltstyp = "image/png";
            }
            case "/icons/icon-192.png" -> {
                dateiname = "icons/icon-192.png";
                inhaltstyp = "image/png";
            }
            case "/icons/icon-512.png" -> {
                dateiname = "icons/icon-512.png";
                inhaltstyp = "image/png";
            }
            default -> {
                antworte(exchange, 404, "text/plain; charset=utf-8", "Seite nicht gefunden.");
                return;
            }
        }

        try (var inhalt = Main.class.getResourceAsStream("/web/" + dateiname)) {
            if (inhalt == null) {
                antworte(exchange, 500, "text/plain; charset=utf-8", "Die Webdatei " + dateiname + " fehlt.");
                return;
            }
            byte[] bytes = inhalt.readAllBytes();
            antworte(exchange, 200, inhaltstyp, bytes);
        }
    }

    private static void antworteKatalog(HttpExchange exchange, FilmSammlung sammlung) throws IOException {
        if (!getAnfrage(exchange)) return;
        ObjectNode json = JSON.createObjectNode();
        json.put("totalMovies", sammlung.getAnzahlFilme());
        json.set("genres", JSON.valueToTree(GENRES));
        json.set("moods", JSON.valueToTree(STIMMUNGEN));
        antworteJson(exchange, 200, json);
    }

    private static void antworteStatistik(HttpExchange exchange, FilmSammlung sammlung) throws IOException {
        if (!getAnfrage(exchange)) return;
        Map<String, String> parameter = liesParameter(exchange.getRequestURI().getRawQuery());
        Set<String> ids = teileIds(parameter.getOrDefault("seenIds", ""));
        Set<String> bekannteIds = sammlung.getFilme().stream().map(Film::getId).collect(Collectors.toSet());
        long geseheneFilme = ids.stream().filter(bekannteIds::contains).count();
        ObjectNode json = JSON.createObjectNode();
        json.put("totalMovies", sammlung.getAnzahlFilme());
        json.put("seenMovies", geseheneFilme);
        json.put("unseenMovies", sammlung.getAnzahlFilme() - geseheneFilme);
        antworteJson(exchange, 200, json);
    }

    private static void liefereWatchProviders(HttpExchange exchange, FilmSammlung sammlung,
                                               FilmMetadatenService metadatenService) throws IOException {
        if (!getAnfrage(exchange)) return;
        Map<String, String> parameter = liesParameter(exchange.getRequestURI().getRawQuery());
        String filmId = parameter.getOrDefault("movieId", "");
        Film film = sammlung.getFilme().stream()
                .filter(eintrag -> eintrag.getId().equals(filmId))
                .findFirst().orElse(null);
        if (film == null) {
            antworteFehler(exchange, 404, "Der Film wurde im lokalen Katalog nicht gefunden.");
            return;
        }
        FilmMetadaten metadaten = metadatenService.fuerFilmMitAnbietern(film);
        ObjectNode antwort = JSON.createObjectNode();
        antwort.set("watchProviders", baueWatchProviderAntwort(metadaten));
        antworteJson(exchange, 200, antwort);
    }

    private static void bearbeiteEmpfehlung(HttpExchange exchange, FilmSammlung sammlung,
                                             FilmMetadatenService metadatenService) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            antworteFehler(exchange, 405, "Für Empfehlungen wird POST verwendet.");
            return;
        }

        JsonNode anfrage;
        try {
            byte[] daten = exchange.getRequestBody().readNBytes(64 * 1024);
            anfrage = JSON.readTree(daten);
        } catch (IOException exception) {
            antworteFehler(exchange, 400, "Die Filter konnten nicht gelesen werden.");
            return;
        }
        if (anfrage == null) {
            antworteFehler(exchange, 400, "Bitte gib deine Filmfilter an.");
            return;
        }

        String genre = anfrage.path("genre").asText("");
        String stimmung = anfrage.path("mood").asText("");
        int maximaleDauer = anfrage.path("maxRuntimeMinutes").asInt(0);
        if (!enthaeltOhneGrossKleinschreibung(GENRES, genre)
                || !enthaeltOhneGrossKleinschreibung(STIMMUNGEN, stimmung)
                || maximaleDauer < 1 || maximaleDauer > 500) {
            antworteFehler(exchange, 400, "Wähle ein gültiges Genre, eine Stimmung und eine Laufzeit von 1 bis 500 Minuten.");
            return;
        }

        Set<String> geseheneIds = liesIdArray(anfrage.path("seenMovieIds"));
        Set<String> kuerzlichVorgeschlagen = liesIdArray(anfrage.path("recentlySuggestedIds"));
        List<Film> passendeFilme = sammlung.findeAllePassendenFilme(genre, maximaleDauer, stimmung);
        ObjectNode antwort = JSON.createObjectNode();
        if (passendeFilme.isEmpty()) {
            antwort.put("status", "NO_MATCH");
        } else {
            List<Film> ungeseheneFilme = passendeFilme.stream()
                    .filter(film -> !enthaeltOhneGrossKleinschreibung(geseheneIds, film.getId()))
                    .toList();
            if (ungeseheneFilme.isEmpty()) {
                antwort.put("status", "ALL_MATCHING_SEEN");
            } else {
                Film film = sammlung.findePassendenFilm(genre, maximaleDauer, stimmung,
                        geseheneIds, kuerzlichVorgeschlagen);
                if (film == null) {
                    antwort.put("status", "NO_MORE_UNSEEN_SUGGESTIONS");
                } else {
                    antwort.put("status", "MATCH");
                    antwort.set("film", baueFilmAntwort(film, metadatenService.fuerFilmMitAnbietern(film),
                            genre, maximaleDauer, stimmung));
                }
            }
        }
        antworteJson(exchange, 200, antwort);
    }

    private static void bearbeiteUeberraschung(HttpExchange exchange, FilmSammlung sammlung,
                                                FilmMetadatenService metadatenService) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            antworteFehler(exchange, 405, "Für Überraschungsvorschläge wird POST verwendet.");
            return;
        }
        JsonNode anfrage;
        try {
            anfrage = JSON.readTree(exchange.getRequestBody().readNBytes(64 * 1024));
        } catch (IOException exception) {
            antworteFehler(exchange, 400, "Der Wunsch nach einer Überraschung konnte nicht gelesen werden.");
            return;
        }
        if (anfrage == null) {
            antworteFehler(exchange, 400, "Bitte versuche es noch einmal.");
            return;
        }

        Set<String> geseheneIds = liesIdArray(anfrage.path("seenMovieIds"));
        Set<String> kuerzlichVorgeschlagen = liesIdArray(anfrage.path("recentlySuggestedIds"));
        Film film = sammlung.findeUeberraschungsfilm(geseheneIds, kuerzlichVorgeschlagen);
        ObjectNode antwort = JSON.createObjectNode();
        if (film == null) {
            antwort.put("status", "ALL_MOVIES_SEEN");
        } else {
            antwort.put("status", "MATCH");
            antwort.set("film", baueFilmAntwort(film, metadatenService.fuerFilmMitAnbietern(film),
                    "Eine zufällige Überraschung aus dem gesamten Katalog."));
        }
        antworteJson(exchange, 200, antwort);
    }

    private static ObjectNode baueFilmAntwort(Film film, FilmMetadaten metadaten,
                                              String genre, int maximaleDauer, String stimmung) {
        return baueFilmAntwort(film, metadaten, "Passt zu " + genre + ", " + stimmung
                + " und deiner Obergrenze von " + maximaleDauer + " Minuten.");
    }

    private static ObjectNode baueFilmAntwort(Film film, FilmMetadaten metadaten, String empfehlungsgrund) {
        ObjectNode json = JSON.createObjectNode();
        json.put("id", film.getId());
        json.put("title", film.getTitle());
        json.put("year", metadaten.getYear() == null ? film.getYear() : metadaten.getYear());
        json.put("duration", metadaten.getDuration() == null ? film.getDuration() : metadaten.getDuration());
        json.set("genres", JSON.valueToTree(metadaten.getGenres().isEmpty() ? film.getGenres() : metadaten.getGenres()));
        json.set("moods", JSON.valueToTree(film.getMoods()));
        json.put("posterUrl", metadaten.getPosterUrl());
        json.put("backdropUrl", metadaten.getBackdropUrl());
        json.put("overview", metadaten.getOverview());
        json.put("recommendationReason", empfehlungsgrund);
        ArrayNode ratings = json.putArray("ratings");
        for (FilmBewertung bewertung : metadaten.getRatings()) {
            ObjectNode rating = ratings.addObject();
            rating.put("source", bewertung.getSource());
            rating.put("value", bewertung.getValue());
            if (bewertung.getCategory() != null) {
                rating.put("category", bewertung.getCategory());
            }
        }
        json.set("watchProviders", baueWatchProviderAntwort(metadaten));
        return json;
    }

    private static ObjectNode baueWatchProviderAntwort(FilmMetadaten metadaten) {
        ObjectNode watchProviders = JSON.createObjectNode();
        watchProviders.put("loaded", metadaten.isWatchProvidersLoaded());
        if (metadaten.getWatchProvidersLink() != null && !metadaten.getWatchProvidersLink().isBlank()) {
            watchProviders.put("link", metadaten.getWatchProvidersLink());
        }
        ObjectNode kategorien = watchProviders.putObject("categories");
        metadaten.getWatchProviders().forEach((kategorie, anbieter) -> {
            ArrayNode liste = kategorien.putArray(kategorie);
            for (FilmWatchProvider eintrag : anbieter) {
                ObjectNode anbieterJson = liste.addObject();
                anbieterJson.put("name", eintrag.getName());
                anbieterJson.put("logoUrl", eintrag.getLogoUrl());
            }
        });
        return watchProviders;
    }

    private static int lesePort() {
        String wert = System.getenv("PORT");
        if (wert == null || wert.isBlank()) return DEFAULT_PORT;
        try {
            int port = Integer.parseInt(wert.trim());
            if (port < 1 || port > 65535) throw new IllegalArgumentException("PORT muss zwischen 1 und 65535 liegen.");
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("PORT muss eine gültige Zahl sein.", exception);
        }
    }

    private static Set<String> liesIdArray(JsonNode array) {
        Set<String> ids = new HashSet<>();
        if (array.isArray()) {
            array.forEach(wert -> {
                if (wert.isTextual() && wert.asText().length() <= 80) {
                    ids.add(wert.asText());
                }
            });
        }
        return ids;
    }

    private static Set<String> teileIds(String ids) {
        if (ids == null || ids.isBlank()) return Set.of();
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .collect(Collectors.toSet());
    }

    private static boolean getAnfrage(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) return true;
        antworteFehler(exchange, 405, "Für diese Route wird GET verwendet.");
        return false;
    }

    private static Map<String, String> liesParameter(String query) {
        if (query == null || query.isBlank()) return Map.of();
        return Arrays.stream(query.split("&"))
                .map(teil -> teil.split("=", 2))
                .collect(Collectors.toMap(
                        paar -> URLDecoder.decode(paar[0], StandardCharsets.UTF_8),
                        paar -> paar.length > 1 ? URLDecoder.decode(paar[1], StandardCharsets.UTF_8) : "",
                        (ersterWert, ignoriert) -> ersterWert));
    }

    private static boolean enthaeltOhneGrossKleinschreibung(List<String> optionen, String wert) {
        return optionen.stream().anyMatch(option -> option.equalsIgnoreCase(wert));
    }

    private static boolean enthaeltOhneGrossKleinschreibung(Set<String> optionen, String wert) {
        return optionen.stream().anyMatch(option -> option.equalsIgnoreCase(wert));
    }

    private static void antworteFehler(HttpExchange exchange, int status, String meldung) throws IOException {
        ObjectNode fehler = JSON.createObjectNode();
        fehler.put("error", meldung);
        antworteJson(exchange, status, fehler);
    }

    private static void antworteJson(HttpExchange exchange, int status, JsonNode antwort) throws IOException {
        antworte(exchange, status, "application/json; charset=utf-8", JSON.writeValueAsBytes(antwort));
    }

    private static void antworte(HttpExchange exchange, int status, String inhaltstyp, byte[] bytes)
            throws IOException {
        exchange.getResponseHeaders().set("Content-Type", inhaltstyp);
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void antworte(HttpExchange exchange, int status, String inhaltstyp, String inhalt)
            throws IOException {
        antworte(exchange, status, inhaltstyp, inhalt.getBytes(StandardCharsets.UTF_8));
    }
}
