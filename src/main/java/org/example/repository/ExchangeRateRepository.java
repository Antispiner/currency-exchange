package org.example.repository;

import org.example.entity.Currency;
import org.example.entity.ExchangeRate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    ExchangeRate findFirstByCurrencyOrderByTimestampDesc(Currency currency);

    @EntityGraph(value = "ExchangeRate.currency", type = EntityGraph.EntityGraphType.LOAD)
    List<ExchangeRate> findAll();
}
