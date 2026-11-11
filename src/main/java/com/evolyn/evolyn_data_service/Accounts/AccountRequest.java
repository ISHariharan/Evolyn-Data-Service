package com.evolyn.evolyn_data_service.Accounts;

import java.util.UUID;

public record AccountRequest(
        UUID userId,
        String name,
        String accountType,
        String last4) {
}
