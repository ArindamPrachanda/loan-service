package com.rbih.loan.dto.response;

import com.rbih.loan.domain.enums.ApplicationStatus;
import com.rbih.loan.domain.enums.RiskBand;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class LoanApplicationResponse {
    private UUID applicationId;
    private ApplicationStatus status;
    private RiskBand riskBand;
    private LoanOfferResponse offer;
    private List<String> rejectionReasons;
}
