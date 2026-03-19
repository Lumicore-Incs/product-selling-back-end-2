package com.selling.dto.get;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
public class DailyCountDtoGet {
    private Integer id;
    private Date date;
    private Integer totalQty;
    private LocalDateTime lastTime;
    private List<DailyCountDetailsDtoGet> dailyCountDetailsDtoGet;
}
