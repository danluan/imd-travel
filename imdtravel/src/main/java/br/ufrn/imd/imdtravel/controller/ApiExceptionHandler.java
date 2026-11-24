package br.ufrn.imd.imdtravel.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.ufrn.imd.imdtravel.controller.dto.ErrorResponse;
import br.ufrn.imd.imdtravel.obs.JsonLogger;
import br.ufrn.imd.imdtravel.service.exception.FlightNotFoundException;
import br.ufrn.imd.imdtravel.service.exception.SoldOutException;

/**
 * Único lugar do sistema que captura exceções. Ele só MAPEIA falhas para códigos
 * de erro; não transforma falha em sucesso (R-21).
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private final JsonLogger logger;

    public ApiExceptionHandler(JsonLogger logger) {
        this.logger = logger;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ErrorResponse> invalidRequest(Exception e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("invalid-request"));
    }

    @ExceptionHandler(FlightNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(FlightNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("not-found"));
    }

    @ExceptionHandler(SoldOutException.class)
    ResponseEntity<ErrorResponse> soldOut(SoldOutException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("sold-out"));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> internalError(Exception e) {
        logger.log("erro.interno", 0, Map.of("erro", JsonLogger.descrever(e)));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("internal-error"));
    }
}