package com.example.poc;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class Mock1CService {

    public record Payment(String number, LocalDate date, String counterparty, BigDecimal amount, String status) {}

    public List<Payment> payments() {
        LocalDate today = LocalDate.now();
        return List.of(
                new Payment("ПП-001", today.minusDays(2), "ООО «Ромашка»",  new BigDecimal("15000.00"),  "PAID"),
                new Payment("ПП-002", today.minusDays(1), "ИП Иванов",        new BigDecimal("32500.50"),  "PAID"),
                new Payment("ПП-003", today,              "АО «Вектор»",       new BigDecimal("88000.00"),  "PENDING"),
                new Payment("ПП-004", today,              "ООО «Сфера»",       new BigDecimal("12400.00"),  "PENDING"),
                new Payment("ПП-005", today.minusDays(3), "ООО «Альфа»",       new BigDecimal("210000.00"), "PAID"),
                new Payment("ПП-006", today,              "ООО «Магнит»",      new BigDecimal("47500.00"),  "PENDING")
        );
    }

    public BigDecimal total() {
        return payments().stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal paidTotal() {
        return payments().stream().filter(p -> "PAID".equals(p.status()))
                .map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal pendingTotal() {
        return payments().stream().filter(p -> "PENDING".equals(p.status()))
                .map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
