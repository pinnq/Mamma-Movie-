import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lädt und cached externe Filmdaten, ohne die lokale Empfehlung zu blockieren. */
public class FilmMetadatenService {
    private static final long CACHE_DAUER_MILLIS = Duration.ofDays(30).toMillis();
    private static final long WATCH_PROVIDER_CACHE_MILLIS = Duration.ofHours(24).toMillis();
    private static final String TMDB_BILDER = "https://image.tmdb.org/t/p/";
    private static final String WATCH_PROVIDER_REGION = "AT";
    private static final Map<String, String> WATCH_PROVIDER_KATEGORIEN = Map.of(
            "flatrate", "streaming", "free", "free", "ads", "ads", "rent", "rent", "buy", "buy");

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String tmdbToken;
    private final String omdbApiKey;
    private final String tmdbApiUrl;
    private final String omdbApiUrl;
    private final Path cacheDatei;
    private final Map<String, CacheEintrag> cache = new HashMap<>();

    public FilmMetadatenService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build(),
                System.getenv("TMDB_API_KEY"), System.getenv("OMDB_API_KEY"),
                Path.of(".mamma-movie-cache", "metadata-cache.json"),
                "https://api.themoviedb.org/3", "https://www.omdbapi.com/");
    }

    /** Constructor mit austauschbaren Endpunkten für lokale Integrationstests. */
    FilmMetadatenService(HttpClient httpClient, String tmdbToken, String omdbApiKey,
                         Path cacheDatei, String tmdbApiUrl, String omdbApiUrl) {
        this.httpClient = httpClient;
        this.tmdbToken = tmdbToken == null ? "" : tmdbToken.trim();
        this.omdbApiKey = omdbApiKey == null ? "" : omdbApiKey.trim();
        this.cacheDatei = cacheDatei;
        this.tmdbApiUrl = tmdbApiUrl.replaceAll("/$", "");
        this.omdbApiUrl = omdbApiUrl;
        ladeCache();
    }

    public synchronized FilmMetadaten fuerFilm(Film film) {
        CacheEintrag vorhanden = cache.get(film.getId());
        long jetzt = System.currentTimeMillis();
        if (vorhanden != null && jetzt - vorhanden.getFetchedAt() < CACHE_DAUER_MILLIS) {
            return vorhanden.getMetadaten();
        }
        if (tmdbToken.isBlank() && omdbApiKey.isBlank()) {
            return new FilmMetadaten();
        }

        FilmMetadaten metadaten = vorhanden == null ? new FilmMetadaten() : vorhanden.getMetadaten();
        boolean providerSuccess = false;
        if (!tmdbToken.isBlank()) {
            providerSuccess |= versuche(() -> reicherAnMitTmdb(film, metadaten));
        }
        if (!omdbApiKey.isBlank()) {
            providerSuccess |= versuche(() -> reicherAnMitOmdb(film, metadaten));
        }
        if (providerSuccess) {
            cache.put(film.getId(), new CacheEintrag(jetzt, metadaten));
            speichereCache();
        }
        return metadaten;
    }

    /** Lädt Österreich-Anbieter nur bei Bedarf und hält sie kürzer als Filmbasisdaten frisch. */
    public synchronized FilmMetadaten fuerFilmMitAnbietern(Film film) {
        FilmMetadaten metadaten = fuerFilm(film);
        if (tmdbToken.isBlank() || metadaten.getTmdbId() == null || metadaten.getTmdbId().isBlank()) {
            return metadaten;
        }

        CacheEintrag eintrag = cache.get(film.getId());
        if (eintrag == null) {
            return metadaten;
        }
        long jetzt = System.currentTimeMillis();
        if (jetzt - eintrag.getWatchProvidersFetchedAt() < WATCH_PROVIDER_CACHE_MILLIS) {
            return metadaten;
        }

        try {
            URI uri = URI.create(tmdbApiUrl + "/movie/" + metadaten.getTmdbId() + "/watch/providers");
            JsonNode region = sendeTmdb(uri).path("results").path(WATCH_PROVIDER_REGION);
            metadaten.setWatchProvidersLink(region.path("link").asText(null));
            Map<String, List<FilmWatchProvider>> anbieter = new LinkedHashMap<>();
            WATCH_PROVIDER_KATEGORIEN.forEach((tmdbKategorie, uiKategorie) -> {
                List<FilmWatchProvider> liste = new ArrayList<>();
                region.path(tmdbKategorie).forEach(provider -> {
                    String name = provider.path("provider_name").asText("");
                    if (name.isBlank()) {
                        return;
                    }
                    String logoPath = provider.path("logo_path").asText("");
                    String logoUrl = logoPath.startsWith("/") ? TMDB_BILDER + "w92" + logoPath : null;
                    liste.add(new FilmWatchProvider(name, logoUrl));
                });
                if (!liste.isEmpty()) {
                    anbieter.put(uiKategorie, liste);
                }
            });
            metadaten.setWatchProviders(anbieter);
            eintrag.setMetadaten(metadaten);
            eintrag.setWatchProvidersFetchedAt(jetzt);
            speichereCache();
        } catch (IOException exception) {
            System.err.println("Österreichische Streaminginformationen konnten nicht geladen werden.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Laden der Streaminginformationen wurde unterbrochen.");
        }
        return metadaten;
    }

    private void reicherAnMitTmdb(Film film, FilmMetadaten metadaten) throws IOException, InterruptedException {
        String query = kodieren(film.getApiTitle());
        URI suchUri = URI.create(tmdbApiUrl + "/search/movie?query=" + query
                + "&year=" + film.getYear() + "&language=en-US&include_adult=false");
        JsonNode suchAntwort = sendeTmdb(suchUri);
        JsonNode ergebnisse = suchAntwort.path("results");
        JsonNode treffer = findeJahrestreffer(ergebnisse, film.getYear());
        if (treffer == null || !treffer.hasNonNull("id")) {
            return;
        }

        String tmdbId = treffer.path("id").asText();
        metadaten.setTmdbId(tmdbId);
        URI detailUri = URI.create(tmdbApiUrl + "/movie/" + tmdbId + "?language=en-US");
        JsonNode details = sendeTmdb(detailUri);
        String posterPath = details.path("poster_path").asText("");
        String backdropPath = details.path("backdrop_path").asText("");
        if (!posterPath.isBlank()) {
            metadaten.setPosterUrl(TMDB_BILDER + "w500" + posterPath);
        }
        if (!backdropPath.isBlank()) {
            metadaten.setBackdropUrl(TMDB_BILDER + "w1280" + backdropPath);
        }
        if (!details.path("overview").asText().isBlank()) {
            metadaten.setOverview(details.path("overview").asText());
        }
        if (details.path("runtime").canConvertToInt() && details.path("runtime").asInt() > 0) {
            metadaten.setDuration(details.path("runtime").asInt());
        }
        String releaseDate = details.path("release_date").asText("");
        if (releaseDate.length() >= 4) {
            try {
                metadaten.setYear(Integer.parseInt(releaseDate.substring(0, 4)));
            } catch (NumberFormatException ignored) {
                // Die lokale Jahresangabe bleibt als Fallback erhalten.
            }
        }
        List<String> genres = new ArrayList<>();
        details.path("genres").forEach(genre -> {
            if (genre.hasNonNull("name")) {
                genres.add(genre.path("name").asText());
            }
        });
        if (!genres.isEmpty()) {
            metadaten.setGenres(genres);
        }
        String imdbId = details.path("imdb_id").asText("");
        if (!imdbId.isBlank()) {
            metadaten.setImdbId(imdbId);
        }
        double tmdbRating = details.path("vote_average").asDouble(0);
        if (tmdbRating > 0) {
            metadaten.getRatings().removeIf(rating -> rating.getSource().equals("TMDB"));
            metadaten.getRatings().add(new FilmBewertung("TMDB", String.format("%.1f / 10", tmdbRating), "audience"));
        }

        if (metadaten.getPosterUrl() == null || metadaten.getPosterUrl().isBlank()) {
            ladePosterAusBilderEndpoint(tmdbId, metadaten);
        }
    }

    private void ladePosterAusBilderEndpoint(String tmdbId, FilmMetadaten metadaten) {
        URI bilderUri = URI.create(tmdbApiUrl + "/movie/" + tmdbId
                + "/images?include_image_language=en,null");
        try {
            JsonNode bilderAntwort = sendeTmdb(bilderUri);
            for (JsonNode poster : bilderAntwort.path("posters")) {
                String posterPath = poster.path("file_path").asText("");
                if (posterPath.startsWith("/")) {
                    metadaten.setPosterUrl(TMDB_BILDER + "w500" + posterPath);
                    return;
                }
            }
        } catch (IOException exception) {
            System.err.println("TMDB-Posterbilder konnten nicht geladen werden; Filmdaten bleiben verfügbar.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Laden der TMDB-Posterbilder wurde unterbrochen.");
        }
    }

    private void reicherAnMitOmdb(Film film, FilmMetadaten metadaten) throws IOException, InterruptedException {
        String url = omdbApiUrl + (omdbApiUrl.contains("?") ? "&" : "?")
                + "apikey=" + kodieren(omdbApiKey)
                + "&t=" + kodieren(film.getApiTitle())
                + "&y=" + film.getYear()
                + "&type=movie&plot=short";
        JsonNode antwort = sende(URI.create(url));
        if (!antwoordOk(antwort)) {
            return;
        }
        String posterUrl = antwort.path("Poster").asText("");
        if ((metadaten.getPosterUrl() == null || metadaten.getPosterUrl().isBlank())
                && !posterUrl.isBlank() && !"N/A".equalsIgnoreCase(posterUrl)) {
            metadaten.setPosterUrl(posterUrl);
        }
        if (metadaten.getOverview() == null && !antwort.path("Plot").asText().isBlank()
                && !"N/A".equalsIgnoreCase(antwort.path("Plot").asText())) {
            metadaten.setOverview(antwort.path("Plot").asText());
        }
        String imdbId = antwort.path("imdbID").asText("");
        if (!imdbId.isBlank() && !"N/A".equalsIgnoreCase(imdbId)) {
            metadaten.setImdbId(imdbId);
        }
        antwort.path("Ratings").forEach(rating -> {
            String source = rating.path("Source").asText("");
            String value = rating.path("Value").asText("");
            if (source.isBlank() || value.isBlank() || "N/A".equalsIgnoreCase(value)) {
                return;
            }
            String category = switch (source) {
                case "Internet Movie Database" -> "audience";
                case "Metacritic" -> "critics";
                default -> null;
            };
            metadaten.getRatings().removeIf(existing -> existing.getSource().equalsIgnoreCase(source));
            metadaten.getRatings().add(new FilmBewertung(source, value, category));
        });
    }

    private JsonNode sendeTmdb(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + tmdbToken)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("TMDB returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private JsonNode sende(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(3))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Movie metadata provider returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body());
    }

    private boolean antwoordOk(JsonNode antwoord) {
        return antwoord.path("Response").asText("False").equalsIgnoreCase("True");
    }

    private JsonNode findeJahrestreffer(JsonNode ergebnisse, int jahr) {
        for (JsonNode treffer : ergebnisse) {
            String datum = treffer.path("release_date").asText("");
            if (datum.length() >= 4 && datum.startsWith(String.valueOf(jahr))) {
                return treffer;
            }
        }
        return null;
    }

    private String kodieren(String wert) {
        return URLEncoder.encode(wert, StandardCharsets.UTF_8);
    }

    private boolean versuche(ProviderAufruf aufruf) {
        try {
            aufruf.ausfuehren();
            return true;
        } catch (IOException exception) {
            System.err.println("Externe Filmdaten konnten nicht geladen werden. Der lokale Film bleibt verfügbar.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Laden der Filmdaten wurde unterbrochen.");
        }
        return false;
    }

    private void ladeCache() {
        if (!Files.isRegularFile(cacheDatei)) {
            return;
        }
        try {
            Map<String, CacheEintrag> gespeicherterCache = mapper.readValue(cacheDatei.toFile(), new TypeReference<>() { });
            cache.putAll(gespeicherterCache);
        } catch (IOException exception) {
            System.err.println("Der Metadaten-Cache konnte nicht gelesen werden; ein neuer Cache wird erstellt.");
        }
    }

    private void speichereCache() {
        try {
            Files.createDirectories(cacheDatei.getParent());
            mapper.writerWithDefaultPrettyPrinter().writeValue(cacheDatei.toFile(), cache);
        } catch (IOException exception) {
            System.err.println("Der Metadaten-Cache konnte nicht gespeichert werden.");
        }
    }

    @FunctionalInterface
    private interface ProviderAufruf {
        void ausfuehren() throws IOException, InterruptedException;
    }

    public static class CacheEintrag {
        private long fetchedAt;
        private long watchProvidersFetchedAt;
        private FilmMetadaten metadaten;

        public CacheEintrag() { }
        public CacheEintrag(long fetchedAt, FilmMetadaten metadaten) {
            this.fetchedAt = fetchedAt;
            this.metadaten = metadaten;
        }
        public long getFetchedAt() { return fetchedAt; }
        public void setFetchedAt(long fetchedAt) { this.fetchedAt = fetchedAt; }
        public long getWatchProvidersFetchedAt() { return watchProvidersFetchedAt; }
        public void setWatchProvidersFetchedAt(long watchProvidersFetchedAt) {
            this.watchProvidersFetchedAt = watchProvidersFetchedAt;
        }
        public FilmMetadaten getMetadaten() { return metadaten == null ? new FilmMetadaten() : metadaten; }
        public void setMetadaten(FilmMetadaten metadaten) { this.metadaten = metadaten; }
    }
}
