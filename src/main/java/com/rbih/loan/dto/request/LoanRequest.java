package com.rbih.loan.dto.request;

import com.rbih.loan.domain.enums.LoanPurpose;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class LoanRequest {

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "10000", message = "Loan amount must be at least 10,000")
    @DecimalMax(value = "5000000", message = "Loan amount must not exceed 50,00,000")
    private BigDecimal amount;

    @NotNull(message = "Tenure is required")
    @Min(value = 6, message = "Tenure must be at least 6 months")
    @Max(value = 360, message = "Tenure must not exceed 360 months")
    private Integer tenureMonths;

    @NotNull(message = "Loan purpose is required")
    private LoanPurpose purpose;
}
