package com.selling.controller;

import com.selling.dto.ResetPasswordDto;
import com.selling.dto.UserDto;
import com.selling.dto.get.UserDtoForGet;
import com.selling.service.UserService;
import com.selling.util.JWTTokenGenerator;
import com.selling.util.TokenStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/user")
public class AuthController {

    private final UserService userService;

    private final JWTTokenGenerator jwtTokenGenerator;

    @Autowired
    public AuthController(UserService userService, JWTTokenGenerator jwtTokenGenerator) {
        this.userService = userService;
        this.jwtTokenGenerator = jwtTokenGenerator;
    }


    @PostMapping("/login")
    public ResponseEntity<Object> postLogin(@RequestBody UserDto dto) {
        UserDto user = userService.userLogin(dto);
        Map<String, String> response = new HashMap<>();
        if (user == null) {
            response.put("massage", "wrong details");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        } else {
            String token = this.jwtTokenGenerator.generateJwtToken(user);
            response.put("token", token);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        }
    }


    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@RequestBody UserDto userDto) {
        UserDto isUser = this.userService.findUserByName(userDto.getEmail(), userDto.getName());
        if (isUser == null) {
            UserDtoForGet dto = this.userService.registerUser(userDto);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } else {
            System.out.println(isUser);
            return new ResponseEntity<>("Email is All Ready exist", HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/get_user_info_by_token")
    public ResponseEntity<Object> getUserInfoByToken(@RequestHeader(name = "Authorization") String authorizationHeader) {
        if (this.jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
            UserDto userFromJwtToken = this.jwtTokenGenerator.getUserFromJwtToken(authorizationHeader);
            return new ResponseEntity<>(userFromJwtToken, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(TokenStatus.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
        }
    }

    @PutMapping("/update/{userId}")
    public ResponseEntity<Object> updateUser(@PathVariable Long userId, @RequestBody UserDto userDto, @RequestHeader(name = "Authorization") String authorizationHeader) {
        if (this.jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
            UserDtoForGet dto = this.userService.updateUser(userDto, userId);
            return new ResponseEntity<>(dto, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(TokenStatus.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping("/get_all_user")
    public ResponseEntity<Object> getAllUser(@RequestHeader(name = "Authorization") String authorizationHeader) {
        if (this.jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
            List<UserDtoForGet> allUsers = this.userService.getAllUser();
            return new ResponseEntity<>(allUsers, HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>(TokenStatus.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
        }
    }

    @PostMapping("/send")
    public String sendOtp(@RequestParam String email) {
        boolean isSave=userService.sendOtpToEmail(email);
        if (isSave){
            return "OTP sent successfully to " + email;
        }else {
            return "OTP sent failed email check again..!";
        }
    }

    @PostMapping("/validate")
    public String validateOtp(@RequestParam String email, @RequestParam String otp) {
        boolean isValid = userService.validateOtp(email, otp);
        if (isValid) {
            return "OTP is valid";
        } else {
            return "OTP is invalid";
        }
    }

    @PostMapping("/reset")
    public String resetPassword(@RequestBody ResetPasswordDto dto) {

        // First validate OTP
        boolean isValid = userService.validateOtp(dto.getEmail(), dto.getOtp());
        if (!isValid) {
            throw new RuntimeException("Invalid OTP");
        }

        // Change password
        userService.changePassword(dto.getEmail(), dto.getPassword());

        return "Password changed successfully";
    }
}
