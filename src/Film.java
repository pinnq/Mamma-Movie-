import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/** Stabile lokale Filmdaten; optionale API-Metadaten werden separat gespeichert. */
public class Film {
    private String id;
    private String title;
    private String apiTitle;
    private int year;
    private List<String> genres = new ArrayList<>();
    private int duration;
    private List<String> moods = new ArrayList<>();

    public Film() {
        // Jackson liest den lokalen Katalog über diesen Konstruktor ein.
    }

    public Film(String id, String title, String apiTitle, int year, List<String> genres,
                int duration, List<String> moods) {
        this.id = id;
        this.title = title;
        this.apiTitle = apiTitle;
        this.year = year;
        this.genres = new ArrayList<>(genres);
        this.duration = duration;
        this.moods = new ArrayList<>(moods);
    }

    /** Kompatibilitätskonstruktor für die ursprüngliche Filmklasse. */
    public Film(String titel, String genre, int dauer, String stimmung) {
        this(null, titel, titel, 0, List.of(genre), dauer, List.of(stimmung));
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getApiTitle() { return apiTitle == null || apiTitle.isBlank() ? title : apiTitle; }
    public void setApiTitle(String apiTitle) { this.apiTitle = apiTitle; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres == null ? new ArrayList<>() : genres; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public List<String> getMoods() { return moods; }
    public void setMoods(List<String> moods) { this.moods = moods == null ? new ArrayList<>() : moods; }

    /** Das Kataloggenre, mit dem die bisherige API kompatibel bleibt. */
    @JsonIgnore
    public String getGenre() { return genres.isEmpty() ? "" : genres.get(0); }

    /** Alter Getter aus der Konsolenversion. */
    @JsonIgnore
    public String getTitel() { return title; }

    /** Das erste Kataloggenre im bisherigen Datenformat. */
    @JsonIgnore
    public String getGenreDisplay() { return String.join(" · ", genres); }

    @JsonIgnore
    public int getDauer() { return duration; }

    @JsonIgnore
    public String getStimmung() { return moods.isEmpty() ? "" : moods.get(0); }

    @Override
    public String toString() {
        return title + " (" + year + ") | " + String.join(", ", genres) + " | " + duration + " Minuten";
    }
}
