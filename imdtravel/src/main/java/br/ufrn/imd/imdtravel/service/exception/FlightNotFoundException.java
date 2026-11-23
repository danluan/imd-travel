package br.ufrn.imd.imdtravel.service.exception;

public class FlightNotFoundException extends RuntimeException {
    public FlightNotFoundException(String flight, String day) {
        super("Voo não encontrado: " + flight + " em " + day);
    }
}