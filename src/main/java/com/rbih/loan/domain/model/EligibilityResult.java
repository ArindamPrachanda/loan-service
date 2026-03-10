package com.rbih.loan.domain.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EligibilityResult {
    private boolean eligible;
    private List<String> rejectionReasons;
}
