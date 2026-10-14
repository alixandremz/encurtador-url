package com.alixandremz.encurtador.dto;

import java.time.Instant;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LinkRequest(
        @NotBlank(message = "a url é obrigatória")
        @Size(max = 2048, message = "a url deve ter no máximo 2048 caracteres")
        @Pattern(regexp = "^https?://.+", message = "a url deve começar com http:// ou https://")
        String urlOriginal,

        // opcional: se informado, o link deixa de funcionar depois desse instante
        @Future(message = "a data de expiração deve estar no futuro")
        Instant expiraEm
) {
}
