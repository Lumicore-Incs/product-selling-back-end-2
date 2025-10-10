package com.selling.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.selling.dto.UserDto;
import com.selling.dto.get.ExcelTypeDto;
import com.selling.model.Customer;
import com.selling.model.Order;
import com.selling.model.OrderDetails;
import com.selling.model.Product;
import com.selling.repository.CustomerRepo;
import com.selling.repository.OrderRepo;
import com.selling.repository.ProductRepo;
import com.selling.service.DashBoardService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DashBoardServiceImpl implements DashBoardService {

  @Autowired
  private final CustomerRepo customerRepo;
  private final OrderRepo orderRepo;
  private final ProductRepo productRepo;

  @Transactional
  @Override
  public List<ExcelTypeDto> findOrder(String name) {
    List<ExcelTypeDto> excelTypeDtos = new ArrayList<>();
    Product byName = productRepo.findByName(name);
    if (byName == null) {
      List<Order> pendingOrdersWithQuantities = customerRepo.findPendingOrdersWithQuantities(0);

      for (Order order : pendingOrdersWithQuantities) {
        int size = order.getOrderDetails().size();

        if (size != 1) {
          StringBuilder qtyDetails = null;
          for (OrderDetails od : order.getOrderDetails()) {
            Product product = productRepo.findAllByProductId(od.getProduct().getProductId());

            if (qtyDetails == null) {
              qtyDetails = new StringBuilder();
            }
            qtyDetails.append(" + ").append(product.getName()).append(" + ").append(od.getQty());
          }

          Customer customer = order.getCustomer();
          customer.setStatus("PRINTING");
          customerRepo.save(customer);

          ExcelTypeDto excelTypeDto = new ExcelTypeDto();
          excelTypeDto.setId(order.getOrderId());
          excelTypeDto.setName(customer.getName());
          excelTypeDto.setAddress(customer.getAddress());
          excelTypeDto.setContact01(customer.getContact01());
          excelTypeDto.setContact02(customer.getContact02());
          excelTypeDto.setQty(String.valueOf(qtyDetails));
          excelTypeDtos.add(excelTypeDto);
        }
      }
    } else {
      List<Order> pendingOrdersWithQuantities = customerRepo.findPendingOrdersWithQuantities(byName.getProductId());
      for (Order order : pendingOrdersWithQuantities) {
        int size = order.getOrderDetails().size();
        StringBuilder qtyDetails = null;
        for (OrderDetails od : order.getOrderDetails()) {

          if (od.getProduct().getProductId().equals(byName.getProductId()) && size == 1) {
            Product product = productRepo.findAllByProductId(od.getProduct().getProductId());
            if (qtyDetails == null) {
              qtyDetails = new StringBuilder();
            }
            qtyDetails.append(" + ").append(product.getName()).append(" + ").append(od.getQty());

            Customer customer = order.getCustomer();
            customerRepo.save(customer);
            System.out.println(customer.getName());
            ExcelTypeDto excelTypeDto = new ExcelTypeDto();
            excelTypeDto.setId(order.getOrderId());
            excelTypeDto.setName(customer.getName());
            excelTypeDto.setAddress(customer.getAddress());
            excelTypeDto.setContact01(customer.getContact01());
            excelTypeDto.setContact02(customer.getContact02());
            excelTypeDto.setQty(String.valueOf(qtyDetails));
            excelTypeDtos.add(excelTypeDto);
          }
        }
      }
    }

    return excelTypeDtos;
  }

  @Override
  public List<ExcelTypeDto> ConformOrder() {
    List<ExcelTypeDto> pendingOrdersWithQuantities = customerRepo.findPendingOrdersWithQuantities();
    for (ExcelTypeDto excelTypeDto : pendingOrdersWithQuantities) {
      Optional<Customer> byId = customerRepo.findById(excelTypeDto.getId());
      if (byId.isPresent()) {
        byId.get().setStatus("ACTIVE");
        customerRepo.save(byId.get());
      }
    }
    return pendingOrdersWithQuantities;
  }

  @Override
  public int getTotalOrder(UserDto user) {
    if (user.getRole().equals("admin") || user.getRole().equals("ADMIN") || user.getRole().equals("Admin")) {
      return (int) orderRepo.count();
    }
    return orderRepo.countByUserId(user.getId());
  }

  @Override
  public int getTodayOrder(UserDto user) {
    LocalDateTime start = LocalDate.now().atStartOfDay();
    LocalDateTime end = start.plusDays(1);
    if (user.getRole().equals("admin") || user.getRole().equals("ADMIN") || user.getRole().equals("Admin")) {
      return orderRepo.findByDateBetween(start, end).size();
    }
    // need to update this when userId added to orders table

    return orderRepo.findByDateBetweenByUserId(start, end, user.getId()).size();
  }

  @Override
  public int getConformOrder(UserDto user) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
    if (user.getRole().equals("admin") || user.getRole().equals("ADMIN") || user.getRole().equals("Admin")) {
      return orderRepo.countByStatusAndDateBetween("Deliver", startOfMonth, now);
    }
    return orderRepo.countByCustomerUserEmailAndStatusAndDateBetween(
        user.getEmail(), "Deliver", startOfMonth, now);
  }

  @Override
  public int getCancelOrder(UserDto user) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
    String cancelStatus = "Failed to Deliver";

    if (user.getRole().equalsIgnoreCase("admin")) {
      return orderRepo.countByStatusAndDateBetween(cancelStatus, startOfMonth, now);
    } else {
      return orderRepo.countByCustomerUserEmailAndStatusAndDateBetween(
          user.getEmail(), cancelStatus, startOfMonth, now);
    }
  }
}
