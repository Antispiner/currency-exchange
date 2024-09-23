package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.CurrencyApplication;
import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.example.exception.ExchangeRateServiceException;
import org.example.service.CurrencyService;
import org.example.service.ExchangeRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CurrencyApplication.class)
@AutoConfigureMockMvc
class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyService currencyService;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @Test
    void testGetAllCurrencies_emptyList() throws Exception {
        when(currencyService.getAllCurrencies()).thenReturn(List.of());

        mockMvc.perform(get("/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testGetAllCurrencies_nonEmptyList() throws Exception {
        when(currencyService.getAllCurrencies()).thenReturn(List.of(new Currency("USD"), new Currency("EUR")));

        mockMvc.perform(get("/currencies")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].code").value("USD"))
                .andExpect(jsonPath("$[1].code").value("EUR"));
    }

    @Test
    void testAddCurrency_success() throws Exception {
        Currency currency = new Currency("GBP");
        when(currencyService.addCurrency("GBP")).thenReturn(currency);

        mockMvc.perform(post("/currencies")
                        .param("code", "GBP")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("GBP"));
    }

    @Test
    void testAddCurrency_validationError() throws Exception {
        mockMvc.perform(post("/currencies")
                        .param("code", "GB")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Currency code must be exactly 3 letters"));
    }

    @Test
    void testAddCurrency_invalidFormat() throws Exception {
        mockMvc.perform(post("/currencies")
                        .param("code", "12G")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Currency code must consist of letters only"));
    }

    @Test
    void testGetExchangeRates_success() throws Exception {
        Currency currency = new Currency("USD");
        ObjectNode ratesNode = new ObjectMapper().createObjectNode();
        ratesNode.put("AED", 1.0);

        ExchangeRate exchangeRate = new ExchangeRate(currency, ratesNode, LocalDateTime.now());

        when(exchangeRateService.getExchangeRate("USD")).thenReturn(exchangeRate);

        mockMvc.perform(get("/currencies/USD/rates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.AED").value(1.0));
    }

    @Test
    void testGetExchangeRates_notFound() throws Exception {
        when(exchangeRateService.getExchangeRate("XYZ")).thenReturn(null);

        mockMvc.perform(get("/currencies/XYZ/rates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Exchange rates not available for currency: XYZ"));
    }

    @Test
    void testGetExchangeRates_validationError() throws Exception {
        mockMvc.perform(get("/currencies/XY/rates") // Код валюты меньше 3 символов
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Currency code must be 3 characters"));
    }

    @Test
    void testGetExchangeRates_invalidFormat() throws Exception {
        mockMvc.perform(get("/currencies/12G/rates") // Код валюты содержит цифры
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Currency code must contain only letters"));
    }

    @Test
    void testGetExchangeRates_serviceException() throws Exception {
        when(exchangeRateService.getExchangeRate("USD")).thenThrow(new ExchangeRateServiceException("Service error"));

        mockMvc.perform(get("/currencies/USD/rates")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Service error"));
    }
}
