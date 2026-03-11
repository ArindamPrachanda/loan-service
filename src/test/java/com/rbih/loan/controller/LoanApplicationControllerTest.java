package com.rbih.loan.controller;

import com.rbih.loan.domain.enums.ApplicationStatus;
import com.rbih.loan.domain.enums.RiskBand;
import com.rbih.loan.dto.request.LoanApplicationRequest;
import com.rbih.loan.dto.response.LoanApplicationResponse;
import com.rbih.loan.dto.response.LoanOfferResponse;
import com.rbih.loan.service.LoanApplicationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanApplicationController.class)
public class LoanApplicationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    LoanApplicationService loanApplicationService;

    @Test
    @DisplayName("POST /application - should return 201 for approved application")
    void shouldReturn201ForApprovedApplication() throws Exception {

        LoanApplicationResponse mockResponse = LoanApplicationResponse.builder()
                .applicationId(UUID.randomUUID())
                .status(ApplicationStatus.APPROVED)
                .riskBand(RiskBand.LOW)
                .offer(LoanOfferResponse.builder()
                        .interestRate(new BigDecimal("12.0"))
                        .tenureMonths(36)
                        .emi(new BigDecimal("16234.23"))
                        .totalPayable(new BigDecimal("584432.23"))
                        .build())
                .build();
        when(loanApplicationService.evaluate(any())).thenReturn(mockResponse);

        String body = """
                     {"applicant":{"name":"John Doe","age":30,"monthlyIncome":75000,
                     "employmentType":"SALARIED","creditScore":560},
                     "loan":{"amount":500000,"tenureMonths":36,"purpose":"PERSONAL"}}
                     """;

        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.riskBand").value("LOW"))
                .andExpect(jsonPath("$.offer.tenureMonths").value(36));
    }

    @Test
    @DisplayName("POST /applications - should return 400 for invalid request")
    void shouldReturn400ForInvalidRequest() throws Exception {
        String body = """
                     {"applicant":{"name":"","age":15,"monthlyIncome":-100,
                     "employmentType":"SALARIED","creditScore":200},
                     "loan":{"amount":100,"tenureMonths":3,"purpose":"PERSONAL"}}
                     """;

        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("POST /applications - should return 200 for rejected application")
    void shouldReturn200ForRejectedApplication() throws Exception {
        LoanApplicationResponse mockResponse = LoanApplicationResponse.builder()
                .applicationId(UUID.randomUUID())
                .status(ApplicationStatus.REJECTED)
                .rejectionReasons(List.of("CREDIT_SCORE_BELOW_MINIMUM"))
                .build();

        when(loanApplicationService.evaluate(any(LoanApplicationRequest.class))).thenReturn(mockResponse);

        String body = """
                     {"applicant":{"name":"Jane Doe","age":30,"monthlyIncome":75000,
                     "employmentType":"SALARIED","creditScore":650},
                     "loan":{"amount":500000,"tenureMonths":36,"purpose":"HOME"}}
                     """;

        mockMvc.perform(post("/applications").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.rejectionReasons[0]").value("CREDIT_SCORE_BELOW_MINIMUM"));
    }


}
