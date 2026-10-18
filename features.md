# Funktionen von Mamma Movie!

Die Liste beschreibt die aktuell implementierte lokale und öffentliche Web-App.

1. **Genre-Filter:** Auswahl aus den unterstützten Filmgenres.
2. **Laufzeitfilter:** Maximale Filmdauer in Minuten angeben.
3. **Stimmungsfilter:** Filme passend zu einer gewünschten Stimmung auswählen.
4. **Filmempfehlung:** Das Backend sucht im lokalen Katalog nach Filmen, die alle gesetzten Filter erfüllen.
5. **Noch einen Film:** Einen weiteren Treffer anhand derselben Filter anfordern; bereits gesehene Filme werden ausgeschlossen.
6. **Wiederholungen vermeiden:** Die App bevorzugt, sofern möglich, einen anderen Film als den direkt zuvor vorgeschlagenen.
7. **Überrasch mich:** Einen zufälligen ungesehenen Film aus dem gesamten Katalog vorschlagen.
8. **Keine-Treffer-Meldungen:** Verständliche Hinweise anzeigen, wenn kein passender ungesehener Film mehr verfügbar ist.
9. **Schon gesehen:** Eine Bestätigung schützt davor, einen Film versehentlich als gesehen zu markieren.
10. **Gesehen-Status speichern:** Gesehene IDs bleiben nach einem Browser-Refresh lokal gespeichert.
11. **Gesehene Filme ausschließen:** Als gesehen markierte Filme werden aus Empfehlungen und Überraschungen herausgefiltert.
12. **Gesehen-Liste zurücksetzen:** Gespeicherte Gesehen-IDs nach Bestätigung löschen.
13. **Favoriten verwalten:** Filme unabhängig vom Gesehen-Status zu Favoriten hinzufügen und wieder entfernen.
14. **Meine Favoriten:** Favoriten mit Poster oder Fallback, Titel, Jahr, Genres, Laufzeit und verfügbaren Bewertungen ansehen.
15. **Favoriten lokal speichern:** Favoriten bleiben nach einem Refresh im Browser erhalten und werden nicht zwischen Geräten synchronisiert.
16. **TMDB-Filmdaten und Poster:** Verfügbare Metadaten und Filmplakate von TMDB laden; bei fehlendem Poster den lokalen Fallback zeigen.
17. **Bewertungen anzeigen:** TMDB- und verfügbare OMDb-Bewertungen nach Quelle getrennt anzeigen, ohne fehlende Werte zu erfinden.
18. **Streaming für Österreich:** Von TMDB zurückgelieferte Watch Provider für Region `AT` mit verfügbaren Streaming-, kostenlosen, Leih- oder Kaufoptionen anzeigen.
19. **Streaming in Favoriten:** Anbieterinformationen für gespeicherte Favoriten anzeigen und bei Bedarf über das Backend nachladen.
20. **Filter zurücksetzen:** Genre und Stimmung leeren sowie die maximale Laufzeit auf 120 Minuten setzen; Favoriten und Gesehen-Status bleiben erhalten.
21. **Katalogstatistik:** Gesamtzahl und lokal gespeicherte Gesehen- beziehungsweise ungesehene Filme anzeigen.
22. **600-Film-Katalog:** Lokale Filmsammlung mit Schwerpunkt auf neueren bekannten Filmen und populären Kultfilmen/Klassikern.
23. **Responsive Oberfläche:** Browseroberfläche für Smartphone, Tablet, Laptop und Desktop.
24. **PWA-Grundausstattung:** Web App Manifest, Theme-Farbe und App-Icons unterstützen das Hinzufügen zum Home-Bildschirm. Ein Offline-Service-Worker ist nicht vorhanden.
25. **Öffentliche Web-App:** Die App läuft über Render unter <https://mamma-movie.onrender.com> und kann ohne lokale Installation im Browser genutzt werden.
26. **Lokaler Betrieb:** Die Anwendung kann aus IntelliJ gestartet und unter <http://localhost:8080> verwendet werden.
27. **API-Fallbacks:** Bei fehlender TMDB-/OMDb-Verbindung bleiben lokaler Katalog und Empfehlungen verfügbar; externe Angaben können fehlen.
