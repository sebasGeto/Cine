package com.cineconecta.milista.dto;

import java.time.Instant;

/**
 * Un elemento de Mi lista tal como lo ve el cliente (RF-26).
 *
 * retired           el contenido fue retirado del catálogo: se muestra marcado y no se puede reproducir
 * available         publicado y reproducible
 * allowedForProfile su clasificación no supera la del perfil (puede haber cambiado después de agregarlo)
 *
 * Si el contenido ya no existe en el catálogo, solo se conocen contentId y addedAt.
 */
public record MyListEntry(
    String contentId,
    String title,
    String type,
    Integer releaseYear,
    Integer ageRating,
    String coverUrl,
    boolean available,
    boolean retired,
    boolean allowedForProfile,
    Instant addedAt
) {}
