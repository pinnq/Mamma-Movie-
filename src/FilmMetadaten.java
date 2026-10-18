import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Optionale dynamische Informationen von TMDB und OMDb. */
public class FilmMetadaten {
    private String posterUrl;
    private String backdropUrl;
    private String overview;
    private List<String> genres = new ArrayList<>();
    private Integer year;
    private Integer duration;
    private String tmdbId;
    private String imdbId;
    private Double tmdbRating;
    private List<FilmBewertung> ratings = new ArrayList<>();
    private String watchProvidersLink;
    private Map<String, List<FilmWatchProvider>> watchProviders = new LinkedHashMap<>();
    private boolean watchProvidersLoaded;

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
    public String getBackdropUrl() { return backdropUrl; }
    public void setBackdropUrl(String backdropUrl) { this.backdropUrl = backdropUrl; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres == null ? new ArrayList<>() : genres; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }
    public String getTmdbId() { return tmdbId; }
    public void setTmdbId(String tmdbId) { this.tmdbId = tmdbId; }
    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }
    public Double getTmdbRating() { return tmdbRating; }
    public void setTmdbRating(Double tmdbRating) { this.tmdbRating = tmdbRating; }
    public List<FilmBewertung> getRatings() { return ratings; }
    public void setRatings(List<FilmBewertung> ratings) { this.ratings = ratings == null ? new ArrayList<>() : ratings; }
    public String getWatchProvidersLink() { return watchProvidersLink; }
    public void setWatchProvidersLink(String watchProvidersLink) { this.watchProvidersLink = watchProvidersLink; }
    public Map<String, List<FilmWatchProvider>> getWatchProviders() { return watchProviders; }
    public void setWatchProviders(Map<String, List<FilmWatchProvider>> watchProviders) {
        this.watchProviders = watchProviders == null ? new LinkedHashMap<>() : new LinkedHashMap<>(watchProviders);
    }
    public boolean isWatchProvidersLoaded() { return watchProvidersLoaded; }
    public void setWatchProvidersLoaded(boolean watchProvidersLoaded) { this.watchProvidersLoaded = watchProvidersLoaded; }
}
