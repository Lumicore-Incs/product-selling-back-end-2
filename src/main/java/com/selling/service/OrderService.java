package com.selling.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.TrackingDto;
import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDtoGet;
import com.selling.model.Product;

public interface OrderService {
  List<OrderDtoGet> getAllTodayOrder();

  List<OrderDtoGet> getAllTodayOrderByUserId(UserDto userDto);

  List<OrderDtoGet> getAllOrder();

  List<OrderDtoGet> getAllOrderByUserId(UserDto userDto);

  Page<OrderDtoGet> getAllOrderPaginated(int page, int size, String status, String search);

  Page<OrderDtoGet> getAllOrderByUserIdPaginated(UserDto userDto, int page, int size, String status, String search);

  List<OrderDtoGet> getTemporaryOrders(UserDto dto);

  void updateOrderDetails(UserDto userDto);

  Object resolveDuplicateOrder(Integer orderId, String userRole, CustomerRequestDTO requestDTO);

  Object deleteOrder(Integer orderId);

  // Generate serial number for an order based on product
  String generateOrderSerialNumber(Product product, UserDto userDto);

  String trackingUpload(List<TrackingDto> trackingList);

  ArrayList<String> getUrgentOrders(Long id, String date);

}
