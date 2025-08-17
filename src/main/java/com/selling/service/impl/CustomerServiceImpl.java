package com.selling.service.impl;

import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.UserDto;
import com.selling.dto.get.CustomerDtoGet;
import com.selling.model.*;
import com.selling.repository.*;
import com.selling.service.CustomerService;
import com.selling.util.ModelMapperConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepo customerRepository;
    private final OrderRepo orderRepository;
    private final OrderDetailsRepo orderDetailsRepository;
    private final ProductRepo productRepository;
    private final UserRepo userRepository;
    private final ModelMapperConfig modelMapperConfig;
    private final UserRepo userRepo;

    @Override
    @Transactional
    public Object saveCustomer(CustomerRequestDTO requestDTO, UserDto userDto) {

        List<Customer> isCustomerCheck=customerRepository.findByContact01(requestDTO.getContact01());
        LocalDateTime twoWeeksAgo = LocalDateTime.now().minusWeeks(2);
        for (Customer customer : isCustomerCheck) {
            LocalDateTime date = customer.getDate();
            if (date.isAfter(twoWeeksAgo)) {
                // Customer date is within last 2 weeks
                return ("true");
            }
        }

        // 1. Save Customer
        Customer customer = new Customer();
        customer.setName(requestDTO.getName());
        customer.setAddress(requestDTO.getAddress());
        customer.setContact01(requestDTO.getContact01());
        customer.setContact02(requestDTO.getContact02());
        customer.setDate(LocalDateTime.now());
        customer.setUser(userRepository.findUserById(Long.valueOf(userDto.getId())));
        customer.setStatus("pending");

        if (requestDTO.getUserId() != null) {
            User user = userRepository.findById(Long.valueOf(requestDTO.getUserId()))
                    .orElseThrow(() -> new RuntimeException("User not found"));
            customer.setUser(user);
        }

        Customer savedCustomer = customerRepository.save(customer);

        // 2. Create and Save Order
        Order order = new Order();
        order.setCustomer(savedCustomer);
        order.setDate(LocalDateTime.now());
        order.setStatus("pending");
        order.setRemark(requestDTO.getRemark());
        order.setTrackingId(generateTrackingId()); // Implement this method
        order.setTotalPrice(requestDTO.getTotalCost());
        Order savedOrder = orderRepository.save(order);

        // 3. Save Order Details
        List<OrderDetails> orderDetailsList = requestDTO.getItems().stream()
                .map(item -> {
                    Product product = productRepository.findById(Long.valueOf(item.getProductId()))
                            .orElseThrow(() -> new RuntimeException("Product not found with id: " + item.getProductId()));

                    OrderDetails orderDetails = new OrderDetails();
                    orderDetails.setOrder(savedOrder);
                    orderDetails.setProduct(product);
                    orderDetails.setQty(item.getQty());
                    orderDetails.setTotal(item.getTotal());

                    return orderDetails;
                })
                .collect(Collectors.toList());

        orderDetailsRepository.saveAll(orderDetailsList);

        // 4. Prepare and Return Response
        return entityToCustomerDto(savedCustomer);
    }

    @Override
    public List<CustomerDtoGet> getAllCustomer() {
        List<CustomerDtoGet> customerDtoGetList = new ArrayList<>();
        List<Customer> allCustomer = customerRepository.findAll();
        for (Customer customer : allCustomer) {
            customerDtoGetList.add(entityToCustomerDto(customer));
        }
        return customerDtoGetList;
    }

    @Override
    public List<CustomerDtoGet> getAllCustomerByUserId(UserDto userDto) {
        List<CustomerDtoGet> customerDtoGetList = new ArrayList<>();
        List<Customer> allCustomer = customerRepository.findAllByUserId(Long.valueOf(userDto.getId()));
        for (Customer customer : allCustomer) {
            customerDtoGetList.add(entityToCustomerDto(customer));
        }
        return customerDtoGetList;
    }

    @Override
    public boolean deleteCustomer(Integer id) {
        Optional<Customer> customerOptional = customerRepository.findById(id);
        customerOptional.ifPresent(customerRepository::delete);
        return false;
    }


    private String generateTrackingId() {
        return "TRK" + System.currentTimeMillis();
    }

    //mapper handle=============
    private CustomerDtoGet entityToCustomerDto(Customer savedCustomer) {
        if (savedCustomer != null) {
            CustomerDtoGet map = modelMapperConfig.modelMapper().map(savedCustomer, CustomerDtoGet.class);
            map.setUserId(entityToUserDto(savedCustomer.getUser()));
            return map;
        }
        return null;
    }

    private UserDto entityToUserDto(User user) {
        return modelMapperConfig.modelMapper().map(user, UserDto.class);
    }
}