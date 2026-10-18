# Funktionen von Mamma Movie!

Die folgende Liste beschreibt den aktuellen Projektstand mit 600 Filmen.

1. **Genre auswählen:** Die Oberfläche bietet Action, Comedy, Drama, Horror, Thriller, Science-Fiction, Fantasy, Romance, Adventure und Animation.
2. **Maximale Laufzeit festlegen:** Die gewünschte Obergrenze wird in Minuten eingegeben.
3. **Stimmung auswählen:** Zur Auswahl stehen lustig, spannend, traurig, entspannt, düster und romantisch.
4. **Passende Filme finden:** Das Java-Backend berücksichtigt Genre, Laufzeit und Stimmung gemeinsam.
5. **Weiteren Film anfordern:** „Noch einen Film“ verwendet die aktuellen Filter, schließt gesehene Filme aus und vermeidet nach Möglichkeit den unmittelbar vorherigen Vorschlag.
6. **Kein-Treffer-Meldungen:** Die App unterscheidet fehlende Treffer, bereits gesehene passende Filme und erschöpfte ungesehene Vorschläge.
7. **Zufällige Überraschung:** „Überrasch mich“ wählt einen ungesehenen Film aus dem gesamten Katalog und meidet nach Möglichkeit zuletzt gezeigte Filme.
8. **Als gesehen markieren:** Die Markierung wird vor dem Speichern bestätigt.
9. **Gesehen-Status lokal speichern:** Gesehene Film-IDs liegen in `localStorage` und bleiben nach einem Refresh erhalten.
10. **Gesehene Filme ausschließen:** Als gesehen gespeicherte Filme werden aus passenden Empfehlungen und Überraschungen ausgeschlossen.
11. **Gesehen-Liste zurücksetzen:** Die lokale Liste kann nach Bestätigung gelöscht werden.
12. **Filter zurücksetzen:** Genre und Stimmung werden geleert und die Laufzeit auf 120 Minuten gesetzt; Favoriten und Gesehen-Status bleiben erhalten.
13. **TMDB-Filmmetadaten laden:** Bei konfiguriertem Backend-Schlüssel ergänzt TMDB unter anderem Poster, Backdrop, Beschreibung, Jahr, Laufzeit und TMDB-Bewertung.
14. **Poster-Fallback verwenden:** Fehlen Poster in den Filmdetails, prüft die App den TMDB-Movie-Images-Endpunkt. Bei fehlendem oder nicht ladbarem Bild erscheint der lokale MM-Platzhalter.
15. **Bewertungen nach Quelle anzeigen:** TMDB- und verfügbare OMDb-Angaben werden getrennt nach Quelle dargestellt; fehlende Bewertungen werden nicht erfunden.
16. **Österreichische Streaminganbieter anzeigen:** Die App fragt TMDB Watch Providers für die Region `AT` ab und zeigt zurückgelieferte Streaming-, kostenlose, werbefinanzierte, Leih- und Kaufoptionen samt TMDB-Logo, sofern vorhanden.
17. **Streamingquelle verlinken:** Der von TMDB gelieferte Verfügbarkeitslink kann geöffnet werden; ohne Providerdaten erscheint ein Hinweis.
18. **Favoriten verwalten:** Ein Film lässt sich unabhängig vom Gesehen-Status zu Favoriten hinzufügen oder daraus entfernen.
19. **„Meine Favoriten“ anzeigen:** Die eigene Favoritenliste zeigt Poster oder Fallback, Titel, Jahr, Genres, Laufzeit, verfügbare Bewertungen und gespeicherte österreichische Streaminganbieter. Favoriten bleiben nach einem Refresh erhalten.
20. **Favoriten-Streaming nachladen:** Bestehende lokale Favoriten ohne gespeicherte Providerdaten können die Daten über eine lokale Backend-Route nachladen; der Backend-Cache wird wiederverwendet.
21. **Statistik anzeigen:** Die Oberfläche zeigt Kataloggröße sowie lokal gesehene und noch offene Filme.
22. **Responsive Browseroberfläche:** Die Oberfläche passt sich Mobiltelefonen, Tablets, Laptops und Desktop-Bildschirmen an. Formulare und Aktionsbuttons sind mit Touch bedienbar.
23. **PWA zum Home-Bildschirm hinzufügen:** Web App Manifest, Theme-Farbe und lokale PNG-/SVG-Icons ermöglichen die Installation als Web-App. Es gibt keinen Offline-Service-Worker.
24. **Render-Deployment vorbereiten:** Dockerfile und Render Blueprint konfigurieren einen öffentlichen Web Service mit `PORT`, Health Check und extern gesetzten Secrets.
25. **Ohne externe API weiterarbeiten:** Katalog und lokale Empfehlungen funktionieren auch dann, wenn TMDB oder OMDb nicht konfiguriert beziehungsweise nicht erreichbar sind.
