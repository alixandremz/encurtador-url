package com.alixandremz.encurtador.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.alixandremz.encurtador.service.LinkExpiradoException;
import com.alixandremz.encurtador.service.LinkNaoEncontradoException;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(LinkNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> naoEncontrado(LinkNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErroResposta.de(ex.getMessage()));
    }

    @ExceptionHandler(LinkExpiradoException.class)
    public ResponseEntity<ErroResposta> expirado(LinkExpiradoException ex) {
        return ResponseEntity.status(HttpStatus.GONE).body(ErroResposta.de(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> invalido(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.put(erro.getField(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ErroResposta("dados inválidos", campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
                .body(ErroResposta.de("corpo da requisição inválido"));
    }
}
