package com.rbih.loan.service;

import com.rbih.loan.domain.entity.LoanApplication;
import com.rbih.loan.domain.enums.ApplicationStatus;
import com.rbih.loan.domain.enums.EmploymentType;
import com.rbih.loan.domain.enums.RejectionReason;
import com.rbih.loan.domain.enums.RiskBand;
import com.rbih.loan.domain.model.LoanOffer;
import com.rbih.loan.dto.request.ApplicantRequest;
import com.rbih.loan.dto.request.LoanApplicationRequest;
import com.rbih.loan.dto.request.LoanRequest;
import com.rbih.loan.dto.response.LoanApplicationResponse;
import com.rbih.loan.dto.response.LoanOfferResponse;
import com.rbih.loan.repository.LoanApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    private static final int MIN_CREDIT_SCORE = 600;
    private static final int MAX_AGE_PLUS_TENURE = 65;
    private static final BigDecimal EMI_INCOME_THRESHOLD = new BigDecimal("0.60");
    private static final BigDecimal EMI_OFFER_THRESHOLD = new BigDecimal("0.50");
    private static final BigDecimal BASE_RATE = BigDecimal.valueOf(12);
    private static final int SCALE = 2;
    private static final int MONTHS_IN_YEAR = 12;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    @Autowired
    public LoanApplicationService(LoanApplicationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public LoanApplicationResponse evaluate(LoanApplicationRequest request) {

        List<String> reasons = new ArrayList<>();

        if (request.getApplicant().getCreditScore() < MIN_CREDIT_SCORE) {
            reasons.add(RejectionReason.LOW_CREDIT_SCORE.name());
        }

        if (request.getApplicant().getAge() + request.getLoan().getTenureMonths()/12 > MAX_AGE_PLUS_TENURE) {
            reasons.add(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED.name());
        }

        RiskBand riskBand = determineRisk(request.getApplicant().getCreditScore());

        BigDecimal interestRate = calculateInterestRate(request.getApplicant(), request.getLoan(), riskBand);

        BigDecimal emi = calculateEmi(request.getLoan().getAmount(), interestRate, request.getLoan().getTenureMonths());

        BigDecimal maxAllowedEmi = request.getApplicant().getMonthlyIncome().multiply(EMI_INCOME_THRESHOLD).setScale(SCALE, ROUNDING);

        if (emi.compareTo(maxAllowedEmi) > 0) {
            reasons.add(RejectionReason.EMI_EXCEEDS_60_PERCENT.name());
        }

        if (!reasons.isEmpty()) {
            return saveAndBuildRejectedResponse(request, reasons);
        }

        LoanOffer loanOffer = generateOffer(emi, interestRate, request.getLoan().getTenureMonths(),
                request.getApplicant().getMonthlyIncome());

        if (loanOffer == null) {
            return saveAndBuildRejectedResponse(request, List.of(RejectionReason.EMI_EXCEEDS_50_PERCENT_OF_INCOME.name()));
        }

        return saveAndBuildApprovedResponse(request, riskBand, loanOffer);
    }

    private LoanApplicationResponse saveAndBuildApprovedResponse(LoanApplicationRequest request, RiskBand riskBand,
                                                                 LoanOffer loanOffer) {

        LoanApplication entity = buildEntity(request, ApplicationStatus.APPROVED, riskBand, loanOffer, null);
        LoanApplication saved  = repository.save(entity);

        return LoanApplicationResponse.builder()
                .applicationId(saved.getId())
                .status(ApplicationStatus.APPROVED)
                .riskBand(riskBand)
                .offer(LoanOfferResponse.builder()
                        .interestRate(loanOffer.getInterestRate())
                        .tenureMonths(loanOffer.getTenureMonths())
                        .emi(loanOffer.getEmi())
                        .totalPayable(loanOffer.getTotalPayable())
                        .build())
                .build();
    }

    private LoanOffer generateOffer(BigDecimal emi, BigDecimal interestRate,
                                    Integer tenureMonths, BigDecimal monthlyIncome) {

        BigDecimal maxAllowedEmi = monthlyIncome.multiply(EMI_OFFER_THRESHOLD).setScale(2, RoundingMode.HALF_UP);

        if (emi.compareTo(maxAllowedEmi) > 0) {
            return null; // offer rejected
        }
        BigDecimal totalPayable = emi.multiply(BigDecimal.valueOf(tenureMonths)).setScale(SCALE, ROUNDING);

        return LoanOffer.builder()
                .interestRate(interestRate.setScale(2, RoundingMode.HALF_UP))
                .tenureMonths(tenureMonths)
                .emi(emi)
                .totalPayable(totalPayable)
                .build();
    }

    private LoanApplicationResponse saveAndBuildRejectedResponse(LoanApplicationRequest request, List<String> reasons) {

        LoanApplication entity = buildEntity(request, ApplicationStatus.REJECTED, null, null, reasons);
        LoanApplication saved  = repository.save(entity);

        return LoanApplicationResponse.builder()
                .applicationId(saved.getId())
                .status(ApplicationStatus.REJECTED)
                .riskBand(null)
                .rejectionReasons(reasons)
                .build();
    }

    private LoanApplication buildEntity(LoanApplicationRequest request, ApplicationStatus status,
                                        RiskBand riskBand, LoanOffer offer, List<String> rejectionReasons) {
        return LoanApplication.builder()
                .applicantName(request.getApplicant().getName())
                .applicantAge(request.getApplicant().getAge())
                .monthlyIncome(request.getApplicant().getMonthlyIncome())
                .employmentType(request.getApplicant().getEmploymentType())
                .creditScore(request.getApplicant().getCreditScore())
                .loanAmount(request.getLoan().getAmount())
                .tenureMonths(request.getLoan().getTenureMonths())
                .loanPurpose(request.getLoan().getPurpose())
                .status(status)
                .riskBand(riskBand)
                .interestRate(offer != null ? offer.getInterestRate() : null)
                .emi(offer != null ? offer.getEmi() : null)
                .totalPayable(offer != null ? offer.getTotalPayable() : null)
                .rejectionReasons(rejectionReasons != null ? String.join(",", rejectionReasons) : null)
                .build();
    }

    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {

        // r = annual rate / 12 / 100
        BigDecimal monthlyRate = annualRatePercent
                .divide(BigDecimal.valueOf(MONTHS_IN_YEAR), 10, ROUNDING)
                .divide(BigDecimal.valueOf(100), 10, ROUNDING);

        // (1 + r)^n
        BigDecimal onePlusRPowerN = monthlyRate.add(BigDecimal.ONE).pow(tenureMonths, new MathContext(10, ROUNDING));

        // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
        BigDecimal numerator   = principal.multiply(monthlyRate).multiply(onePlusRPowerN);
        BigDecimal denominator = onePlusRPowerN.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    private BigDecimal calculateInterestRate(ApplicantRequest applicantRequest, LoanRequest loan, RiskBand riskBand) {

        BigDecimal rate = BASE_RATE;

        switch (riskBand) {
            case MEDIUM -> rate = rate.add(BigDecimal.valueOf(1.5));
            case HIGH -> rate = rate.add(BigDecimal.valueOf(3));
        }

        if (applicantRequest.getEmploymentType() == EmploymentType.SELF_EMPLOYED)
            rate = rate.add(BigDecimal.ONE);

        if (loan.getAmount().compareTo(BigDecimal.valueOf(1_000_000)) > 0)
            rate = rate.add(BigDecimal.valueOf(0.5));

        return rate;
    }

    private RiskBand determineRisk(int creditScore) {

        if (creditScore >= 750) return RiskBand.LOW;

        if (creditScore >= 650) return RiskBand.MEDIUM;

        return RiskBand.HIGH;
    }
}
