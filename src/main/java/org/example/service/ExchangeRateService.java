package org.example.service;

import org.example.cache.CacheNames;
import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.example.exception.ExchangeRateServiceException;
import org.example.exception.ExternalApiException;
import org.example.repository.ExchangeRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Service
public class ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyService currencyService;
    private final ExternalApiService externalApiService;
    private final Cache exchangeRateCache;

    public ExchangeRateService(ExchangeRateRepository exchangeRateRepository,
                               CurrencyService currencyService,
                               ExternalApiService externalApiService,
                               CacheManager cacheManager) {
        this.exchangeRateRepository = exchangeRateRepository;
        this.currencyService = currencyService;
        this.externalApiService = externalApiService;
        this.exchangeRateCache = cacheManager.getCache(CacheNames.EXCHANGE_RATES);
    }

    public ExchangeRate getExchangeRate(String currencyCode) {
        Currency currency = currencyService.getCurrencyByCode(currencyCode);

        try {
            return exchangeRateCache.get(currencyCode, () -> {
                ExchangeRate exchangeRate = exchangeRateRepository.findFirstByCurrencyOrderByTimestampDesc(currency);
                if (exchangeRate != null && !isExchangeRateExpired(exchangeRate)) {
                    return exchangeRate;
                } else {
                    ExchangeRate fetchedExchangeRate = externalApiService.fetchExchangeRate(currency);
                    exchangeRateRepository.save(fetchedExchangeRate);
                    return fetchedExchangeRate;
                }
            });
        } catch (Cache.ValueRetrievalException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ExternalApiException) {
                throw new ExchangeRateServiceException("Failed to fetch exchange rate for currency: " + currencyCode, cause);
            } else {
                throw new ExchangeRateServiceException("Unexpected error while fetching exchange rate for currency: " + currencyCode, cause);
            }
        } catch (Exception e) {
            throw new ExchangeRateServiceException("Unexpected error while fetching exchange rate for currency: " + currencyCode, e);
        }
    }

    private boolean isExchangeRateExpired(ExchangeRate exchangeRate) {
        return exchangeRate.getTimestamp().isBefore(LocalDateTime.now().minusHours(1));
    }

    public void updateExchangeRates() {
        List<Currency> currencies = currencyService.getAllCurrencies();
        List<ExchangeRate> exchangeRates = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(currencies.size());

        for (Currency currency : currencies) {
            Thread.startVirtualThread(() -> {
                try {
                    ExchangeRate exchangeRate = externalApiService.fetchExchangeRate(currency);
                    exchangeRateCache.put(currency.getCode(), exchangeRate);
                    synchronized (exchangeRates) {
                        exchangeRates.add(exchangeRate);
                    }
                } catch (ExternalApiException e) {
                    logger.error("Error fetching exchange rate for currency {}: {}", currency.getCode(), e.getMessage());
                } catch (Exception e) {
                    logger.error("Unexpected error fetching exchange rate for currency {}: {}", currency.getCode(), e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (!exchangeRates.isEmpty()) {
            exchangeRateRepository.saveAll(exchangeRates);
        } else {
            logger.warn("No exchange rates were fetched successfully.");
        }
    }
}
