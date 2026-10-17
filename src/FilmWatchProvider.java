/** Ein von TMDB für ein Land und eine Nutzungsart gemeldeter Anbieter. */
public class FilmWatchProvider {
    private String name;
    private String logoUrl;

    public FilmWatchProvider() { }

    public FilmWatchProvider(String name, String logoUrl) {
        this.name = name;
        this.logoUrl = logoUrl;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
}
