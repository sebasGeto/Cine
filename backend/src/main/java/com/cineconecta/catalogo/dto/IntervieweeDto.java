package com.cineconecta.catalogo.dto;

import com.cineconecta.catalogo.Interviewee;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import jakarta.validation.constraints.NotBlank;

@JsonInclude(Include.NON_NULL)
public record IntervieweeDto(
        @NotBlank(message = "es obligatorio") String name,
        String description) {

    static IntervieweeDto from(Interviewee i) {
        return new IntervieweeDto(i.getName(), i.getDescription());
    }
}
