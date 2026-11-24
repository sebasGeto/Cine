package com.cineconecta.catalogo.dto;

import java.util.List;

import com.cineconecta.catalogo.Content;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/** Detalle de un contenido (RF-22). Los campos que no son de su tipo no aparecen en el JSON. */
@JsonInclude(Include.NON_NULL)
public record ContentDetailResponse(
        String id,
        String type,
        String title,
        String synopsis,
        Integer releaseYear,
        List<String> genres,
        List<String> countries,
        List<String> audioLanguages,
        List<String> subtitleLanguages,
        Integer ageRating,
        String coverUrl,
        Integer durationSec,
        List<String> directors,
        List<CastMemberDto> cast,
        List<String> topics,
        List<IntervieweeDto> interviewees,
        List<SourceDto> sources,
        List<SeasonResponse> seasons) {

    public static ContentDetailResponse from(Content c) {
        return new ContentDetailResponse(c.getId().toHexString(), c.getType(), c.getTitle(), c.getSynopsis(),
                c.getReleaseYear(), c.getGenres(), c.getCountries(), c.getAudioLanguages(),
                c.getSubtitleLanguages(), c.getAgeRating(), c.getCoverUrl(), c.getDurationSec(),
                c.getDirectors(),
                c.getCast() == null ? null : c.getCast().stream().map(CastMemberDto::from).toList(),
                c.getTopics(),
                c.getInterviewees() == null ? null
                        : c.getInterviewees().stream().map(IntervieweeDto::from).toList(),
                c.getSources() == null ? null : c.getSources().stream().map(SourceDto::from).toList(),
                SeasonResponse.sorted(c.getSeasons()));
    }
}
