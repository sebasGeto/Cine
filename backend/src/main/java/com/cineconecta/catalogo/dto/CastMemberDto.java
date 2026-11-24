package com.cineconecta.catalogo.dto;

import com.cineconecta.catalogo.CastMember;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.constraints.NotBlank;

@JsonInclude(Include.NON_NULL)
public record CastMemberDto(
        @NotBlank(message = "es obligatorio") String name,
        String character) {

    static CastMemberDto from(CastMember c) {
        return new CastMemberDto(c.getName(), c.getCharacter());
    }
}
