package com.selling.controller;

import com.selling.dto.UserDto;
import com.selling.dto.get.OrderDtoGet;
import com.selling.service.OrderService;
import com.selling.util.JWTTokenGenerator;
import com.selling.util.TokenStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

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
            if (Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN") || Objects.equals(userDto.getRole(), "Admin")){

                List<OrderDtoGet> allCustomer = orderService.getAllTodayOrder();
                return new ResponseEntity<>(allCustomer, HttpStatus.OK);
            }else {

                List<OrderDtoGet> allCustomer = orderService.getAllTodayOrderByUserId(userDto);
                return new ResponseEntity<>(allCustomer, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving products: " + e.getMessage(),
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
            if (Objects.equals(userDto.getRole(), "admin") || Objects.equals(userDto.getRole(), "ADMIN") || Objects.equals(userDto.getRole(), "Admin")){

                List<OrderDtoGet> allCustomer = orderService.getAllOrder();
                return new ResponseEntity<>(allCustomer, HttpStatus.OK);
            }else {

                List<OrderDtoGet> allCustomer = orderService.getAllOrderByUserId(userDto);
                return new ResponseEntity<>(allCustomer, HttpStatus.OK);
            }
        } catch (Exception e) {
            return new ResponseEntity<>("Error retrieving products: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
