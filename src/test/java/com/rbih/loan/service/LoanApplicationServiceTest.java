package com.rbih.loan.service;


import com.rbih.loan.domain.entity.LoanApplication;
import com.rbih.loan.domain.enums.*;
import com.rbih.loan.dto.request.ApplicantRequest;
import com.rbih.loan.dto.request.LoanApplicationRequest;
import com.rbih.loan.dto.request.LoanRequest;
import com.rbih.loan.dto.response.LoanApplicationResponse;
import com.rbih.loan.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository repository;

    @InjectMocks
    private LoanApplicationService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Test Risk Classification")
    void shouldReturnLowRiskForHighCreditScore() {

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("John");
        applicant.setAge(30);
        applicant.setEmploymentType(EmploymentType.SALARIED);
        applicant.setMonthlyIncome(new BigDecimal("50000"));
        applicant.setCreditScore(780);


        LoanRequest loan = new LoanRequest();
        loan.setPurpose(LoanPurpose.HOME);
        loan.setAmount(BigDecimal.valueOf(500000));
        loan.setTenureMonths(24);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        when(repository.save(any())).thenAnswer(i -> {
            LoanApplication entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LoanApplicationResponse response = service.evaluate(request);

        assertEquals(RiskBand.LOW, response.getRiskBand());
    }

    @Test
    @DisplayName("Test EMI Calculation Logic")
    void shouldCalculateEmiCorrectly() {

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("John");
        applicant.setAge(30);
        applicant.setCreditScore(760);
        applicant.setEmploymentType(EmploymentType.SALARIED);
        applicant.setMonthlyIncome(BigDecimal.valueOf(100000));

        LoanRequest loan = new LoanRequest();
        loan.setAmount(BigDecimal.valueOf(500000));
        loan.setTenureMonths(24);
        loan.setPurpose(LoanPurpose.HOME);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        when(repository.save(any())).thenAnswer(i -> {
            LoanApplication entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LoanApplicationResponse response = service.evaluate(request);

        assertNotNull(response.getOffer());
        assertTrue(response.getOffer().getEmi().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Test Low Credit Score Rejection")
    void shouldRejectWhenCreditScoreTooLow() {

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("John");
        applicant.setCreditScore(550);
        applicant.setAge(30);
        applicant.setEmploymentType(EmploymentType.SALARIED);
        applicant.setMonthlyIncome(BigDecimal.valueOf(100000));

        LoanRequest loan = new LoanRequest();
        loan.setAmount(BigDecimal.valueOf(300000));
        loan.setTenureMonths(24);
        loan.setPurpose(LoanPurpose.HOME);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        when(repository.save(any())).thenAnswer(i -> {
            LoanApplication entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LoanApplicationResponse response = service.evaluate(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.LOW_CREDIT_SCORE.name()));
    }

    @Test
    @DisplayName("Test EMI Exceeds Monthly Income Rejection")
    void shouldRejectWhenEmiExceedsIncomeThreshold() {

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("john");
        applicant.setCreditScore(750);
        applicant.setAge(30);
        applicant.setMonthlyIncome(BigDecimal.valueOf(20000));
        applicant.setEmploymentType(EmploymentType.SALARIED);

        LoanRequest loan = new LoanRequest();
        loan.setAmount(BigDecimal.valueOf(900000));
        loan.setTenureMonths(12);
        loan.setPurpose(LoanPurpose.HOME);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        when(repository.save(any())).thenAnswer(i -> {
            LoanApplication entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LoanApplicationResponse response = service.evaluate(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.EMI_EXCEEDS_60_PERCENT.name()));
    }

    @Test
    @DisplayName("Test Age + tenure more than 65")
    void shouldRejectWhenAgePlusTenureExceeds65() {

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("john");
        applicant.setCreditScore(750);
        applicant.setAge(60);
        applicant.setMonthlyIncome(BigDecimal.valueOf(60000));
        applicant.setEmploymentType(EmploymentType.SALARIED);

        LoanRequest loan = new LoanRequest();
        loan.setAmount(BigDecimal.valueOf(900000));
        loan.setTenureMonths(72);
        loan.setPurpose(LoanPurpose.HOME);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        when(repository.save(any())).thenAnswer(i -> {
            LoanApplication entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LoanApplicationResponse response = service.evaluate(request);

        assertEquals(ApplicationStatus.REJECTED, response.getStatus());
        assertTrue(response.getRejectionReasons().contains(RejectionReason.AGE_TENURE_LIMIT_EXCEEDED.name()));

    }

    @Test
    @DisplayName("Test Successful Approval")
    void shouldApproveLoanWhenAllConditionsSatisfied() {

        ApplicantRequest applicant = new ApplicantRequest();
        applicant.setName("John");
        applicant.setAge(30);
        applicant.setCreditScore(760);
        applicant.setMonthlyIncome(BigDecimal.valueOf(100000));
        applicant.setEmploymentType(EmploymentType.SALARIED);

        LoanRequest loan = new LoanRequest();
        loan.setAmount(BigDecimal.valueOf(300000));
        loan.setTenureMonths(24);
        loan.setPurpose(LoanPurpose.PERSONAL);

        LoanApplicationRequest request = new LoanApplicationRequest();
        request.setApplicant(applicant);
        request.setLoan(loan);

        when(repository.save(any())).thenAnswer(i -> {
            LoanApplication entity = i.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        LoanApplicationResponse response = service.evaluate(request);

        assertEquals(ApplicationStatus.APPROVED, response.getStatus());
        assertNotNull(response.getOffer());
    }

}
