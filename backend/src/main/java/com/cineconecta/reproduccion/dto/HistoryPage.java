package com.cineconecta.reproduccion.dto;

import java.util.List;

/** Página del historial. totalElements cuenta todas las sesiones del perfil. */
public record HistoryPage(List<HistoryItem> items, int page, int size, long totalElements) {}
