package service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.example.exception.ExternalApiException;
import org.example.service.ExternalApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

class ExternalApiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ExternalApiService externalApiService;

    @Value("${external.api.url}")
    private String externalApiUrl = "https://api.exchangeratesapi.io/v1/latest";

    @Value("${external.api.symbols.url}")
    private String externalApiSymbolsUrl = "https://api.exchangeratesapi.io/v1/symbols";

    @Value("${external.api.access_key}")
    private String accessKey = "test_access_key";

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        externalApiService = new ExternalApiService(restTemplate, externalApiUrl, externalApiSymbolsUrl, accessKey);
    }

    @Test
    void testFetchAllCurrencies_Success() throws Exception {
        String jsonResponse = "{\"success\":true,\"symbols\":{\"USD\":\"United States Dollar\",\"EUR\":\"Euro\"}}";
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(responseNode);

        List<Currency> currencies = externalApiService.fetchAllCurrencies();

        assertEquals(2, currencies.size());
        assertEquals("USD", currencies.get(0).getCode());
        assertEquals("EUR", currencies.get(1).getCode());
    }

    @Test
    void testFetchAllCurrencies_Failure() throws Exception {
        String jsonResponse = "{\"success\":false,\"error\":{\"code\":101,\"type\":\"missing_access_key\"}}";
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(responseNode);

        assertThrows(ExternalApiException.class, () -> externalApiService.fetchAllCurrencies());
    }

    @Test
    void testFetchExchangeRate_Success() throws Exception {
        Currency currency = new Currency("USD");
        String jsonResponse = "{\"success\":true,\"rates\":{\"USD\":0.85}}";
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(responseNode);

        ExchangeRate exchangeRate = externalApiService.fetchExchangeRate(currency);

        assertNotNull(exchangeRate);
        assertEquals(currency, exchangeRate.getCurrency());
    }

    @Test
    void testFetchExchangeRate_Failure() throws Exception {
        Currency currency = new Currency("USD");
        String jsonResponse = "{\"success\":false,\"error\":{\"code\":101,\"type\":\"invalid_access_key\"}}";
        JsonNode responseNode = objectMapper.readTree(jsonResponse);

        when(restTemplate.getForObject(anyString(), eq(JsonNode.class))).thenReturn(responseNode);

        assertThrows(ExternalApiException.class, () -> externalApiService.fetchExchangeRate(currency));
    }
}
