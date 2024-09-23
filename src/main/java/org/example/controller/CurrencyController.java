package org.example.controller;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.example.service.CurrencyService;
import org.example.service.ExchangeRateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.example.util.ResponseUtil.errorResponse;

@RestController
@RequestMapping("/currencies")
@Validated
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    public CurrencyController(CurrencyService currencyService, ExchangeRateService exchangeRateService) {
        this.currencyService = currencyService;
        this.exchangeRateService = exchangeRateService;
    }

    @GetMapping
    public ResponseEntity<List<Currency>> getAllCurrencies() {
        List<Currency> currencies = currencyService.getAllCurrencies();
        return ResponseEntity.ok(currencies);
    }

    @PostMapping
    public ResponseEntity<?> addCurrency(
            @RequestParam("code")
            @Size(min = 3, max = 3, message = "Currency code must be exactly 3 letters")
            @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency code must consist of letters only")
            String code) {
        Currency currency = currencyService.addCurrency(code);
        return new ResponseEntity<>(currency, HttpStatus.CREATED);
    }

    @GetMapping("/{currencyCode}/rates")
    public ResponseEntity<?> getExchangeRates(
            @PathVariable
            @Size(min = 3, max = 3, message = "Currency code must be 3 characters")
            @Pattern(regexp = "^[A-Za-z]+$", message = "Currency code must contain only letters")
            String currencyCode) {
        ExchangeRate exchangeRate = exchangeRateService.getExchangeRate(currencyCode);
        if (exchangeRate == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(errorResponse("Exchange rates not available for currency: " + currencyCode));
        }
        JsonNode rates = exchangeRate.getRates();
        return ResponseEntity.ok(rates);
    }
}
