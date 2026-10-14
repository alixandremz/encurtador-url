package com.alixandremz.encurtador.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.alixandremz.encurtador.dto.LinkRequest;
import com.alixandremz.encurtador.dto.LinkResponse;
import com.alixandremz.encurtador.service.LinkService;

import jakarta.validation.Valid;

@RestController
public class LinkController {

    private final LinkService servico;

    public LinkController(LinkService servico) {
        this.servico = servico;
    }

    @PostMapping("/links")
    public ResponseEntity<LinkResponse> criar(@Valid @RequestBody LinkRequest pedido) {
        LinkResponse criado = servico.criar(pedido);
        return ResponseEntity.created(URI.create("/links/" + criado.slug())).body(criado);
    }

    @GetMapping("/links/{slug}")
    public LinkResponse estatisticas(@PathVariable String slug) {
        return servico.estatisticas(slug);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<Void> redirecionar(@PathVariable String slug) {
        String destino = servico.redirecionar(slug);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(destino)).build();
    }
}
