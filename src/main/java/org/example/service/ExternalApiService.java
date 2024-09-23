package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.example.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class ExternalApiService {

    private static final Logger logger = LoggerFactory.getLogger(ExternalApiService.class);

    private final RestTemplate restTemplate;
    private final String externalApiUrl;
    private final String externalApiSymbolsUrl;
    private final String accessKey;

    public ExternalApiService(RestTemplate restTemplate,
                              @Value("${external.api.url}") String externalApiUrl,
                              @Value("${external.api.symbols.url}") String externalApiSymbolsUrl,
                              @Value("${external.api.access_key}") String accessKey) {
        this.restTemplate = restTemplate;
        this.externalApiUrl = externalApiUrl;
        this.externalApiSymbolsUrl = externalApiSymbolsUrl;
        this.accessKey = accessKey;
    }

    public List<Currency> fetchAllCurrencies() {
        String url = externalApiSymbolsUrl + "?access_key=" + accessKey;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        if (response != null && response.path("success").asBoolean()) {
            JsonNode symbols = response.get("symbols");
            List<Currency> currencies = new ArrayList<>();
            if (symbols != null) {
                Iterator<String> currencyCodes = symbols.fieldNames();
                while (currencyCodes.hasNext()) {
                    String code = currencyCodes.next();
                    currencies.add(new Currency(code));
                }
            }
            return currencies;
        } else {
            String errorMessage = response != null ? response.path("error").toString() : "Unknown error";
            throw new ExternalApiException("Failed to fetch currency symbols: " + errorMessage);
        }
    }
    @Retry(name = "externalApiRetry", fallbackMethod = "fallbackFetchExchangeRate")
    @CircuitBreaker(name = "externalApiCircuitBreaker", fallbackMethod = "fallbackFetchExchangeRate")
    public ExchangeRate fetchExchangeRate(Currency currency) {
        //just working for EUR....?
        String url = externalApiUrl + "?access_key=" + accessKey + "&base=" + currency.getCode();
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        if (response != null) {
            if (response.path("success").asBoolean()) {
                JsonNode ratesNode = response.get("rates");
                if (ratesNode != null && ratesNode.has(currency.getCode())) {
                    return new ExchangeRate(currency, ratesNode, LocalDateTime.now());
                } else {
                    String errorMessage = "Rates not found for currency: " + currency.getCode();
                    logger.error(errorMessage);
                    throw new ExternalApiException(errorMessage);
                }
            } else {
                String errorMessage = response.path("error").toString();
                logger.error("External API error for currency {}: {}", currency.getCode(), errorMessage);
                throw new ExternalApiException("External API error: " + errorMessage);
            }
        } else {
            String errorMessage = "Null response from external API for currency: " + currency.getCode();
            logger.error(errorMessage);
            throw new ExternalApiException(errorMessage);
        }
    }

    public ExchangeRate fallbackFetchExchangeRate(Currency currency, Throwable t) {
        logger.error("Fallback method invoked for currency {}: {}", currency.getCode(), t.getMessage());
        throw new ExternalApiException("Failed to fetch exchange rate for currency: " + currency.getCode());
    }
}

