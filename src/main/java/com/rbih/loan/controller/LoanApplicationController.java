package com.rbih.loan.controller;

import com.rbih.loan.dto.request.LoanApplicationRequest;
import com.rbih.loan.dto.response.LoanApplicationResponse;
import com.rbih.loan.service.LoanApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @Autowired
    public LoanApplicationController(LoanApplicationService loanApplicationService) {
        this.loanApplicationService = loanApplicationService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> getAllApplications() {
        return ResponseEntity.status(HttpStatus.OK).body("Loan Applications is Up!");
    }

    @PostMapping
    public ResponseEntity<LoanApplicationResponse> createApplication(
            @Valid @RequestBody LoanApplicationRequest request) {

        LoanApplicationResponse response = loanApplicationService.evaluate(request);
        HttpStatus status = response.getStatus().name().equals("APPROVED")
                ? HttpStatus.CREATED
                : HttpStatus.OK;

        return ResponseEntity.status(status).body(response);
    }
}
