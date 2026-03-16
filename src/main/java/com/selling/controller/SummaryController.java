package com.selling.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import com.selling.dto.get.UserSummeryGet;
import com.selling.service.SummaryService;
import com.selling.util.JWTTokenGenerator;
import com.selling.util.TokenStatus;
import lombok.RequiredArgsConstructor;

@CrossOrigin()
@RestController
@RequiredArgsConstructor
@RequestMapping("/summary")
public class SummaryController {

    @Autowired
    private final JWTTokenGenerator jwtTokenGenerator;
    private final SummaryService summaryService;

    @GetMapping("/getUserDetails")
    public ResponseEntity<Object> getSummaryDetails(@RequestHeader(name = "Authorization") String authorizationHeader,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Integer month) {
        try {
            if (!jwtTokenGenerator.validateJwtToken(authorizationHeader)) {
                return new ResponseEntity<>(TokenStatus.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
            }

            if (month == 0) {
                month = java.time.LocalDate.now().getMonthValue(); // current month (1-12)
            }

            
            UserSummeryGet entities = summaryService.getSummaryDetails(id, month);

            return new ResponseEntity<>(entities, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
