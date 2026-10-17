import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Lädt und filtert den lokalen Filmbestand. */
public class FilmSammlung {
    private final List<Film> filme;

    public FilmSammlung() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream katalog = FilmSammlung.class.getResourceAsStream("/data/films.json")) {
            if (katalog == null) {
                throw new IllegalStateException("Der lokale Filmkatalog /data/films.json fehlt.");
            }
            filme = mapper.readValue(katalog, new TypeReference<>() { });
            pruefeKatalog();
        } catch (IOException exception) {
            throw new IllegalStateException("Der lokale Filmkatalog konnte nicht gelesen werden.", exception);
        }
    }

    /** Konstruktor für gezielte Tests der Filterlogik. */
    public FilmSammlung(List<Film> filme) {
        this.filme = new ArrayList<>(filme);
    }

    /**
     * Liefert den nächsten Treffer. Die Methode erhält die Funktion des
     * ursprünglichen Konsolenprogramms und berücksichtigt jetzt alle Filmgenres.
     */
    public Film findePassendenFilm(String genre, int maximaleDauer, String stimmung,
                                    List<Film> bereitsVorgeschlagen) {
        for (Film film : filme) {
            if (passt(film, genre, maximaleDauer, stimmung) && !bereitsVorgeschlagen.contains(film)) {
                return film;
            }
        }
        return null;
    }

    /** Liefert den nächsten Film nach Genre, Laufzeit und Stimmung. */
    public Film findePassendenFilm(String genre, int maximaleDauer, String stimmung,
                                    Set<String> geseheneIds, Set<String> kuerzlichVorgeschlagen) {
        for (Film film : filme) {
            if (passt(film, genre, maximaleDauer, stimmung)
                    && !enthaeltOhneGrossKleinschreibung(geseheneIds, film.getId())
                    && !enthaeltOhneGrossKleinschreibung(kuerzlichVorgeschlagen, film.getId())) {
                return film;
            }
        }
        return null;
    }

    /** Liefert den nächsten Treffer, dessen Titel noch nicht vorgeschlagen wurde (alte Web-API). */
    public Film findePassendenFilmAusserTitel(String genre, int maximaleDauer, String stimmung,
                                               Set<String> bereitsVorgeschlageneTitel) {
        for (Film film : filme) {
            boolean schonVorgeschlagen = bereitsVorgeschlageneTitel.stream()
                    .anyMatch(titel -> titel.equalsIgnoreCase(film.getTitle()));
            if (passt(film, genre, maximaleDauer, stimmung) && !schonVorgeschlagen) {
                return film;
            }
        }
        return null;
    }

    public List<Film> findeAllePassendenFilme(String genre, int maximaleDauer, String stimmung) {
        return filme.stream().filter(film -> passt(film, genre, maximaleDauer, stimmung)).toList();
    }

    /** Wählt zufällig aus allen ungesehenen Filmen und vermeidet letzte Vorschläge, wenn möglich. */
    public Film findeUeberraschungsfilm(Set<String> geseheneIds, Set<String> kuerzlichVorgeschlagen) {
        List<Film> ungeseheneFilme = filme.stream()
                .filter(film -> !enthaeltOhneGrossKleinschreibung(geseheneIds, film.getId()))
                .toList();
        if (ungeseheneFilme.isEmpty()) {
            return null;
        }
        List<Film> neueFilme = ungeseheneFilme.stream()
                .filter(film -> !enthaeltOhneGrossKleinschreibung(kuerzlichVorgeschlagen, film.getId()))
                .toList();
        List<Film> auswahl = neueFilme.isEmpty() ? ungeseheneFilme : neueFilme;
        return auswahl.get(ThreadLocalRandom.current().nextInt(auswahl.size()));
    }

    public List<Film> getFilme() {
        return List.copyOf(filme);
    }

    public int getAnzahlFilme() {
        return filme.size();
    }

    private boolean passt(Film film, String genre, int maximaleDauer, String stimmung) {
        return film.getGenres().stream().anyMatch(eintrag -> eintrag.equalsIgnoreCase(genre))
                && film.getDuration() <= maximaleDauer
                && film.getMoods().stream().anyMatch(eintrag -> eintrag.equalsIgnoreCase(stimmung));
    }

    private boolean enthaeltOhneGrossKleinschreibung(Set<String> werte, String suchwert) {
        return werte.stream().anyMatch(wert -> wert.equalsIgnoreCase(suchwert));
    }

    private void pruefeKatalog() {
        Set<String> ids = new HashSet<>();
        Set<String> titel = new HashSet<>();
        Set<String> apiTitelUndJahr = new HashSet<>();
        for (Film film : filme) {
            if (film.getId() == null || film.getId().isBlank() || !ids.add(film.getId())) {
                throw new IllegalStateException("Film-IDs müssen vorhanden und eindeutig sein.");
            }
            if (film.getTitle() == null || film.getTitle().isBlank()
                    || !titel.add(film.getTitle().trim().toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("Filmtitel müssen vorhanden und eindeutig sein: " + film.getTitle());
            }
            String apiTitelUndJahrSchluessel = film.getApiTitle().trim().toLowerCase(Locale.ROOT)
                    + "|" + film.getYear();
            if (!apiTitelUndJahr.add(apiTitelUndJahrSchluessel)) {
                throw new IllegalStateException("Doppelter Film für Movie-Metadaten: " + film.getApiTitle());
            }
            if (film.getYear() < 1888 || film.getYear() > 2100
                    || film.getDuration() < 30 || film.getDuration() > 400
                    || film.getGenres().isEmpty() || film.getMoods().isEmpty()) {
                throw new IllegalStateException("Ungültige Pflichtdaten für Film " + film.getId());
            }
        }
    }
}
