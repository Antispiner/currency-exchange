package org.example.scheduler;

import org.example.service.ExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ExchangeRateScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateScheduler.class);

    private final ExchangeRateService exchangeRateService;

    public ExchangeRateScheduler(ExchangeRateService exchangeRateService) {
        this.exchangeRateService = exchangeRateService;
    }

    @Scheduled(fixedRateString = "${scheduler.rate}", initialDelay = 1000)
    public void updateExchangeRates() {
        logger.info("Starting scheduled task to update exchange rates");
        exchangeRateService.updateExchangeRates();
        logger.info("Exchange rates update completed");
    }
}
