package com.alixandremz.encurtador.dto;

import java.time.Instant;

import com.alixandremz.encurtador.model.Link;

public record LinkResponse(
        String slug,
        String urlOriginal,
        String urlCurta,
        long cliques,
        Instant expiraEm,
        Instant criadoEm
) {
    public static LinkResponse de(Link link, String baseUrl) {
        return new LinkResponse(
                link.getSlug(),
                link.getUrlOriginal(),
                baseUrl + "/" + link.getSlug(),
                link.getCliques(),
                link.getExpiraEm(),
                link.getCriadoEm()
        );
    }
}
