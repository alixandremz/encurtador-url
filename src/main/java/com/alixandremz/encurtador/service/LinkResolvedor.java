package com.alixandremz.encurtador.service;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.alixandremz.encurtador.model.Link;
import com.alixandremz.encurtador.repository.LinkRepository;

// Bean separado de propósito: @Cacheable só funciona quando a chamada passa
// pelo proxy do Spring, ou seja, de fora da própria classe.
@Component
public class LinkResolvedor {

    private final LinkRepository repositorio;

    public LinkResolvedor(LinkRepository repositorio) {
        this.repositorio = repositorio;
    }

    @Cacheable(cacheNames = "links", key = "#slug")
    public LinkResolvido resolver(String slug) {
        Link link = repositorio.findBySlug(slug)
                .orElseThrow(() -> new LinkNaoEncontradoException(slug));
        return new LinkResolvido(link.getUrlOriginal(), link.getExpiraEm());
    }
}
