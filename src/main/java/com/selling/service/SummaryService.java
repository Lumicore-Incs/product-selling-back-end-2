package com.selling.service;

import com.selling.dto.get.UserSummeryGet;

public interface SummaryService {

    UserSummeryGet getSummaryDetails(int id, int month);
    
}
