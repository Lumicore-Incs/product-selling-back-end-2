package com.selling.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.selling.dto.ApiResponse;
import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDtoGet;
import com.selling.service.OrderService;
import com.selling.util.JWTTokenGenerator;
import com.selling.util.TokenStatus;

import lombok.RequiredArgsConstructor;

@CrossOrigin()
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {
  @Autowired
  private JWTTokenGenerator jwtTokenGenerator;

  @Autowired
  private final OrderService orderService;

  @GetMapping
  public ResponseEntity<Object> getAllTodayCustomer(@RequestHeader(name = "Authorization") String authorizationHeader) {
    try {
      if (!jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
        return new ResponseEntity<>(TokenStatus.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
      }
      UserDto userDto = jwtTokenGenerator.getUserFromJwtToken(authorizationHeader);
      System.out.println(userDto.getRole());
      if (Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN")
          || Objects.equals(userDto.getRole(), "Admin")) {

        List<OrderDtoGet> allCustomer = orderService.getAllTodayOrder();
        return new ResponseEntity<>(allCustomer, HttpStatus.OK);
      } else {

        List<OrderDtoGet> allCustomer = orderService.getAllTodayOrderByUserId(userDto);
        return new ResponseEntity<>(allCustomer, HttpStatus.OK);
      }
    } catch (Exception e) {
      return new ResponseEntity<>("Error retrieving products: " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PutMapping("/{id}/resolve")
  public ResponseEntity<Object> resolveDuplicateOrder(
      @RequestHeader(name = "Authorization") String authorizationHeader,
      @PathVariable Integer id) {
    try {
      if (!jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Invalid token", 401));
      }
      Object result = orderService.resolveDuplicateOrder(id);
      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (ResponseStatusException rse) {
      return new ResponseEntity<>(rse.getReason(), rse.getStatusCode());
    } catch (Exception e) {
      return new ResponseEntity<>("Error resolving order: " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/allCustomer")
  public ResponseEntity<Object> getAllCustomer(@RequestHeader(name = "Authorization") String authorizationHeader) {
    try {
      if (!jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
        return new ResponseEntity<>(TokenStatus.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
      }
      UserDto userDto = jwtTokenGenerator.getUserFromJwtToken(authorizationHeader);
      if (Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN")
          || Objects.equals(userDto.getRole(), "Admin")) {

        List<OrderDtoGet> allCustomer = orderService.getAllOrder();
        return new ResponseEntity<>(allCustomer, HttpStatus.OK);
      } else {

        List<OrderDtoGet> allCustomer = orderService.getAllOrderByUserId(userDto);
        return new ResponseEntity<>(allCustomer, HttpStatus.OK);
      }
    } catch (Exception e) {
      return new ResponseEntity<>("Error retrieving products: " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/duplicate")
  public ResponseEntity<Object> getDuplicateCustomerOrders(
      @RequestHeader(name = "Authorization") String authorizationHeader) {
    try {
      if (!jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Invalid token", 401));
      }
      UserDto userDto = jwtTokenGenerator.getUserFromJwtToken(authorizationHeader);
      List<OrderDtoGet> temporaryOrders = orderService.getTemporaryOrders(userDto);

      return new ResponseEntity<>(temporaryOrders, HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>("Error retrieving products: " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Object> deleteOrder(@RequestHeader(name = "Authorization") String authorizationHeader,
      @PathVariable Integer id) {
    try {
      if (!jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error("Invalid token", 401));
      }
      UserDto userDto = jwtTokenGenerator.getUserFromJwtToken(authorizationHeader);
      if (!(Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN")
          || Objects.equals(userDto.getRole(), "Admin"))) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error("Forbidden: admin only", 403));
      }

      Object result = orderService.deleteOrder(id);
      return new ResponseEntity<>(result, HttpStatus.OK);
    } catch (ResponseStatusException rse) {
      return new ResponseEntity<>(rse.getReason(), rse.getStatusCode());
    } catch (Exception e) {
      return new ResponseEntity<>("Error deleting order: " + e.getMessage(),
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
