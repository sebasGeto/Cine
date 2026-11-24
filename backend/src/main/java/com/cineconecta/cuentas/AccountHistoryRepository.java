package com.cineconecta.cuentas;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.cineconecta.cuentas.dto.AccountHistoryItem;

import lombok.RequiredArgsConstructor;

/** Lee la vista v_account_history, que ya une suscripción, plan y pago. */
@Repository
@RequiredArgsConstructor
public class AccountHistoryRepository {

    private static final String SQL = """
            SELECT subscriptionId, planName, startsAt, endsAt, isCurrent, paymentId, paidAt, amount, reference
            FROM v_account_history
            WHERE accountId = ?
            ORDER BY startsAt DESC
            """;

    private final JdbcTemplate jdbcTemplate;

    public List<AccountHistoryItem> findByAccount(Integer accountId) {
        return jdbcTemplate.query(SQL, (rs, row) -> new AccountHistoryItem(
                rs.getInt("subscriptionId"),
                rs.getString("planName"),
                rs.getObject("startsAt", LocalDateTime.class),
                rs.getObject("endsAt", LocalDateTime.class),
                rs.getBoolean("isCurrent"),
                rs.getInt("paymentId"),
                rs.getObject("paidAt", LocalDateTime.class),
                rs.getBigDecimal("amount"),
                rs.getString("reference")), accountId);
    }
}
