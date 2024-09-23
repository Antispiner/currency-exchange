package org.example.exception;

public class ExchangeRateServiceException extends RuntimeException {
    public ExchangeRateServiceException(String message) {
        super(message);
    }

    public ExchangeRateServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
