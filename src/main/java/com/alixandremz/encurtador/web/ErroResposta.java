package com.alixandremz.encurtador.web;

import java.util.Map;

public record ErroResposta(String erro, Map<String, String> campos) {

    public static ErroResposta de(String erro) {
        return new ErroResposta(erro, Map.of());
    }
}
