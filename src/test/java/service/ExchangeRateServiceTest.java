package service;

import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.example.exception.ExchangeRateServiceException;
import org.example.repository.ExchangeRateRepository;
import org.example.service.CurrencyService;
import org.example.service.ExchangeRateService;
import org.example.service.ExternalApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class ExchangeRateServiceTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyService currencyService;

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private Cache exchangeRateCache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exchangeRateCache = new ConcurrentMapCache("exchangeRates");
        ReflectionTestUtils.setField(exchangeRateService, "exchangeRateCache", exchangeRateCache);
    }

    @Test
    void testGetExchangeRate() {
        Currency currency = new Currency("USD");
        ExchangeRate exchangeRate = new ExchangeRate(currency, null, LocalDateTime.now());

        when(currencyService.getCurrencyByCode("USD")).thenReturn(currency);
        exchangeRateCache.put("USD", exchangeRate);
        ExchangeRate result = exchangeRateService.getExchangeRate("USD");

        assertNotNull(result);
        assertEquals("USD", result.getCurrency().getCode());
    }

    @Test
    void testGetExchangeRateWithMissingCache() {
        Currency currency = new Currency("USD");
        ExchangeRate exchangeRate = new ExchangeRate(currency, null, LocalDateTime.now());
        when(currencyService.getCurrencyByCode("USD")).thenReturn(currency);
        when(externalApiService.fetchExchangeRate(currency)).thenReturn(exchangeRate);

        ExchangeRate result = exchangeRateService.getExchangeRate("USD");

        assertNotNull(result);
        assertEquals("USD", result.getCurrency().getCode());

        assertEquals(exchangeRate, exchangeRateCache.get("USD").get());
    }

    @Test
    void testGetExchangeRateWithException() {
        Currency currency = new Currency("USD");
        when(currencyService.getCurrencyByCode("USD")).thenReturn(currency);
        when(externalApiService.fetchExchangeRate(currency)).thenThrow(new ExchangeRateServiceException("Failed to fetch"));

        assertThrows(ExchangeRateServiceException.class, () -> exchangeRateService.getExchangeRate("USD"));
    }
}
