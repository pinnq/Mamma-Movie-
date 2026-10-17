import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class FilmMetadatenServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void laedtBewertungenPosterUndCacheSieOhneWeitereAufrufe() throws Exception {
        AtomicInteger aufrufe = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/3/search/movie", exchange -> antworte(exchange, aufrufe,
                "{\"results\":[{\"id\":42,\"release_date\":\"2001-01-01\"}] }"));
        server.createContext("/3/movie/42", exchange -> antworte(exchange, aufrufe,
                "{\"runtime\":118,\"poster_path\":\"/poster.jpg\",\"backdrop_path\":\"/backdrop.jpg\","
                        + "\"overview\":\"A movie\",\"release_date\":\"2001-01-01\",\"imdb_id\":\"tt123\","
                        + "\"vote_average\":8.1,\"genres\":[{\"name\":\"Drama\"}]}"));
        server.createContext("/", exchange -> antworte(exchange, aufrufe,
                "{\"Response\":\"True\",\"imdbID\":\"tt123\",\"Ratings\":["
                        + "{\"Source\":\"Internet Movie Database\",\"Value\":\"8.4/10\"},"
                        + "{\"Source\":\"Metacritic\",\"Value\":\"82/100\"},"
                        + "{\"Source\":\"Rotten Tomatoes\",\"Value\":\"91%\"}]}"));
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            Path cache = tempDir.resolve("cache.json");
            Film film = new Film("mm-test", "Test Film", "Test Film", 2001,
                    List.of("Drama"), 117, List.of("spannend"));
            FilmMetadatenService service = neuerService(cache, base);

            FilmMetadaten metadata = service.fuerFilm(film);
            assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", metadata.getPosterUrl());
            assertEquals("https://image.tmdb.org/t/p/w1280/backdrop.jpg", metadata.getBackdropUrl());
            assertEquals(118, metadata.getDuration());
            assertEquals("A movie", metadata.getOverview());
            assertTrue(metadata.getRatings().stream().anyMatch(rating -> rating.getSource().equals("TMDB")));
            assertTrue(metadata.getRatings().stream().anyMatch(rating -> rating.getSource().equals("Metacritic")
                    && "critics".equals(rating.getCategory())));
            assertTrue(metadata.getRatings().stream().anyMatch(rating -> rating.getSource().equals("Internet Movie Database")
                    && "audience".equals(rating.getCategory())));
            assertEquals(3, aufrufe.get());

            service.fuerFilm(film);
            neuerService(cache, base).fuerFilm(film);
            assertEquals(3, aufrufe.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nutztMovieImagesEndpointWennDetailantwortKeinPosterEnthaelt() throws Exception {
        AtomicInteger aufrufe = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/3/search/movie", exchange -> antworte(exchange, aufrufe,
                "{\"results\":[{\"id\":42,\"release_date\":\"2017-02-24\"}] }"));
        server.createContext("/3/movie/42", exchange -> antworte(exchange, aufrufe,
                "{\"poster_path\":null,\"release_date\":\"2017-02-24\",\"vote_average\":7.1}"));
        server.createContext("/3/movie/42/images", exchange -> antworte(exchange, aufrufe,
                "{\"posters\":[{\"file_path\":\"/get-out-fallback.jpg\"}]}"));
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            Film film = new Film("mm-get-out", "Get Out", "Get Out", 2017,
                    List.of("Horror", "Thriller"), 104, List.of("spannend"));

            FilmMetadaten metadata = neuerService(tempDir.resolve("images-fallback-cache.json"), base,
                    "test-tmdb", "").fuerFilm(film);

            assertEquals("https://image.tmdb.org/t/p/w500/get-out-fallback.jpg", metadata.getPosterUrl());
            assertEquals("42", metadata.getTmdbId());
            assertTrue(metadata.getRatings().stream().anyMatch(rating -> rating.getSource().equals("TMDB")));
            assertEquals(3, aufrufe.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void laedtNurOesterreichischeWatchProviderUndCachedSieKurz() throws Exception {
        AtomicInteger aufrufe = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/3/search/movie", exchange -> antworte(exchange, aufrufe,
                "{\"results\":[{\"id\":99,\"release_date\":\"2020-01-01\"}]}"));
        server.createContext("/3/movie/99", exchange -> antworte(exchange, aufrufe,
                "{\"poster_path\":\"/poster.jpg\",\"release_date\":\"2020-01-01\"}"));
        server.createContext("/3/movie/99/watch/providers", exchange -> {
            assertEquals("Bearer test-tmdb", exchange.getRequestHeaders().getFirst("Authorization"));
            antworte(exchange, aufrufe,
                    "{\"results\":{"
                            + "\"AT\":{\"link\":\"https://www.themoviedb.org/movie/99/watch\","
                            + "\"flatrate\":[{\"provider_name\":\"Joyn Plus\",\"logo_path\":\"/joyn.png\"}],"
                            + "\"rent\":[{\"provider_name\":\"Apple TV Store\",\"logo_path\":\"/apple.png\"}]},"
                            + "\"DE\":{\"flatrate\":[{\"provider_name\":\"German Only Service\","
                            + "\"logo_path\":\"/de.png\"}]}}}");
        });
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            Film film = new Film("mm-palm", "Palm Springs", "Palm Springs", 2020,
                    List.of("Comedy", "Romance"), 90, List.of("lustig", "romantisch"));
            FilmMetadatenService service = neuerService(tempDir.resolve("provider-cache.json"), base,
                    "test-tmdb", "");

            FilmMetadaten metadata = service.fuerFilmMitAnbietern(film);
            assertEquals("https://www.themoviedb.org/movie/99/watch", metadata.getWatchProvidersLink());
            assertEquals("https://image.tmdb.org/t/p/w92/joyn.png",
                    metadata.getWatchProviders().get("streaming").get(0).getLogoUrl());
            assertEquals("Joyn Plus", metadata.getWatchProviders().get("streaming").get(0).getName());
            assertEquals("Apple TV Store", metadata.getWatchProviders().get("rent").get(0).getName());
            assertFalse(metadata.getWatchProviders().toString().contains("German Only Service"));
            assertEquals(3, aufrufe.get());

            service.fuerFilmMitAnbietern(film);
            assertEquals(3, aufrufe.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void providerAusfallLaesstLokaleEmpfehlungNutzbar() throws Exception {
        AtomicInteger aufrufe = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            aufrufe.incrementAndGet();
            byte[] body = "provider unavailable".getBytes();
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            Film film = new Film("mm-test", "Test Film", "Test Film", 2001,
                    List.of("Drama"), 117, List.of("spannend"));
            FilmMetadatenService service = neuerService(tempDir.resolve("failure-cache.json"), base);
            FilmMetadaten metadata = assertDoesNotThrow(() -> service.fuerFilm(film));

            assertNotNull(metadata);
            assertNull(metadata.getPosterUrl());
            assertTrue(metadata.getRatings().isEmpty());
            assertEquals(2, aufrufe.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void nutztOmdbPosterWennTmdbNichtErreichbarIst() throws Exception {
        AtomicInteger aufrufe = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/3/", exchange -> {
            aufrufe.incrementAndGet();
            byte[] body = "unavailable".getBytes();
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/", exchange -> antworte(exchange, aufrufe,
                "{\"Response\":\"True\",\"Poster\":\"https://m.media-amazon.com/poster.jpg\","
                        + "\"Ratings\":[]}"));
        server.start();
        try {
            String base = "http://localhost:" + server.getAddress().getPort();
            Film film = new Film("mm-test", "Test Film", "Test Film", 2001,
                    List.of("Drama"), 117, List.of("spannend"));
            FilmMetadaten metadata = neuerService(tempDir.resolve("omdb-poster-cache.json"), base).fuerFilm(film);
            assertEquals("https://m.media-amazon.com/poster.jpg", metadata.getPosterUrl());
            assertTrue(metadata.getRatings().isEmpty());
            assertEquals(2, aufrufe.get());
        } finally {
            server.stop(0);
        }
    }

    private FilmMetadatenService neuerService(Path cache, String base) {
        return neuerService(cache, base, "test-tmdb", "test-omdb");
    }

    private FilmMetadatenService neuerService(Path cache, String base, String tmdbToken, String omdbApiKey) {
        return new FilmMetadatenService(HttpClient.newHttpClient(), tmdbToken, omdbApiKey,
                cache, base + "/3", base + "/");
    }

    private void antworte(com.sun.net.httpserver.HttpExchange exchange, AtomicInteger calls, String body)
            throws java.io.IOException {
        calls.incrementAndGet();
        byte[] bytes = body.getBytes();
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
