package br.ufrn.imd.imdtravel.service.exception;

public class SoldOutException extends RuntimeException {
    public SoldOutException(String flight, String day) {
        super("Voo esgotado: " + flight + " em " + day);
    }
}