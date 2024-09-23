package org.example.service;

import org.example.cache.CacheNames;
import org.example.entity.Currency;
import org.example.exception.CurrencyNotFoundException;
import org.example.repository.CurrencyRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyService {

    private final CurrencyRepository currencyRepository;
    private final ExternalApiService externalApiService;

    public CurrencyService(CurrencyRepository currencyRepository, ExternalApiService externalApiService) {
        this.currencyRepository = currencyRepository;
        this.externalApiService = externalApiService;
    }

    @Cacheable(CacheNames.CURRENCIES)
    public List<Currency> getAllCurrencies() {
        long count = currencyRepository.count();
        if (count == 0) {
            initializeCurrencies();
        }
        return currencyRepository.findAll();
    }

    @Async
    public void initializeCurrencies() {
        List<Currency> currencies = externalApiService.fetchAllCurrencies();
        currencyRepository.saveAll(currencies);
        evictCurrenciesCache();
    }

    @CacheEvict(value = CacheNames.CURRENCIES, allEntries = true)
    public void evictCurrenciesCache() {
        // return and finish
    }

    @CacheEvict(value = CacheNames.CURRENCIES, allEntries = true)
    public Currency addCurrency(String code) {
        return currencyRepository.findByCode(code.toUpperCase())
                .orElseGet(() -> currencyRepository.save(new Currency(code)));
    }

    public Currency getCurrencyByCode(String code) {
        return currencyRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new CurrencyNotFoundException("Currency not found: " + code));
    }
}
