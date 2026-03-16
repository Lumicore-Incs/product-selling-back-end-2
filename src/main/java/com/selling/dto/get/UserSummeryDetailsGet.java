package com.selling.dto.get;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class UserSummeryDetailsGet {
    private LocalDateTime date;
    private Integer qty;
    private BigDecimal commission;
    private BigDecimal total;
}
