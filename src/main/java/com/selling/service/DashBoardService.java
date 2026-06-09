package com.selling.service;

import com.selling.dto.UserDto;
import com.selling.dto.get.ExcelTypeDto;
import com.selling.dto.get.GetUserDetailsDto;

import java.util.List;

public interface DashBoardService {
    List<ExcelTypeDto> findOrder(String name);

    String ConformOrder(List<String> serialNumbers);

    int getTotalOrder(UserDto user);

    int getTodayOrder(UserDto user);

    int getConformOrder(UserDto user);

    int getConformOrderByUser(UserDto user);

    int getCancelOrder(UserDto user);

    int getCancelOrderByUser(UserDto user);

    List<Object> findOrderQty();
}
