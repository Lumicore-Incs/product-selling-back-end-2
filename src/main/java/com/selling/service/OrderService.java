package com.selling.service;

import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDtoGet;

import java.util.List;

public interface OrderService {
    List<OrderDtoGet> getAllTodayOrder();

    List<OrderDtoGet> getAllTodayOrderByUserId(UserDto userDto);

    List<OrderDtoGet> getAllOrder();

    List<OrderDtoGet> getAllOrderByUserId(UserDto userDto);

    void updateOrderDetails();
}
