package com.selling.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class CustomerRequestDTO {
    private Integer customerId;
    private String name;
    private String address;
    private String contact01;
    private String contact02;
    private LocalDateTime date;
    private String remark;
    private String status; // active, inactive
    private Integer userId;
    private List<OrderDetailsDto> items;
    private BigDecimal totalCost;
}
