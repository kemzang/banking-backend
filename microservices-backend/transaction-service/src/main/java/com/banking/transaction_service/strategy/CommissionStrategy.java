package com.banking.transaction_service.strategy;

import java.math.BigDecimal;

public interface CommissionStrategy {

    default BigDecimal calculate(BigDecimal montant) {
        return calculate(montant, null, null);
    }

    BigDecimal calculate(
            BigDecimal montant,
            Long operateurSourceId,
            Long operateurDestId
    );
}
