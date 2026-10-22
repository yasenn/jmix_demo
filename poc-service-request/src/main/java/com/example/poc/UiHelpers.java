package com.example.poc;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Component("uiHelpers")
public class UiHelpers {

    private static final Map<String, String> STATUS_RU = Map.of(
            "IN_APPROVAL", "На согласовании",
            "APPROVED", "Согласовано",
            "REJECTED", "Отклонено",
            "DRAFT", "Черновик"
    );

    private static final Map<String, String> PAYMENT_STATUS_RU = Map.of(
            "PAID", "Оплачено",
            "PENDING", "Ожидает"
    );

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DecimalFormat MONEY = new DecimalFormat(
            "#,##0.00", new DecimalFormatSymbols(Locale.forLanguageTag("ru-RU")));

    public String statusRu(String status) {
        if (status == null) return "";
        return STATUS_RU.getOrDefault(status, status);
    }

    public String paymentStatusRu(String status) {
        if (status == null) return "";
        return PAYMENT_STATUS_RU.getOrDefault(status, status);
    }

    public String dateRu(LocalDate d) { return d == null ? "" : DATE.format(d); }
    public String dateTimeRu(LocalDateTime d) { return d == null ? "" : DATETIME.format(d); }

    public String money(BigDecimal v) { return v == null ? "" : MONEY.format(v); }
}
