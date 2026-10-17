/** Ein unveränderter Wert einer benannten Bewertungsquelle. */
public class FilmBewertung {
    private String source;
    private String value;
    private String category;

    public FilmBewertung() { }

    public FilmBewertung(String source, String value, String category) {
        this.source = source;
        this.value = value;
        this.category = category;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
