package com.cineconecta.cuentas;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cineconecta.cuentas.dto.AccountHistoryItem;
import com.cineconecta.security.CurrentUser;

import lombok.RequiredArgsConstructor;

/** Datos de la cuenta autenticada. Solo CLIENT. */
@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountHistoryRepository history;
    private final CurrentUser currentUser;

    /** RF-04. La cuenta sale del token: no hay forma de pedir el historial de otra. */
    @GetMapping("/history")
    public List<AccountHistoryItem> history() {
        return history.findByAccount(currentUser.accountId());
    }
}
