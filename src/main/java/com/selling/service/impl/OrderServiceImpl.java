package com.selling.service.impl;

import static com.selling.dto.ApiResponse.success;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.selling.dto.CustomerDto;
import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.PaginationResponse;
import com.selling.dto.ProductDto;
import com.selling.dto.TrackingDto;
import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDetailsDtoGet;
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
import com.selling.service.OrderService;
import com.selling.service.StockService;
import com.selling.util.MapperService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
  private final OrderRepo orderRepo;
  private final OrderDetailsRepo orderDetailsRepo;
  private final MapperService mapperService;
  private final ProductRepo productRepository;
  private final StockService stockService;
  private final CustomerRepo customerRepo;
  private final RestTemplate restTemplate;

  @Override
  public String generateOrderSerialNumber(Product product, UserDto userDto) {
    Order last = orderRepo.findTopBySerialNoStartingWithOrderBySerialNoDesc(userDto.getSerialPrefix());

    long nextNum = 1L;
    if (last != null && last.getSerialNo() != null) {
      String serial = last.getSerialNo();
      int i = serial.length() - 1;
      while (i >= 0 && Character.isDigit(serial.charAt(i)))
        i--;
      String numPart = (i < serial.length() - 1) ? serial.substring(i + 1) : null;
      try {
        if (numPart != null)
          nextNum = Long.parseLong(numPart) + 1L;
      } catch (NumberFormatException ignored) {
        nextNum = 1L;
      }
    }

    String prefix = userDto.getSerialPrefix() != null ? userDto.getSerialPrefix() : "XXX";
    String numFormatted = String.format("%05d", nextNum);
    return prefix + numFormatted;
  }

  @Override
  @Transactional
  public String trackingUpload(List<TrackingDto> trackingList) {
    try {
      List<String> results = new ArrayList<>();
      int successCount = 0;
      int failureCount = 0;

      for (TrackingDto trackingDto : trackingList) {
        String result = updateTrackingForOrder(trackingDto);
        results.add(result);

        if (result.startsWith("Success")) {
          successCount++;
        } else {
          failureCount++;
        }
      }

      return String.format("Completed: %d successful, %d failed. Details: %s",
          successCount, failureCount, String.join("; ", results));

    } catch (Exception e) {
      return "Error processing tracking upload: " + e.getMessage();
    }
  }

  @Override
  public ArrayList<String> getUrgentOrders(Long id, String date) {
    return null;
  }

  private String updateTrackingForOrder(TrackingDto trackingDto) {
    try {
      // serialNo (orderId in DTO) මගින් order එක සොයාගැනීම
      String serialNo = trackingDto.getOrderId();
      String wayBillNo = trackingDto.getWayBillNo();

      if (serialNo == null || serialNo.trim().isEmpty()) {
        return "Failed: Serial number is empty for waybill: " + wayBillNo;
      }

      if (wayBillNo == null || wayBillNo.trim().isEmpty()) {
        return "Failed: Waybill number is empty for order: " + serialNo;
      }

      // Order එක සොයාගැනීම
      Order order = orderRepo.findBySerialNo(serialNo)
          .orElse(null);

      if (order == null) {
        return "Failed: Order not found with serial number: " + serialNo;
      }

      // trackingId update කිරීම
      order.setTrackingId(wayBillNo);
      order.setWeyBillId(wayBillNo);
      orderRepo.save(order);

      return String.format("Success: Order %s updated with tracking ID %s",
          serialNo, wayBillNo);

    } catch (Exception e) {
      return "Failed: Error updating order " + trackingDto.getOrderId() + " - " + e.getMessage();
    }
  }

  // private final VonageClient vonageClient;

  @Value("${vonage.sms.sender}")
  private String senderId;

  @Override
  public List<OrderDtoGet> getAllTodayOrder() {
    List<OrderDtoGet> customerDtoGetList = new ArrayList<>();
    List<Order> allCustomer = orderRepo.findAll();

    // Get today's date (without time)
    LocalDate today = LocalDate.now();

    for (Order order : allCustomer) {
      // Convert order date to LocalDate (if stored as LocalDateTime)
      LocalDate orderDate = order.getDate().toLocalDate();

      // Check if year, month, and day match
      if (orderDate.equals(today)) {
        if (order.getCustomer() != null) {
          OrderDtoGet map = mapperService.map(order, OrderDtoGet.class);
          map.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
          map.setOrderDetails(getOrderDetailsData(order));
          customerDtoGetList.add(map);
        }
      }
    }
    return customerDtoGetList;
  }

  @Override
  public List<OrderDtoGet> getAllOrder() {
    List<OrderDtoGet> customerDtoGetList = new ArrayList<>();
    List<Order> allCustomer = orderRepo.findAll();
    for (Order order : allCustomer) {
      if (order.getCustomer() != null) {
        OrderDtoGet map = mapperService.map(order, OrderDtoGet.class);
        map.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
        map.setOrderDetails(getOrderDetailsData(order));
        customerDtoGetList.add(map);
      }
    }
    return customerDtoGetList;
  }

  @Override
  public List<OrderDtoGet> getAllTodayOrderByUserId(UserDto userDto) {
    List<OrderDtoGet> orderDtoGetList = new ArrayList<>();

    LocalDate today = LocalDate.now();
    List<Order> userOrders = orderRepo
        .findByUser((userDto == null) ? null : mapperService.map(userDto, User.class));

    for (Order order : userOrders) {
      LocalDate orderDate = order.getDate().toLocalDate();

      if (orderDate.equals(today)) {
        if (order.getCustomer() != null) {
          OrderDtoGet map = mapperService.map(order, OrderDtoGet.class);
          map.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
          map.setOrderDetails(getOrderDetailsData(order));
          orderDtoGetList.add(map);
        }
      }
    }
    return orderDtoGetList;
  }

  @Override
  public List<OrderDtoGet> getAllOrderByUserId(UserDto userDto) {
    List<OrderDtoGet> orderDtoGetList = new ArrayList<>();
    List<Order> userOrders = orderRepo
        .findByUser((userDto == null) ? null : mapperService.map(userDto, User.class));
    for (Order order : userOrders) {
      if (order.getCustomer() != null) {
        OrderDtoGet map = mapperService.map(order, OrderDtoGet.class);
        map.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
        map.setOrderDetails(getOrderDetailsData(order));
        orderDtoGetList.add(map);
      }
    }
    return orderDtoGetList;
  }

  @Override
  public PaginationResponse<OrderDtoGet> getAllTodayOrderPaginated(int page, int size, String search, String status,
      Integer productId) {
    List<Order> allOrders = orderRepo.findAll();
    return buildFilteredPaginatedResponse(allOrders, page, size, search, status, productId, true, null);
  }

  @Override
  public PaginationResponse<OrderDtoGet> getAllTodayOrderByUserIdPaginated(UserDto userDto, int page, int size,
      String search, String status, Integer productId) {
    List<Order> userOrders = orderRepo.findByUser(mapperService.map(userDto, User.class));
    return buildFilteredPaginatedResponse(userOrders, page, size, search, status, productId, true, null);
  }

  @Override
  public PaginationResponse<OrderDtoGet> getAllOrderPaginated(int page, int size, String search, String status,
      Integer productId) {
    List<Order> allOrders = orderRepo.findAllByOrderByOrderIdDesc();
    return buildFilteredPaginatedResponse(allOrders, page, size, search, status, productId, false, null);
  }

  @Override
  public PaginationResponse<OrderDtoGet> getAllOrderByUserIdPaginated(UserDto userDto, int page, int size,
      String search, String status, Integer productId) {
    List<Order> userOrders = orderRepo.findByUserOrderByOrderIdDesc(mapperService.map(userDto, User.class));
    return buildFilteredPaginatedResponse(userOrders, page, size, search, status, productId, false, null);
  }

  /**
   * Shared helper: filter + paginate a list of Order entities.
   *
   * @param source    raw list of orders to filter
   * @param page      zero-based page number
   * @param size      page size
   * @param search    LIKE match against customerName, weyBillId, contact01,
   *                  contact02
   * @param status    exact match on order status (null → no filter)
   * @param productId filter orders containing at least one detail for this
   *                  product (null → no filter)
   * @param todayOnly if true, restrict to orders whose date is today
   * @param ignored   reserved (pass null)
   */
  private PaginationResponse<OrderDtoGet> buildFilteredPaginatedResponse(
      List<Order> source, int page, int size,
      String search, String status, Integer productId,
      boolean todayOnly, Object ignored) {
    try {
      // Resolve order IDs that contain the requested product (single DB call)
      final java.util.Set<Integer> productOrderIds;
      if (productId != null) {
        productOrderIds = new java.util.HashSet<>(orderRepo.findOrderIdsByProductId(productId));
      } else {
        productOrderIds = null;
      }

      LocalDate today = LocalDate.now();
      String searchLower = (search != null && !search.isEmpty()) ? search.toLowerCase() : null;
      String statusFilter = (status != null && !status.isEmpty() && !status.equals("ALL STATUS")) ? status : null;

      List<Order> filtered = source.stream()
          .filter(order -> {
            if (order.getCustomer() == null)
              return false;

            // Today-only restriction
            if (todayOnly && !order.getDate().toLocalDate().equals(today))
              return false;

            // Exact status match
            if (statusFilter != null && !statusFilter.equals(order.getStatus()))
              return false;

            // productId: must appear in at least one order detail
            if (productOrderIds != null && !productOrderIds.contains(order.getOrderId()))
              return false;

            // Multi-field search: customerName, weyBillId, contact01, contact02
            if (searchLower != null) {
              String name = order.getCustomer().getName() != null ? order.getCustomer().getName().toLowerCase() : "";
              String waybill = order.getWeyBillId() != null ? order.getWeyBillId().toLowerCase() : "";
              String contact1 = order.getCustomer().getContact01() != null
                  ? order.getCustomer().getContact01().toLowerCase()
                  : "";
              String contact2 = order.getCustomer().getContact02() != null
                  ? order.getCustomer().getContact02().toLowerCase()
                  : "";
              if (!name.contains(searchLower) && !waybill.contains(searchLower)
                  && !contact1.contains(searchLower) && !contact2.contains(searchLower)) {
                return false;
              }
            }

            return true;
          })
          .collect(Collectors.toList());

      long totalElements = filtered.size();
      int totalPages = (totalElements == 0) ? 0 : (int) Math.ceil((double) totalElements / size);

      if (page < 0)
        page = 0;
      if (totalElements > 0 && page >= totalPages)
        page = totalPages - 1;

      int startIndex = page * size;
      int endIndex = (int) Math.min((long) startIndex + size, totalElements);

      List<OrderDtoGet> content = filtered.subList(startIndex, endIndex).stream()
          .map(order -> {
            OrderDtoGet dto = mapperService.map(order, OrderDtoGet.class);
            dto.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
            dto.setOrderDetails(getOrderDetailsData(order));
            return dto;
          })
          .collect(Collectors.toList());

      PaginationResponse<OrderDtoGet> response = new PaginationResponse<>();
      response.setContent(content);
      response.setPageNumber(page);
      response.setPageSize(size);
      response.setTotalElements(totalElements);
      response.setTotalPages(totalPages);
      response.setLastPage(totalElements == 0 || page == totalPages - 1);
      return response;

    } catch (Exception e) {
      System.out.println("Error fetching paginated orders: " + e.getMessage());
      throw new RuntimeException("Error fetching paginated orders", e);
    }
  }

  @Async
  @Override
  public void updateOrderDetails(UserDto userDto) {
    List<Order> recentOrders = null;
    if (userDto.getRole().equals("ADMIN") || userDto.getRole().equals("admin") || userDto.getRole().equals("super user")
        || userDto.getRole().equals("SUPER USER")) {
      recentOrders = orderRepo.findAllByOrderByOrderIdDesc();
    } else {
      recentOrders = orderRepo.findByUserIdOrderByOrderIdDesc(userDto.getId());
    }

    // Process up to 5 orders in parallel
    List<CompletableFuture<Void>> futures = recentOrders.stream()
        .filter(order -> !(order.getStatus().equals("Delivered") || order.getStatus().equals("Failed to Deliver")
            || order.getStatus().equals("NotFound"))
            && !order.getTrackingId().equals("TRK"))
        .map(order -> CompletableFuture.runAsync(() -> {
          String value = checkTrackingStatus(order.getTrackingId());
          if (value != null && !value.equals(order.getStatus())) {
            order.setStatus(value);
            order.setDeliveryDate(LocalDateTime.now());
            orderRepo.save(order);
          }
        }))
        .collect(Collectors.toList());

    // Wait for all to complete (with timeout)
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
        .orTimeout(5, TimeUnit.MINUTES) // 5 minute timeout for all
        .exceptionally(ex -> {
          System.err.println("Error updating tracking status: " + ex.getMessage());
          return null;
        })
        .join();
  }

  private String checkTrackingStatus(String id) {
    String apiUrl = "https://api.transexpress.lk/api/v1/tracking?waybill_id=" + id;

    try {
      String response = restTemplate.getForObject(apiUrl, String.class);
      JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();

      JsonArray dataArray = jsonResponse.getAsJsonArray("data");
      String lastStatus = null;

      for (int i = 0; i < dataArray.size(); i++) {
        JsonObject dataItem = dataArray.get(i).getAsJsonObject();
        if ("tracking_history".equals(dataItem.get("key").getAsString())) {
          JsonArray historyArray = dataItem.getAsJsonArray("value");

          if (historyArray.size() > 0) {
            JsonObject lastStatusItem = historyArray.get(historyArray.size() - 1).getAsJsonObject();
            lastStatus = lastStatusItem.get("status_name").getAsString();
          }
          break;
        }
      }

      if (lastStatus != null) {
        return lastStatus;
      } else {
        // Don't return "NotFound" - preserve existing status when API has no data
        System.out.println("No tracking history found for ID: " + id);
        return null; // Return null to indicate "no update needed"
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error while calling API for tracking ID " + id + ": " + e.getMessage());
      return null; // Don't update status on API exception
    }
  }

  private List<OrderDetailsDtoGet> getOrderDetailsData(Order orderId) {
    List<OrderDetails> byOrder = orderDetailsRepo.findByOrder(orderId);
    List<OrderDetailsDtoGet> orderDetailsDtoGetList = new ArrayList<>();
    for (OrderDetails orderDetails : byOrder) {
      if (orderDetails != null) {
        OrderDetailsDtoGet map = mapperService.map(orderDetails, OrderDetailsDtoGet.class);
        map.setProductId((orderDetails.getProduct() == null) ? null
            : mapperService.map(orderDetails.getProduct(), ProductDto.class));
        orderDetailsDtoGetList.add(map);
      }
    }
    return orderDetailsDtoGetList;
  }

  @Override
  public List<OrderDtoGet> getTemporaryOrders(UserDto userDto) {
    List<OrderDtoGet> orderDtoGetList = new ArrayList<>();
    List<Order> temporaryOrders = orderRepo.findByStatus("TEMPORARY");

    if (Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN")
        || Objects.equals(userDto.getRole(), "SUPER USER") || Objects.equals(userDto.getRole(), "SUPERUSER")) {
      for (Order order : temporaryOrders) {
        OrderDtoGet dto = mapperService.map(order, OrderDtoGet.class);
        dto.setOrderDetails(getOrderDetailsData(order));
        if (order.getCustomer() != null) {
          dto.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
        }
        orderDtoGetList.add(dto);
      }
    } else {
      for (Order order : temporaryOrders) {
        OrderDtoGet dto = mapperService.map(order, OrderDtoGet.class);
        if (order.getCustomer() != null) {
          dto.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
          dto.setOrderDetails(getOrderDetailsData(order));
        }
        if (order.getCustomer().getUser().getId().equals(userDto.getId())) {
          orderDtoGetList.add(dto);
        }
      }
    }
    return orderDtoGetList;
  }

  // here we have added order update flow to this same method
  @Override
  public Object resolveDuplicateOrder(Integer orderId, String userRole, CustomerRequestDTO requestDTO) {
    try {
      Order order = orderRepo.findById(orderId).orElse(null);
      if (order == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
      }
      // Only resolve if status is TEMPORARY or similar
      if ("TEMPORARY".equals(order.getStatus())) {
        order.setStatus("PENDING");
        order.getCustomer().setStatus("PENDING");

      }
      order.setTotalPrice(requestDTO.getTotalPrice());
      order.setRemark(requestDTO.getRemark());
      Order savedOrder = orderRepo.save(order);

      // 3. Save Order Details
      List<OrderDetails> orderDetailsList = requestDTO.getItems().stream()
          .map(item -> {
            Product product = productRepository.findAllByProductId(item.getProductId());
            if (product == null) {
              throw new ResponseStatusException(
                  HttpStatus.NOT_FOUND, "Product not found with id: " + item.getProductId());
            }

            OrderDetails orderDetails = new OrderDetails();
            orderDetails.setOrderDetailsId(item.getOrderDetailsId());
            orderDetails.setOrder(savedOrder);
            orderDetails.setProduct(product);
            orderDetails.setQty(item.getQty());
            orderDetails.setTotal(item.getTotal());

            return orderDetails;
          })
          .collect(Collectors.toList());

      orderDetailsRepo.saveAll(orderDetailsList);

      OrderDtoGet dto = mapperService.map(order, OrderDtoGet.class);
      if (order.getCustomer() != null) {
        dto.setCustomer(mapperService.map(order.getCustomer(), CustomerDto.class));
      }
      dto.setOrderDetails(getOrderDetailsData(order));
      return dto;
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error resolving order: " + e.getMessage());
    }
  }

  @Override
  @Transactional
  public Object deleteOrder(Integer orderId) {
    try {
      Order order = orderRepo.findAllByOrderId(orderId);

      if (order == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
      }
      Customer customer = order.getCustomer();

      if (order.getStatus().equals("PENDING") || order.getStatus().equals("TEMPORARY")) {
        List<OrderDetails> details = orderDetailsRepo.findByOrder(order);
        if (details != null && !details.isEmpty()) {
          System.out.println("delete");
          orderDetailsRepo.deleteAll(details);
        }
        orderRepo.delete(order);
        stockService.updateStockQty(details);

        return success("Order deleted successfully", null);
      }
      return null;

    } catch (ResponseStatusException rse) {
      System.out.println("ok " + rse.getMessage());
      throw rse;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting order: " + e.getMessage());
    }
  }

}
