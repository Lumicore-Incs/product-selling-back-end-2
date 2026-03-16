package com.selling.service;

import com.selling.dto.get.DailyCountDtoGet;
import com.selling.dto.PaginationResponse;

public interface DailyCountService {
//    List<DailyCountDto> getDailyCountByDate(Date date);
    
    /**
     * Get daily counts for a specific date with pagination
     */
    PaginationResponse<DailyCountDtoGet> getDailyCountByDatePaginated(int page, int size);
}
