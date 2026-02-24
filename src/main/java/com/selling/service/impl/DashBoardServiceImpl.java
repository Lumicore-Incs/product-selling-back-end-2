package com.selling.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.selling.dto.OrderDto;
import com.selling.dto.get.GetUserDetailsDto;
import org.modelmapper.ModelMapper;
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
    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    @Override
    public List<ExcelTypeDto> findOrder(String name) {

        try {
            List<ExcelTypeDto> excelTypeDtos = new ArrayList<>();

            List<Order> pendingOrdersWithQuantities = customerRepo.findAllPendingOrdersWithQuantities();

            Long filterProductId = null;

            // If name is not "all", get productId
            if (!name.equalsIgnoreCase("all")) {
                Product byName = productRepo.findByName(name);
                if (byName != null) {
                    filterProductId = Long.valueOf(byName.getProductId());
                }
            }

            for (Order order : pendingOrdersWithQuantities) {

                boolean isMatch = false;
                StringBuilder qtyDetails = new StringBuilder();

                for (OrderDetails od : order.getOrderDetails()) {

                    Long orderProductId = Long.valueOf(od.getProduct().getProductId());

                    // If "all", take all orders
                    if (name.equalsIgnoreCase("all")) {
                        isMatch = true;
                    }
                    // Otherwise match productId
                    else if (filterProductId != null && filterProductId.equals(orderProductId)) {
                        isMatch = true;
                    }

                    if (isMatch) {
                        qtyDetails.append(" + ")
                                .append(od.getProduct().getName())
                                .append(" + ")
                                .append(od.getQty());
                    }
                }

                if (isMatch) {
                    Customer customer = order.getCustomer();

                    ExcelTypeDto excelTypeDto = new ExcelTypeDto();
                    excelTypeDto.setId(order.getSerialNo());
                    excelTypeDto.setName(customer.getName());
                    excelTypeDto.setAddress(customer.getAddress());
                    excelTypeDto.setContact01(customer.getContact01());
                    excelTypeDto.setContact02(customer.getContact02());
                    excelTypeDto.setPrice(String.valueOf(order.getTotalPrice()));
                    excelTypeDto.setNote(order.getRemark());

                    excelTypeDtos.add(excelTypeDto);
                }
            }

            return excelTypeDtos;

        } catch (Exception e) {
            System.out.println("message is : " + e.getMessage());
            return null;
        }
    }

    @Override
    public String ConformOrder(List<String> serialNumbers) {
        try {
           for (String serialNumber : serialNumbers) {
               Optional<Order> bySerialNo = orderRepo.findBySerialNo(serialNumber);
               if (bySerialNo.isPresent()) {
                   bySerialNo.get().getCustomer().setStatus("PRINTING");
                   customerRepo.save(bySerialNo.get().getCustomer());
               }
           }
        } catch (Exception e) {
            System.out.println("message is : " + e.getMessage());
            return null;
        }
        return "success";
    }

    @Override
    public int getTotalOrder(UserDto user) {

        LocalDate today = LocalDate.now();

        LocalDate startDate;

        if (today.getDayOfMonth() >= 15) {
            // This month 15
            startDate = LocalDate.of(today.getYear(), today.getMonth(), 15);
        } else {
            // Previous month 15
            LocalDate previousMonth = today.minusMonths(1);
            startDate = LocalDate.of(previousMonth.getYear(), previousMonth.getMonth(), 15);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();

        if (user.getRole().equals("SUPER USER") || user.getRole().equals("ADMIN")) {
            return Math.toIntExact(orderRepo.sumQtyAfterDate(startDateTime));
        }

        return Math.toIntExact(orderRepo.sumQtyByUserAfterDate(
                user.getId(),
                startDateTime
        ));
    }


    @Override
    public int getTodayOrder(UserDto user) {

        LocalDate today = LocalDate.now();

        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);

        if (user.getRole().equals("SUPER USER") || user.getRole().equals("ADMIN")) {
            return orderRepo.sumTodayQty(start, end).intValue();
        }

        return orderRepo.sumTodayQtyByUser(
                user.getId(),
                start,
                end
        ).intValue();
    }


    @Override
    public int getConformOrder(UserDto user) {

        LocalDate today = LocalDate.now();

        LocalDate startDate;

        if (today.getDayOfMonth() >= 15) {
            // This month 15
            startDate = LocalDate.of(today.getYear(), today.getMonth(), 15);
        } else {
            // Previous month 15
            LocalDate previousMonth = today.minusMonths(1);
            startDate = LocalDate.of(previousMonth.getYear(), previousMonth.getMonth(), 15);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();

        if (user.getRole().equals("SUPER USER") || user.getRole().equals("ADMIN")) {
            return Math.toIntExact(orderRepo.countByStatusAndDateBetween("Delivered", startDateTime));
        }

        return Math.toIntExact(orderRepo.countByCustomerUserEmailAndStatusAndDateBetween(
                "Delivered", user.getId(), startDateTime));
    }

    @Override
    public int getCancelOrder(UserDto user) {

        LocalDate today = LocalDate.now();
        String cancelStatus = "Returned to Client";

        LocalDate startDate;

        if (today.getDayOfMonth() >= 15) {
            // This month 15
            startDate = LocalDate.of(today.getYear(), today.getMonth(), 15);
        } else {
            // Previous month 15
            LocalDate previousMonth = today.minusMonths(1);
            startDate = LocalDate.of(previousMonth.getYear(), previousMonth.getMonth(), 15);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();

        if (user.getRole().equals("SUPER USER") || user.getRole().equals("ADMIN")) {
            return Math.toIntExact(orderRepo.countByStatusAndDateBetween(cancelStatus, startDateTime));
        }

        return Math.toIntExact(orderRepo.countByCustomerUserEmailAndStatusAndDateBetween(
                cancelStatus, user.getId(), startDateTime));
    }

    @Override
    public GetUserDetailsDto getUserDetails(Long id) {
        List<Order> byUserId = orderRepo.findByUserId(id);
        System.out.println(byUserId.size());
        GetUserDetailsDto getUserDetailsDto = new GetUserDetailsDto();
        List<OrderDto> allData = new ArrayList<>();
        for (Order order:byUserId){
            allData.add(modelMapper.map(order, OrderDto.class));
        }
        getUserDetailsDto.setOrder(allData);
        //add income---------------------------------------------------
        return getUserDetailsDto;
    }
}
