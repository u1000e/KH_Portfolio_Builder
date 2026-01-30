package com.portfolio.builder.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CategoryAccuracy {
    private String category;
    private double accuracy;
    private int solvedCount;
}
