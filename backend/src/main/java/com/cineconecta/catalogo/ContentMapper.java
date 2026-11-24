package com.cineconecta.catalogo;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;

import com.cineconecta.catalogo.dto.CastMemberDto;
import com.cineconecta.catalogo.dto.ContentRequest;
import com.cineconecta.catalogo.dto.EpisodeRequest;
import com.cineconecta.catalogo.dto.IntervieweeDto;
import com.cineconecta.catalogo.dto.SeasonRequest;
import com.cineconecta.catalogo.dto.SourceDto;

/** Convierte las peticiones del administrador, ya validadas, en lo que se guarda en contents. */
final class ContentMapper {

    private static final int SCHEMA_VERSION = 1;

    private ContentMapper() {
    }

    /** RF-14: contenido nuevo, siempre en borrador. */
    static Content newContent(ContentRequest r, Instant now) {
        Content c = new Content();
        c.setType(r.type());
        c.setTitle(r.title().trim());
        c.setSynopsis(r.synopsis().trim());
        c.setReleaseYear(r.releaseYear());
        c.setGenres(Texts.cleanLower(r.genres()));
        c.setCountries(Texts.clean(r.countries()));
        c.setAudioLanguages(Texts.cleanLower(r.audioLanguages()));
        c.setSubtitleLanguages(Texts.cleanLower(r.subtitleLanguages()));
        c.setAgeRating(r.ageRating());
        c.setCoverUrl(r.coverUrl().trim());
        c.setStatus(ContentStatus.DRAFT);
        c.setSchemaVersion(SCHEMA_VERSION);
        c.setCreatedAt(now);
        c.setDurationSec(r.durationSec());
        c.setDirectors(Texts.clean(r.directors()));
        c.setCast(cast(r.cast()));
        c.setTopics(Texts.clean(r.topics()));
        c.setInterviewees(interviewees(r.interviewees()));
        c.setSources(sources(r.sources()));
        c.setSeasons(r.seasons() == null ? null : r.seasons().stream().map(ContentMapper::newSeason).toList());
        return c;
    }

    /**
     * RF-15: campos que cambia una actualización según el tipo. Los opcionales que no vengan
     * quedan en null y se quitan del documento. Las temporadas no se tocan.
     */
    static Map<String, Object> updatedFields(ContentRequest r) {
        Content c = newContent(r, null);
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("title", c.getTitle());
        fields.put("synopsis", c.getSynopsis());
        fields.put("releaseYear", c.getReleaseYear());
        fields.put("genres", c.getGenres());
        fields.put("countries", c.getCountries());
        fields.put("audioLanguages", c.getAudioLanguages());
        fields.put("subtitleLanguages", c.getSubtitleLanguages());
        fields.put("ageRating", c.getAgeRating());
        fields.put("coverUrl", c.getCoverUrl());
        if (!c.isSeries()) {
            fields.put("durationSec", c.getDurationSec());
            fields.put("directors", c.getDirectors());
        }
        if (ContentType.MOVIE.equals(c.getType())) {
            fields.put("cast", c.getCast());
        }
        if (ContentType.DOCUMENTARY.equals(c.getType())) {
            fields.put("topics", c.getTopics());
            fields.put("interviewees", c.getInterviewees());
            fields.put("sources", c.getSources());
        }
        return fields;
    }

    static Season newSeason(SeasonRequest r) {
        List<Episode> episodes = r.episodes() == null ? List.of()
                : r.episodes().stream().map(ContentMapper::newEpisode).toList();
        return new Season(r.number(), r.description().trim(), episodes);
    }

    static Episode newEpisode(EpisodeRequest r) {
        return new Episode(new ObjectId(), r.number(), r.title().trim(), r.synopsis().trim(), r.durationSec());
    }

    private static List<CastMember> cast(List<CastMemberDto> cast) {
        return cast == null ? null : cast.stream()
                .map(m -> new CastMember(m.name().trim(), Texts.trimToNull(m.character())))
                .toList();
    }

    private static List<Interviewee> interviewees(List<IntervieweeDto> interviewees) {
        return interviewees == null ? null : interviewees.stream()
                .map(i -> new Interviewee(i.name().trim(), Texts.trimToNull(i.description())))
                .toList();
    }

    private static List<Source> sources(List<SourceDto> sources) {
        return sources == null ? null : sources.stream()
                .map(s -> new Source(s.name().trim(), s.kind(), Texts.trimToNull(s.url())))
                .toList();
    }
}
