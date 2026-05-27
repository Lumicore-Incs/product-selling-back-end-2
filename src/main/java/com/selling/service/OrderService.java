package com.selling.service;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;


import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.PaginationResponse;
import com.selling.dto.TrackingDto;
import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDtoGet;
import com.selling.model.Product;

public interface OrderService {
  List<OrderDtoGet> getAllTodayOrder();

  List<OrderDtoGet> getAllTodayOrderByUserId(UserDto userDto);

  List<OrderDtoGet> getAllOrder();

  List<OrderDtoGet> getAllOrderByUserId(UserDto userDto, java.util.Date date);

  PaginationResponse<OrderDtoGet> getAllTodayOrderPaginated(int page, int size, String search, String status,
      Integer productId);

  PaginationResponse<OrderDtoGet> getAllTodayOrderByUserIdPaginated(UserDto userDto, int page, int size, String search,
      String status, Integer productId);

  PaginationResponse<OrderDtoGet> getAllOrderPaginated(int page, int size, String search, String status,
                                                       Integer productId, Date date);

  PaginationResponse<OrderDtoGet> getAllOrderByUserIdPaginated(UserDto userDto, int page, int size, String search,
                                                               String status, Integer productId, Date date);

  List<OrderDtoGet> getTemporaryOrders(UserDto dto);

  void updateOrderDetails(UserDto userDto);

  Object resolveDuplicateOrder(Integer orderId, String userRole, CustomerRequestDTO requestDTO);

  Object deleteOrder(Integer orderId);

  // Generate serial number for an order based on product
  String generateOrderSerialNumber(Product product, UserDto userDto);

  String trackingUpload(List<TrackingDto> trackingList);

  ArrayList<String> getUrgentOrders(Long id, String date);

}