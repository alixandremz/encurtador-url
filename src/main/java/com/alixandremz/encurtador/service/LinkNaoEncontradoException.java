package com.alixandremz.encurtador.service;

public class LinkNaoEncontradoException extends RuntimeException {

    public LinkNaoEncontradoException(String slug) {
        super("link não encontrado: slug " + slug);
    }
}
