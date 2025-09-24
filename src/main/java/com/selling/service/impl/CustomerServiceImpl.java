package com.selling.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.UserDto;
import com.selling.dto.get.CustomerDtoGet;
import com.selling.dto.get.OrderDtoGet;
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
    // collect both contacts if provided (contact01 and contact02)
    String c1 = requestDTO == null ? null : requestDTO.getContact01();
    String c2 = requestDTO == null ? null : requestDTO.getContact02();
    List<String> contacts = new ArrayList<>();
    if (c1 != null && !c1.isBlank())
      contacts.add(c1.trim());
    if (c2 != null && !c2.isBlank() && !contacts.contains(c2.trim()))
      contacts.add(c2.trim());

    Optional<Customer> opt = Optional.empty();
    if (!contacts.isEmpty()) {
      LocalDateTime since = LocalDateTime.now().minusWeeks(2);
      List<String> statuses = List.of("TEMPORARY", "PENDING");
      List<Customer> recent = customerRepository.findByContactsWithOrdersSinceAndStatus(contacts, since, statuses);
      if (recent != null && !recent.isEmpty()) {
        // copy into mutable list then pick the customer with the most recent order
        List<Customer> mutable = new ArrayList<>(recent);
        mutable.sort((a, b) -> {
          LocalDateTime ma = a.getOrders().stream().map(Order::getDate).max(LocalDateTime::compareTo)
              .orElse(LocalDateTime.MIN);
          LocalDateTime mb = b.getOrders().stream().map(Order::getDate).max(LocalDateTime::compareTo)
              .orElse(LocalDateTime.MIN);
          return mb.compareTo(ma);
        });
        opt = Optional.of(mutable.get(0));
        requestDTO.setCustomerId(opt.get().getCustomerId());
      }
    }

    if (opt.isEmpty()) {
      // new customer
      Customer newCustomer = createNewCustomer(requestDTO, userDto);
      opt = Optional.of(newCustomer);
    }
    return createNewOrder(requestDTO, opt);
  }

  private Customer createNewCustomer(CustomerRequestDTO requestDTO, UserDto userDto) {
    Customer newCustomer = mapperService.map(requestDTO, Customer.class);
    if (newCustomer.getUser() == null) {
      newCustomer.setUser(mapperService.map(userDto, User.class));
    }
    return customerRepository.save(newCustomer);
  }

  // 2. Create and Save Order
  private Object createNewOrder(CustomerRequestDTO requestDTO, Optional<Customer> opt) {
    Order order = new Order();
    order.setCustomer(opt.get());
    order.setDate(LocalDateTime.now());
    if (requestDTO.getCustomerId() == null) {
      order.setStatus("PENDING");
    } else {
      order.setStatus("TEMPORARY");
    }
    order.setRemark(requestDTO.getRemark());
    order.setTrackingId(generateTrackingId());
    order.setTotalPrice(requestDTO.getTotalPrice());

    Order savedOrder = orderRepository.save(order);

    // 3. Save Order Details
    List<OrderDetails> orderDetailsList = requestDTO.getItems().stream()
        .map(item -> {
          Product product = productRepository.findAllByProductId(item.getProductId());
          if (product == null) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Product not found with id: " + item.getProductId());
          }

          OrderDetails orderDetails = new OrderDetails();
          orderDetails.setOrder(savedOrder);
          orderDetails.setProduct(product);
          orderDetails.setQty(item.getQty());
          orderDetails.setTotal(item.getTotal());

          return orderDetails;
        })
        .collect(Collectors.toList());

    orderDetailsRepository.saveAll(orderDetailsList);

    savedOrder.setOrderDetails(orderDetailsList);
    // 4. Prepare and Return Response - Return DTO instead of entity to avoid
    // circular references
    OrderDtoGet orderDtoGet = mapperService.map(savedOrder, OrderDtoGet.class);
    return orderDtoGet;
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

  /**
   * Return the first non-blank contact from the request, trimmed, or null if
   * none.
   */
  private String pickFirstNonBlankContact(CustomerRequestDTO requestDTO) {
    if (requestDTO == null)
      return null;
    String c1 = requestDTO.getContact01();
    if (c1 != null && !c1.trim().isEmpty())
      return c1.trim();
    String c2 = requestDTO.getContact02();
    if (c2 != null && !c2.trim().isEmpty())
      return c2.trim();
    return null;
  }

  @Override
  public Object updateCustomer(Integer id, CustomerRequestDTO requestDTO) {
    try{
      Optional<Customer> customerOptional = customerRepository.findById(id);
      if (customerOptional.isPresent()) {
        Customer customer = customerOptional.get();
        Customer requestCustomer = mapperService.map(requestDTO, Customer.class);
        Integer orderId = requestDTO.getItems().get(0).getOrderId();
        Order reqOrder =null;
        for(Order order : customer.getOrders()){
          if(order.getOrderId().equals(orderId)){
            reqOrder = order;
            reqOrder.setStatus("PENDING");
            requestCustomer.setDate(customer.getDate());
          }
        }
        //customerRepository.save(requestCustomer);
        orderRepository.save(reqOrder);
        return mapperService.map(requestCustomer, CustomerDtoGet.class);
      } else {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found");
      }
    }catch(Exception e){
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error updating customer: " + e.getMessage());
    }
  }

}
