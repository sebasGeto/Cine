package com.cineconecta.cuentas;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Integer> {

    /**
     * Se escribe con lower() porque el índice ux_accounts_email_ci es sobre LOWER(email).
     * La consulta que Spring derivaría del nombre usa UPPER(email) y no aprovecharía el índice.
     */
    @Query("select a from Account a where lower(a.email) = lower(:email)")
    Optional<Account> findByEmailIgnoreCase(String email);
}
