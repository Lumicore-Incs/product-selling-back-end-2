package com.selling.dto.get;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class UserSummeryGet {
    private Integer totalOrders;
    private Integer totalItem;
    private Integer deleverd;
    private BigDecimal total;
    private List<UserSummeryDetailsGet> summery;
}
