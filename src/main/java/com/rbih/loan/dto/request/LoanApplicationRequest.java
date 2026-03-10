package com.rbih.loan.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoanApplicationRequest {

    @NotNull(message = "Applicant details are required")
    @Valid
    private ApplicantRequest applicant;

    @NotNull(message = "Loan details are required")
    @Valid
    private LoanRequest loan;
}
