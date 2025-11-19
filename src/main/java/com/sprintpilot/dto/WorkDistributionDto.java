package com.sprintpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkDistributionDto {
    private List<String> labels;
    private List<BigDecimal> values;
    private List<BigDecimal> percentages;
    private Integer totalTasks;
}

