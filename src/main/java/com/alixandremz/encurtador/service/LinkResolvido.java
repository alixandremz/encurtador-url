package com.alixandremz.encurtador.service;

import java.io.Serializable;
import java.time.Instant;

// Serializable porque o Redis serializa o objeto cacheado.
public record LinkResolvido(String urlOriginal, Instant expiraEm) implements Serializable {
}
