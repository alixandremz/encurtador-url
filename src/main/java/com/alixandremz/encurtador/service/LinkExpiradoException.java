package com.alixandremz.encurtador.service;

public class LinkExpiradoException extends RuntimeException {

    public LinkExpiradoException(String slug) {
        super("link expirado: slug " + slug);
    }
}
