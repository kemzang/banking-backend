package com.banking.transaction_service.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class PercentageCommissionStrategy implements CommissionStrategy {

    private final BigDecimal standardRate;
    private final BigDecimal sameOperatorRate;

    public PercentageCommissionStrategy(
            @Value("${transaction.commission.percentage:0.005}") BigDecimal standardRate,
            @Value("${transaction.commission.same-operator-percentage:0}") BigDecimal sameOperatorRate
    ) {
        this.standardRate = standardRate;
        this.sameOperatorRate = sameOperatorRate;
    }

    @Override
    public BigDecimal calculate(
            BigDecimal montant,
            Long operateurSourceId,
            Long operateurDestId
    ) {
        boolean sameOperator = operateurSourceId != null
                && operateurSourceId.equals(operateurDestId);
        BigDecimal rate = sameOperator ? sameOperatorRate : standardRate;
        return montant.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
