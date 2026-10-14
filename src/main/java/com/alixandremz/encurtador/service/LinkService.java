package com.alixandremz.encurtador.service;

import java.security.SecureRandom;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alixandremz.encurtador.dto.LinkRequest;
import com.alixandremz.encurtador.dto.LinkResponse;
import com.alixandremz.encurtador.model.Link;
import com.alixandremz.encurtador.repository.LinkRepository;

@Service
public class LinkService {

    private static final char[] ALFABETO =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int TAMANHO_SLUG = 7;

    private final LinkRepository repositorio;
    private final LinkResolvedor resolvedor;
    private final SecureRandom aleatorio = new SecureRandom();
    private final String baseUrl;

    public LinkService(LinkRepository repositorio, LinkResolvedor resolvedor,
            @Value("${encurtador.base-url}") String baseUrl) {
        this.repositorio = repositorio;
        this.resolvedor = resolvedor;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    @Transactional
    public LinkResponse criar(LinkRequest pedido) {
        Link link = new Link();
        link.setUrlOriginal(pedido.urlOriginal().trim());
        link.setExpiraEm(pedido.expiraEm());
        link.setSlug(gerarSlug());
        return LinkResponse.de(repositorio.save(link), baseUrl);
    }

    @Transactional(readOnly = true)
    public LinkResponse estatisticas(String slug) {
        return LinkResponse.de(obter(slug), baseUrl);
    }

    @Transactional
    public String redirecionar(String slug) {
        LinkResolvido link = resolvedor.resolver(slug);
        if (link.expiraEm() != null && link.expiraEm().isBefore(Instant.now())) {
            throw new LinkExpiradoException(slug);
        }
        repositorio.incrementarCliques(slug);
        return link.urlOriginal();
    }

    private Link obter(String slug) {
        return repositorio.findBySlug(slug).orElseThrow(() -> new LinkNaoEncontradoException(slug));
    }

    private String gerarSlug() {
        String slug;
        do {
            StringBuilder sb = new StringBuilder(TAMANHO_SLUG);
            for (int i = 0; i < TAMANHO_SLUG; i++) {
                sb.append(ALFABETO[aleatorio.nextInt(ALFABETO.length)]);
            }
            slug = sb.toString();
        } while (repositorio.existsBySlug(slug));
        return slug;
    }
}
