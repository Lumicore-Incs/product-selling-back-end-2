package com.selling.service.impl;

import static com.selling.dto.ApiResponse.success;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.selling.dto.CustomerDto;
import com.selling.dto.ProductDto;
import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDetailsDtoGet;
import com.selling.dto.get.OrderDtoGet;
import com.selling.model.Order;
import com.selling.model.OrderDetails;
import com.selling.model.User;
import com.selling.repository.OrderDetailsRepo;
import com.selling.repository.OrderRepo;
import com.selling.service.OrderService;
import com.selling.util.MapperService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
  private final OrderRepo orderRepo;
  private final OrderDetailsRepo orderDetailsRepo;
  private final MapperService mapperService;
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
        .findByCustomerUser((userDto == null) ? null : mapperService.map(userDto, User.class));

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
        .findByCustomerUser((userDto == null) ? null : mapperService.map(userDto, User.class));
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
  public void updateOrderDetails() {
    List<Order> recentOrders = orderRepo.findTop200ByOrderByOrderIdDesc();
    for (Order order : recentOrders) {
      if (!(order.getStatus().equals("Delivered") || order.getStatus().equals("Failed to Deliver")
          || order.getStatus().equals("NotFound"))) {
        String value = checkTrackingStatus(order.getTrackingId());
        // Only update if we got a valid status (not null due to API failure)
        if (value != null && !value.equals(order.getStatus())) {
          order.setStatus(value);
          // sendMassage("0782862763", value);
          orderRepo.save(order);
        }
      }
    }
  }

  // private void sendMassage(String contact01, String value) {
  // Twilio.init("AC9ef6af7b744f3ecd66012d2fe992fc72",
  // "8adb10540bc3d7cdab2ac906db6e148d");
  // String whatsappNumber = "whatsapp:" + contact01;
  // String whatsappFromNumber = "whatsapp:+14155238886";
  // System.out.println(contact01);
  // try {
  // Message message = Message.creator(
  // new PhoneNumber(whatsappNumber),
  // new PhoneNumber(whatsappFromNumber),// Your Twilio WhatsApp number
  // "Your order has been : " + value
  // ).create();
  //
  // System.out.println("WhatsApp message sent! SID: "+ message.getSid());
  // } catch (Exception e) {
  // System.err.println("Failed to send WhatsApp message: " + e.getMessage());
  // }

  // Twilio.init("AC9ef6af7b744f3ecd66012d2fe992fc72",
  // "8adb10540bc3d7cdab2ac906db6e148d");
  // String toNumber = contact01; // අංකයේ format: "+94771234567" (ජාත්‍යන්තර
  // ආකාරයෙන්)
  // String fromNumber = "+12564820760"; // ඔබගේ Twilio SMS-enabled අංකය
  //
  // try {
  // Message message = Message.creator(
  // new PhoneNumber(toNumber), // ලබන්නාගේ අංකය
  // new PhoneNumber(fromNumber), // Twilio අංකය
  // "Your order has been: " + value // පණිවිඩය
  // ).create();
  //
  // System.out.println("SMS sent! SID: " + message.getSid());
  // } catch (Exception e) {
  // System.err.println("Failed to send SMS: " + e.getMessage());
  // }

  // String formattedNumber = formatPhoneNumber(contact01);
  //
  // TextMessage message = new TextMessage(
  // senderId,
  // formattedNumber,
  // value
  // );
  //
  // try {
  // SmsSubmissionResponse response =
  // vonageClient.getSmsClient().submitMessage(message);
  //
  // response.getMessages().forEach(msg -> {
  // System.out.printf("Message sent to %s. ID: %s, Status: %s%n",
  // msg.getTo(),
  // msg.getStatus());
  // });
  // } catch (Exception e) {
  // System.err.println("Error sending SMS: " + e.getMessage());
  // throw new RuntimeException("Failed to send SMS", e);
  // }

  // }

  // In OrderServiceImpl.java
  // private String formatPhoneNumber(String phoneNumber) {
  // Remove any non-digit characters
  // String digits = phoneNumber.replaceAll("\\D", "");
  // Handle Sri Lankan numbers (assumes numbers starting with 0 or +94)
  // if (digits.startsWith("0")) {
  // return "+94" + digits.substring(1);
  // } else if (!digits.startsWith("+")) {
  // return "+" + digits;
  // }
  // return digits;
  // }

  private String checkTrackingStatus(String id) {
    String apiUrl = "https://api.transexpress.lk/api/v1/tracking?waybill_id=" + id;
    HttpURLConnection connection = null;

    try {
      URL url = new URL(apiUrl);
      connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setRequestProperty("Accept", "application/json");

      int responseCode = connection.getResponseCode();

      if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        JsonObject jsonResponse = JsonParser.parseReader(in).getAsJsonObject();
        in.close();

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
      } else {
        System.out.println("API request failed with response code: " + responseCode + " for tracking ID: " + id);
        return null; // Don't update status on API failure
      }

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Error while calling API for tracking ID " + id + ": " + e.getMessage());
      return null; // Don't update status on API exception
    } finally {
      // Clean up connection if it was created
      if (connection != null) {
        try {
          connection.disconnect();
        } catch (Exception ignored) {
        }
      }
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

    if (Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN")) {
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

  // mapping now done inline via MapperService to reduce thin-wrapper boilerplate

  @Override
  public Object resolveDuplicateOrder(Integer orderId) {
    try {
      Order order = orderRepo.findById(orderId).orElse(null);
      if (order == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
      }

      // Only resolve if status is TEMPORARY or similar
      if ("TEMPORARY".equals(order.getStatus())) {
        order.setStatus("PENDING");
        orderRepo.save(order);
      }

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
      Order order = orderRepo.findById(orderId).orElse(null);
      if (order == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
      }

      List<OrderDetails> details = orderDetailsRepo.findByOrder(order);
      if (details != null && !details.isEmpty()) {
        orderDetailsRepo.deleteAll(details);
      }

      orderRepo.delete(order);

      return success("Order deleted successfully", null);
    } catch (ResponseStatusException rse) {
      throw rse;
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error deleting order: " + e.getMessage());
    }
  }

}
