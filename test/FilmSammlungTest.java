import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FilmSammlungTest {
    @Test
    void katalogHat600GueltigeFilmeAusAllenZehnGenresUndGuteFilterabdeckung() {
        FilmSammlung sammlung = new FilmSammlung();
        List<Film> filme = sammlung.getFilme();

        assertEquals(600, filme.size());
        assertEquals(600, filme.stream().map(Film::getId).collect(Collectors.toSet()).size());
        assertEquals(600, filme.stream().map(film -> normalisiere(film.getTitle())).collect(Collectors.toSet()).size());
        assertTrue(filme.stream().allMatch(film -> film.getYear() >= 1888 && film.getYear() <= 2100));
        assertTrue(filme.stream().allMatch(film -> film.getDuration() >= 30 && film.getDuration() <= 400));
        assertTrue(filme.stream().allMatch(film -> !film.getGenres().isEmpty() && !film.getMoods().isEmpty()));
        List<Film> ergaenzungen = filme.subList(400, 600);
        assertTrue(ergaenzungen.stream().filter(film -> film.getYear() >= 2010).count() >= 140);
        assertTrue(ergaenzungen.stream().filter(film -> film.getYear() < 2010).count() >= 60);
        assertTrue(ergaenzungen.stream().filter(film -> film.getYear() >= 2020).count() >= 40);

        Set<String> hauptGenres = Set.of("Action", "Comedy", "Drama", "Horror", "Thriller",
                "Science-Fiction", "Fantasy", "Romance", "Adventure", "Animation");
        Set<String> enthalteneGenres = filme.stream().flatMap(film -> film.getGenres().stream()).collect(Collectors.toSet());
        assertTrue(enthalteneGenres.containsAll(hauptGenres));

        Set<String> stimmungen = Set.of("lustig", "spannend", "traurig", "entspannt", "düster", "romantisch");
        for (String genre : hauptGenres) {
            for (String stimmung : stimmungen) {
                assertTrue(sammlung.findeAllePassendenFilme(genre, 150, stimmung).size() >= 3,
                        () -> "Zu wenige Treffer für " + genre + " / " + stimmung);
            }
        }
    }

    @Test
    void suchtNurFilmeDieGenreDauerUndStimmungErfuellen() {
        FilmSammlung sammlung = new FilmSammlung();
        List<Film> treffer = sammlung.findeAllePassendenFilme("Comedy", 120, "lustig");

        assertFalse(treffer.isEmpty());
        assertTrue(treffer.stream().allMatch(film -> film.getGenres().contains("Comedy")));
        assertTrue(treffer.stream().allMatch(film -> film.getDuration() <= 120));
        assertTrue(treffer.stream().allMatch(film -> film.getMoods().contains("lustig")));
    }

    @Test
    void gesehenUndKuerzlichVorgeschlagenWerdenAusgeschlossen() {
        FilmSammlung sammlung = new FilmSammlung();
        Film erster = sammlung.findePassendenFilm("Comedy", 120, "lustig", Set.of(), Set.of());
        assertNotNull(erster);

        Film naechster = sammlung.findePassendenFilm("Comedy", 120, "lustig", Set.of(), Set.of(erster.getId()));
        assertNotNull(naechster);
        assertNotEquals(erster.getId(), naechster.getId());

        Set<String> gesehen = sammlung.findeAllePassendenFilme("Comedy", 120, "lustig").stream()
                .map(Film::getId).collect(Collectors.toCollection(HashSet::new));
        assertNull(sammlung.findePassendenFilm("Comedy", 120, "lustig", gesehen, Set.of()));
    }

    @Test
    void ueberraschungIgnoriertFilterSchliesstGeseheneAusUndVermeidetLetzteFilme() {
        FilmSammlung sammlung = new FilmSammlung();
        Set<String> gesehen = Set.of("mm-001", "mm-002");
        Set<String> zuletzt = Set.of("mm-003");

        for (int versuch = 0; versuch < 30; versuch++) {
            Film film = sammlung.findeUeberraschungsfilm(gesehen, zuletzt);
            assertNotNull(film);
            assertFalse(gesehen.contains(film.getId()));
            assertFalse(zuletzt.contains(film.getId()));
        }
    }

    @Test
    void ueberraschungErlaubtLetztenFilmWennErDerEinzigeUngeseheneIst() {
        Film film = new Film("only", "Only Film", "Only Film", 2000,
                List.of("Drama"), 100, List.of("spannend"));
        FilmSammlung sammlung = new FilmSammlung(List.of(film));

        assertEquals(film, sammlung.findeUeberraschungsfilm(Set.of(), Set.of("only")));
        assertNull(sammlung.findeUeberraschungsfilm(Set.of("only"), Set.of()));
    }

    private String normalisiere(String titel) {
        return java.text.Normalizer.normalize(titel, java.text.Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }
}
