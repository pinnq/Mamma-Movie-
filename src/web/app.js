const STORAGE_KEY = "mammaMovie.seenMovieIds";
const FAVORITES_STORAGE_KEY = "mammaMovie.favoriteMovies";
const FALLBACK_GENRES = ["Action", "Comedy", "Drama", "Horror", "Thriller", "Science-Fiction", "Fantasy", "Romance", "Adventure", "Animation"];
const FALLBACK_MOODS = ["lustig", "spannend", "traurig", "entspannt", "düster", "romantisch"];

const form = document.querySelector("#movie-form");
const resultArea = document.querySelector("#result-area");
const movieCard = document.querySelector("#movie-card");
const emptyState = document.querySelector("#empty-state");
const errorMessage = document.querySelector("#error-message");
const anotherButton = document.querySelector("#another-button");
const surpriseButton = document.querySelector("#surprise-button");
const seenButton = document.querySelector("#seen-button");
const favoriteButton = document.querySelector("#favorite-button");
const changeFiltersButton = document.querySelector("#change-filters");
const resultCounter = document.querySelector("#result-counter");
const posterImage = document.querySelector("#movie-poster");
const posterFallback = document.querySelector("#poster-fallback");
const seenDialog = document.querySelector("#seen-confirmation");

let seenMovieIds = ladeGeseheneIds();
let favoriteMovies = ladeFavoriten();
let recentlySuggestedIds = [];
let suggestionNumber = 0;
let currentMovie = null;

document.addEventListener("DOMContentLoaded", initialisiere);

form.addEventListener("submit", (event) => {
    event.preventDefault();
    recentlySuggestedIds = [];
    suggestionNumber = 0;
    findeFilm("filtered");
});

anotherButton.addEventListener("click", () => findeFilm("filtered"));
surpriseButton.addEventListener("click", () => findeFilm("surprise"));
favoriteButton.addEventListener("click", schalteFavoritUm);

document.querySelector("#reset-filters").addEventListener("click", () => {
    document.querySelector("#genre").selectedIndex = 0;
    document.querySelector("#mood").selectedIndex = 0;
    document.querySelector("#duration").value = "120";
    recentlySuggestedIds = [];
    suggestionNumber = 0;
    resultArea.hidden = true;
    errorMessage.hidden = true;
    document.querySelector("#filter-panel").scrollIntoView({ behavior: "smooth", block: "center" });
});

seenButton.addEventListener("click", () => {
    if (!currentMovie) return;
    document.querySelector("#confirm-copy").textContent = `„${currentMovie.title}“ als gesehen markieren? Der Film wird dann bei künftigen Empfehlungen übersprungen.`;
    seenDialog.showModal();
});

document.querySelector("#confirm-seen").addEventListener("click", () => {
    if (!currentMovie) return;
    seenMovieIds.add(currentMovie.id);
    speichereGeseheneIds();
    seenDialog.close();
    seenButton.disabled = true;
    seenButton.innerHTML = '<span class="check-icon" aria-hidden="true">✓</span> Schon gesehen';
    ladeStatistik();
});

document.querySelector("#cancel-seen").addEventListener("click", () => seenDialog.close());

changeFiltersButton.addEventListener("click", () => {
    recentlySuggestedIds = [];
    suggestionNumber = 0;
    resultArea.hidden = true;
    form.querySelector("button[type='submit']").focus();
    document.querySelector("#filter-panel").scrollIntoView({ behavior: "smooth", block: "center" });
});

document.querySelector("#reset-seen").addEventListener("click", () => {
    if (seenMovieIds.size === 0) {
        window.alert("Deine Liste der gesehenen Filme ist bereits leer.");
        return;
    }
    if (!window.confirm("Möchtest du alle lokal gespeicherten gesehenen Filme zurücksetzen?")) return;
    seenMovieIds.clear();
    speichereGeseheneIds();
    if (currentMovie) {
        seenButton.disabled = false;
        seenButton.innerHTML = '<span class="check-icon" aria-hidden="true">✓</span> Schon gesehen';
    }
    ladeStatistik();
});

window.addEventListener("storage", (event) => {
    if (event.key === STORAGE_KEY) {
        seenMovieIds = ladeGeseheneIds();
        ladeStatistik();
    }
    if (event.key === FAVORITES_STORAGE_KEY) {
        favoriteMovies = ladeFavoriten();
        zeigeFavoriten();
        aktualisiereFavoritenButton();
    }
});

async function initialisiere() {
    const button = form.querySelector("button[type='submit']");
    button.disabled = true;
    await ladeKatalogOptionen();
    await ladeStatistik();
    zeigeFavoriten();
    button.disabled = false;
}

async function ladeKatalogOptionen() {
    try {
        const response = await fetch("/api/catalog");
        if (!response.ok) throw new Error("Katalog nicht erreichbar");
        const katalog = await response.json();
        fuelleAuswahl(document.querySelector("#genre"), katalog.genres || FALLBACK_GENRES, "Genre auswählen");
        fuelleAuswahl(document.querySelector("#mood"), katalog.moods || FALLBACK_MOODS, "Stimmung auswählen", true);
        document.querySelector("#total-count").textContent = katalog.totalMovies;
    } catch (error) {
        fuelleAuswahl(document.querySelector("#genre"), FALLBACK_GENRES, "Genre auswählen");
        fuelleAuswahl(document.querySelector("#mood"), FALLBACK_MOODS, "Stimmung auswählen", true);
        errorMessage.textContent = "Der lokale Katalog konnte nicht geladen werden. Bitte starte Mamma Movie! erneut.";
        errorMessage.hidden = false;
    }
}

function fuelleAuswahl(select, optionen, platzhalter, grossschreibung = false) {
    select.replaceChildren(new Option(platzhalter, "", true, true));
    select.options[0].disabled = true;
    optionen.forEach((option) => {
        const label = grossschreibung ? option.charAt(0).toUpperCase() + option.slice(1) : option;
        select.add(new Option(label, option));
    });
}

async function ladeStatistik() {
    document.querySelector("#seen-count").textContent = seenMovieIds.size;
    try {
        const ids = encodeURIComponent([...seenMovieIds].join(","));
        const response = await fetch(`/api/stats?seenIds=${ids}`);
        if (!response.ok) return;
        const statistik = await response.json();
        document.querySelector("#total-count").textContent = statistik.totalMovies;
        document.querySelector("#seen-count").textContent = statistik.seenMovies;
        document.querySelector("#unseen-count").textContent = statistik.unseenMovies;
    } catch (error) {
        // Der lokal gespeicherte Zähler bleibt sichtbar, falls der Server kurz nicht antwortet.
    }
}

async function findeFilm(mode = "filtered") {
    const submitButton = form.querySelector("button[type='submit']");
    const activeButton = mode === "surprise" ? surpriseButton
        : suggestionNumber === 0 ? submitButton : anotherButton;
    activeButton.disabled = true;
    errorMessage.hidden = true;
    resultArea.hidden = false;
    movieCard.hidden = true;
    emptyState.hidden = true;
    seenButton.hidden = true;
    favoriteButton.hidden = true;
    anotherButton.hidden = true;
    changeFiltersButton.hidden = true;
    resultCounter.textContent = "Suche läuft …";

    const request = {
        genre: document.querySelector("#genre").value,
        maxRuntimeMinutes: Number(document.querySelector("#duration").value),
        mood: document.querySelector("#mood").value,
        seenMovieIds: [...seenMovieIds],
        recentlySuggestedIds
    };

    try {
        const route = mode === "surprise" ? "/api/surprise" : "/api/recommend";
        const response = await fetch(route, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        });
        const answer = await response.json();
        if (!response.ok) throw new Error(answer.error || "Die Filmsuche ist gerade nicht verfügbar.");

        resultCounter.textContent = "";
        if (answer.status === "MATCH") {
            currentMovie = answer.film;
            zeigeFilm(answer.film);
            recentlySuggestedIds.push(answer.film.id);
            suggestionNumber += 1;
            resultCounter.textContent = `Vorschlag ${suggestionNumber}`;
            seenButton.disabled = seenMovieIds.has(answer.film.id);
            seenButton.hidden = false;
            seenButton.innerHTML = '<span class="check-icon" aria-hidden="true">✓</span> Schon gesehen';
            aktualisiereFavoritenButton();
            favoriteButton.hidden = false;
            anotherButton.hidden = false;
            changeFiltersButton.hidden = false;
        } else {
            zeigeLeermeldung(answer.status);
        }
        resultArea.scrollIntoView({ behavior: "smooth", block: "nearest" });
    } catch (error) {
        resultCounter.textContent = "";
        errorMessage.textContent = error.message;
        errorMessage.hidden = false;
        changeFiltersButton.hidden = false;
    } finally {
        activeButton.disabled = false;
    }
}

function zeigeFilm(film) {
    document.querySelector("#movie-title").textContent = film.title;
    document.querySelector("#year-badge").textContent = String(film.year);
    document.querySelector("#movie-meta").replaceChildren();
    [...film.genres, `${film.duration} Minuten`, ...film.moods.map(kapitalisiere)].forEach((wert, index) => {
        const pill = document.createElement("span");
        pill.className = index >= film.genres.length + 1 ? "meta-pill meta-pill-mood" : "meta-pill";
        pill.textContent = wert;
        document.querySelector("#movie-meta").append(pill);
    });
    const overview = document.querySelector("#movie-overview");
    overview.textContent = film.overview || "";
    overview.hidden = !film.overview;
    document.querySelector("#recommendation-reason").textContent = film.recommendationReason;
    const backdropUrl = sichereTmdbBildUrl(film.backdropUrl);
    movieCard.style.backgroundImage = backdropUrl
        ? `linear-gradient(90deg, rgb(26 29 23 / 97%) 0%, rgb(26 29 23 / 94%) 54%, rgb(26 29 23 / 82%) 100%), url("${backdropUrl}")`
        : "none";
    ladePoster(film.posterUrl, film.title);
    zeigeBewertungen(film.ratings || []);
    zeigeAnbieter(film.watchProviders || {});
    movieCard.hidden = false;
}

function ladePoster(url, title) {
    posterImage.hidden = true;
    posterImage.removeAttribute("src");
    posterImage.alt = `Offizielles Filmplakat zu ${title}`;
    posterFallback.hidden = false;
    const sichereUrl = sicherePosterUrl(url);
    if (!sichereUrl) return;
    posterImage.onload = () => {
        posterFallback.hidden = true;
        posterImage.hidden = false;
    };
    posterImage.onerror = () => {
        posterImage.hidden = true;
        posterFallback.hidden = false;
    };
    posterImage.src = sichereUrl;
}

function sichereTmdbBildUrl(url) {
    if (!url) return "";
    try {
        const parsed = new URL(url);
        return parsed.protocol === "https:" && parsed.hostname === "image.tmdb.org" ? parsed.href : "";
    } catch (error) {
        return "";
    }
}

function sicherePosterUrl(url) {
    if (!url) return "";
    try {
        const parsed = new URL(url);
        const erlaubteHosts = ["image.tmdb.org", "m.media-amazon.com"];
        return parsed.protocol === "https:" && erlaubteHosts.includes(parsed.hostname) ? parsed.href : "";
    } catch (error) {
        return "";
    }
}

function zeigeBewertungen(ratings) {
    const liste = document.querySelector("#ratings-list");
    liste.replaceChildren();
    ratings.forEach((rating) => {
        if (!rating.source || !rating.value) return;
        const card = document.createElement("div");
        card.className = "rating-card";
        const source = document.createElement("span");
        source.className = "rating-source";
        source.textContent = rating.source === "Internet Movie Database" ? "IMDb" : rating.source;
        const value = document.createElement("span");
        value.className = "rating-value";
        value.textContent = rating.value;
        card.append(source, value);
        if (rating.category === "critics" || rating.category === "audience") {
            const category = document.createElement("span");
            category.className = "rating-category";
            category.textContent = rating.category === "critics" ? "Kritiker" : "Publikum";
            card.append(category);
        }
        liste.append(card);
    });
    document.querySelector("#ratings-section").hidden = ratings.length === 0;
    document.querySelector("#no-ratings").hidden = ratings.length > 0;
}

function zeigeAnbieter(watchProviders) {
    const liste = document.querySelector("#providers-list");
    const section = document.querySelector("#providers-section");
    const noProviders = document.querySelector("#no-providers");
    const source = document.querySelector("#providers-source");
    const categories = watchProviders.categories || {};
    const labels = { streaming: "Streaming", free: "Kostenlos", ads: "Mit Werbung", rent: "Leihen", buy: "Kaufen" };
    let providerCount = 0;
    liste.replaceChildren();

    Object.entries(labels).forEach(([key, label]) => {
        const providers = Array.isArray(categories[key]) ? categories[key] : [];
        if (providers.length === 0) return;
        providerCount += providers.length;
        const group = document.createElement("section");
        group.className = "provider-group";
        const heading = document.createElement("h3");
        heading.className = "provider-category";
        heading.textContent = label;
        const entries = document.createElement("div");
        entries.className = "provider-entries";
        providers.forEach((provider) => {
            const link = sichereTmdbLink(watchProviders.link);
            const card = document.createElement(link ? "a" : "div");
            card.className = "provider-card";
            if (link) {
                card.href = link;
                card.target = "_blank";
                card.rel = "noopener noreferrer";
                card.setAttribute("aria-label", `${provider.name} bei TMDB / JustWatch öffnen`);
            }
            const logo = sichereTmdbBildUrl(provider.logoUrl);
            if (logo) {
                const image = document.createElement("img");
                image.src = logo;
                image.alt = "";
                image.loading = "lazy";
                image.onerror = () => image.remove();
                card.append(image);
            } else {
                const mark = document.createElement("span");
                mark.className = "provider-mark";
                mark.textContent = (provider.name || "?").slice(0, 1).toUpperCase();
                card.append(mark);
            }
            const name = document.createElement("span");
            name.textContent = provider.name || "Anbieter";
            card.append(name);
            entries.append(card);
        });
        group.append(heading, entries);
        liste.append(group);
    });

    section.hidden = providerCount === 0;
    noProviders.hidden = providerCount > 0;
    const sourceLink = sichereTmdbLink(watchProviders.link);
    source.hidden = providerCount === 0 || !sourceLink;
    if (sourceLink) source.href = sourceLink;
}

function sichereTmdbLink(url) {
    if (!url) return "";
    try {
        const parsed = new URL(url);
        return parsed.protocol === "https:" && parsed.hostname === "www.themoviedb.org" ? parsed.href : "";
    } catch (error) {
        return "";
    }
}

function ladeFavoriten() {
    try {
        const gespeichert = JSON.parse(localStorage.getItem(FAVORITES_STORAGE_KEY) || "[]");
        if (!Array.isArray(gespeichert)) return [];
        const eindeutig = new Map();
        gespeichert.forEach((film) => {
            if (film && typeof film.id === "string" && typeof film.title === "string") {
                eindeutig.set(film.id, film);
            }
        });
        return [...eindeutig.values()];
    } catch (error) {
        return [];
    }
}

function speichereFavoriten() {
    try {
        localStorage.setItem(FAVORITES_STORAGE_KEY, JSON.stringify(favoriteMovies));
    } catch (error) {
        errorMessage.textContent = "Der Browser konnte deine Favoriten nicht speichern. Prüfe die lokalen Speichereinstellungen.";
        errorMessage.hidden = false;
    }
}

function istFavorit(movieId) {
    return favoriteMovies.some((film) => film.id === movieId);
}

function aktualisiereFavoritenButton() {
    const isFavorite = currentMovie && istFavorit(currentMovie.id);
    favoriteButton.textContent = isFavorite ? "In Favoriten ✓" : "Zu Favoriten";
    favoriteButton.classList.toggle("is-favorite", Boolean(isFavorite));
}

function schalteFavoritUm() {
    if (!currentMovie) return;
    const index = favoriteMovies.findIndex((film) => film.id === currentMovie.id);
    if (index >= 0) {
        favoriteMovies.splice(index, 1);
    } else {
        const { watchProviders, recommendationReason, ...snapshot } = currentMovie;
        favoriteMovies.push(snapshot);
    }
    speichereFavoriten();
    zeigeFavoriten();
    aktualisiereFavoritenButton();
}

function entferneFavorit(movieId) {
    favoriteMovies = favoriteMovies.filter((film) => film.id !== movieId);
    speichereFavoriten();
    zeigeFavoriten();
    aktualisiereFavoritenButton();
}

function zeigeFavoriten() {
    const grid = document.querySelector("#favorites-grid");
    const empty = document.querySelector("#favorites-empty");
    grid.replaceChildren();
    document.querySelector("#favorites-count").textContent = `${favoriteMovies.length} Favoriten`;
    empty.hidden = favoriteMovies.length > 0;

    favoriteMovies.forEach((film) => {
        const card = document.createElement("article");
        card.className = "favorite-card";
        const poster = document.createElement("div");
        poster.className = "favorite-poster";
        const fallback = document.createElement("span");
        fallback.className = "favorite-poster-fallback";
        fallback.textContent = (film.title || "MM").slice(0, 2).toUpperCase();
        poster.append(fallback);
        const posterUrl = sicherePosterUrl(film.posterUrl);
        if (posterUrl) {
            const image = document.createElement("img");
            image.src = posterUrl;
            image.alt = `Filmplakat zu ${film.title}`;
            image.loading = "lazy";
            image.onload = () => { fallback.hidden = true; };
            image.onerror = () => image.remove();
            poster.append(image);
        }

        const details = document.createElement("div");
        details.className = "favorite-details";
        const title = document.createElement("h3");
        title.textContent = film.title;
        const year = document.createElement("span");
        year.className = "favorite-year";
        year.textContent = String(film.year || "");
        const meta = document.createElement("p");
        const genres = Array.isArray(film.genres) ? film.genres.join(" · ") : "";
        meta.textContent = [genres, film.duration ? `${film.duration} Min.` : ""].filter(Boolean).join(" · ");
        details.append(title, year, meta);
        const ratings = Array.isArray(film.ratings) ? film.ratings.slice(0, 2) : [];
        if (ratings.length > 0) {
            const ratingText = document.createElement("p");
            ratingText.className = "favorite-rating";
            ratingText.textContent = ratings.map((rating) => {
                const name = rating.source === "Internet Movie Database" ? "IMDb" : rating.source;
                return `${name} ${rating.value}`;
            }).join(" · ");
            details.append(ratingText);
        }
        const remove = document.createElement("button");
        remove.className = "favorite-remove";
        remove.type = "button";
        remove.textContent = "Entfernen";
        remove.setAttribute("aria-label", `${film.title} aus Favoriten entfernen`);
        remove.addEventListener("click", () => entferneFavorit(film.id));
        card.append(poster, details, remove);
        grid.append(card);
    });
}

function zeigeLeermeldung(status) {
    movieCard.hidden = true;
    emptyState.hidden = false;
    const title = document.querySelector("#empty-title");
    const copy = document.querySelector("#empty-copy");
    if (status === "ALL_MATCHING_SEEN") {
        title.textContent = "Du hast bereits alle passenden Filme gesehen.";
        copy.textContent = "Setze deine gesehenen Filme zurück oder ändere deine Filter, um neue Vorschläge zu entdecken.";
    } else if (status === "NO_MORE_UNSEEN_SUGGESTIONS") {
        title.textContent = "Das waren alle ungesehenen Treffer dieser Runde.";
        copy.textContent = "Ändere deine Filter oder markiere einen Film als gesehen, damit wir dir weitere Filme vorschlagen können.";
    } else if (status === "ALL_MOVIES_SEEN") {
        title.textContent = "Du hast bereits alle Filme gesehen.";
        copy.textContent = "Setze deine gesehenen Filme zurück, wenn du wieder eine Überraschung möchtest.";
    } else {
        title.textContent = "Für diese Auswahl haben wir gerade keinen ungesehenen Film gefunden.";
        copy.textContent = "Tipp: Erhöhe die maximale Filmdauer oder wähle ein anderes Genre oder eine andere Stimmung.";
    }
    changeFiltersButton.hidden = false;
}

function ladeGeseheneIds() {
    try {
        const gespeichert = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
        return new Set(Array.isArray(gespeichert) ? gespeichert.filter((id) => typeof id === "string") : []);
    } catch (error) {
        return new Set();
    }
}

function speichereGeseheneIds() {
    try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify([...seenMovieIds]));
    } catch (error) {
        errorMessage.textContent = "Der Browser konnte den Gesehen-Status nicht speichern. Prüfe die lokalen Speichereinstellungen.";
        errorMessage.hidden = false;
    }
}

function kapitalisiere(wort) {
    return wort.charAt(0).toUpperCase() + wort.slice(1);
}
