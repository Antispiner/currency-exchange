package service;

import org.example.entity.Currency;
import org.example.exception.CurrencyNotFoundException;
import org.example.repository.CurrencyRepository;
import org.example.service.CurrencyService;
import org.example.service.ExternalApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CurrencyServiceTest {

    @Mock
    private CurrencyRepository currencyRepository;

    @Mock
    private ExternalApiService externalApiService;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private CurrencyService currencyService;

    @Captor
    private ArgumentCaptor<Currency> currencyCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetAllCurrencies() {
        when(currencyRepository.findAll()).thenReturn(List.of(new Currency("USD"), new Currency("EUR")));

        List<Currency> currencies = currencyService.getAllCurrencies();

        assertNotNull(currencies);
        assertEquals(2, currencies.size());
        verify(currencyRepository, times(1)).findAll();
    }

    @Test
    void testAddCurrency() {
        String code = "GBP";
        Currency currency = new Currency(code);

        when(currencyRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(currencyRepository.save(any(Currency.class))).thenReturn(currency);

        Currency result = currencyService.addCurrency(code);

        assertNotNull(result);
        assertEquals("GBP", result.getCode());

        verify(currencyRepository, times(1)).findByCode(code);
        verify(currencyRepository).save(currencyCaptor.capture());

        assertEquals("GBP", currencyCaptor.getValue().getCode());
    }

    @Test
    void testGetCurrencyByCode() {
        String code = "USD";
        Currency currency = new Currency(code);

        when(currencyRepository.findByCode(anyString())).thenReturn(Optional.of(currency));

        Currency result = currencyService.getCurrencyByCode(code);

        assertNotNull(result);
        assertEquals("USD", result.getCode());
        verify(currencyRepository, times(1)).findByCode(code);
    }

    @Test
    void testGetCurrencyByCodeNotFound() {
        when(currencyRepository.findByCode(anyString())).thenReturn(Optional.empty());

        assertThrows(CurrencyNotFoundException.class, () -> currencyService.getCurrencyByCode("XYZ"));
    }
}
