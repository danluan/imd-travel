package br.ufrn.imd.imdtravel.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record BuyTicketRequest(
        @NotBlank String flight,
        @NotBlank String day,
        @NotBlank String user
) {}