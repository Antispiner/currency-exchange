package org.example.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.example.cache.CacheNames;
import org.example.cache.TtlCache;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class AppConfig {

    @Value("${resttemplate.connect.timeout}")
    private int connectTimeout;

    @Value("${resttemplate.read.timeout}")
    private int readTimeout;

    @Value("${resttemplate.connection.request.timeout}")
    private int connectionRequestTimeout;

    @Bean
    public RestTemplate restTemplate() {
        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(Timeout.ofMilliseconds(readTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultSocketConfig(socketConfig)
                        .build())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectionRequestTimeout(connectionRequestTimeout);
        requestFactory.setConnectTimeout(connectTimeout);

        return new RestTemplate(requestFactory);
    }

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        TtlCache currenciesCache = new TtlCache(CacheNames.CURRENCIES, Duration.ofDays(1));
        TtlCache exchangeRatesCache = new TtlCache(CacheNames.EXCHANGE_RATES, Duration.ofHours(1));

        cacheManager.setCaches(Arrays.asList(currenciesCache, exchangeRatesCache));
        return cacheManager;
    }
}
