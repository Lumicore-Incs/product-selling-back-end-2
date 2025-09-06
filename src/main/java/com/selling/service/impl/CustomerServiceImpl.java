package com.selling.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.UserDto;
import com.selling.dto.get.CustomerDtoGet;
import com.selling.model.Customer;
import com.selling.model.Order;
import com.selling.model.OrderDetails;
import com.selling.model.Product;
import com.selling.model.User;
import com.selling.repository.CustomerRepo;
import com.selling.repository.OrderDetailsRepo;
import com.selling.repository.OrderRepo;
import com.selling.repository.ProductRepo;
import com.selling.service.CustomerService;
import com.selling.util.MapperService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

  private final CustomerRepo customerRepository;
  private final OrderRepo orderRepository;
  private final OrderDetailsRepo orderDetailsRepository;
  private final ProductRepo productRepository;
  private final MapperService mapperService;

  @Override
  @Transactional
  public Object saveCustomerTemporory(CustomerRequestDTO requestDTO, UserDto userDto) {
    String canonical = normalizeContact(
        requestDTO.getContact01() != null ? requestDTO.getContact01() : requestDTO.getContact02());

    Optional<Customer> opt = customerRepository.findByCanonicalContact(canonical);
    if (!opt.isPresent()) {
      // new customer
      opt = Optional.ofNullable(createNewCustomer(requestDTO, userDto));
    }
    return createNewOrder(requestDTO, opt);
  }

  private Customer createNewCustomer(CustomerRequestDTO requestDTO, UserDto userDto) {
    Customer newCustomer = mapperService.map(requestDTO, Customer.class);
    if (newCustomer.getUser() == null) {
      newCustomer.setUser(mapperService.map(userDto, User.class));
    }
    String canonical = normalizeContact(
        newCustomer.getContact01() != null ? newCustomer.getContact01() : newCustomer.getContact02());
    newCustomer.setCanonicalContact(canonical);
    return customerRepository.save(newCustomer);
  }

  // 2. Create and Save Order
  private Object createNewOrder(CustomerRequestDTO requestDTO, Optional<Customer> opt) {
    Order order = new Order();
    order.setCustomer(opt.get());
    order.setDate(LocalDateTime.now());
    if (requestDTO.getCustomerId() != null) {
      order.setStatus("TEMPORARY");
    } else {
      order.setStatus("PENDING");
    }
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
    return "need to add response DTO or entity";
  }

  @Override
  public List<CustomerDtoGet> getAllCustomer() {
    List<CustomerDtoGet> customerDtoGetList = new ArrayList<>();
    List<Customer> allCustomer = customerRepository.findAll();
    for (Customer customer : allCustomer) {
      CustomerDtoGet dto = mapperService.map(customer, CustomerDtoGet.class);
      if (customer.getUser() != null) {
        dto.setUser(mapperService.map(customer.getUser(), com.selling.dto.UserDto.class));
      }
      customerDtoGetList.add(dto);
    }
    return customerDtoGetList;
  }

  @Override
  public List<CustomerDtoGet> getAllCustomerByUserId(UserDto userDto) {
    List<CustomerDtoGet> customerDtoGetList = new ArrayList<>();
    List<Customer> allCustomer = customerRepository.findAllByUserId(Long.valueOf(userDto.getId()));
    for (Customer customer : allCustomer) {
      CustomerDtoGet dto = mapperService.map(customer, CustomerDtoGet.class);
      if (customer.getUser() != null) {
        dto.setUser(mapperService.map(customer.getUser(), com.selling.dto.UserDto.class));
      }
      customerDtoGetList.add(dto);
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

  /**
   * Normalize phone/contact values to a canonical string for comparison.
   */
  private String normalizeContact(String raw) {
    if (raw == null)
      return "";
    String s = raw.trim();
    if (s.isEmpty())
      return "";

    boolean hadPlus = s.startsWith("+");
    String digits = s.replaceAll("\\D", "");
    if (digits.isEmpty())
      return "";

    if (hadPlus) {
      return "+" + digits;
    }

    if (digits.startsWith("0") && digits.length() > 1) {
      return "+94" + digits.substring(1);
    }
    if (digits.startsWith("94") && digits.length() > 2) {
      return "+94" + digits.substring(2);
    }
    if (digits.length() == 9 && digits.startsWith("7")) {
      return "+94" + digits;
    }

    return digits;
  }

}
