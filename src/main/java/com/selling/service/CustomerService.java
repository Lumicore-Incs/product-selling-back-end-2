package com.selling.service;

import com.selling.dto.CustomerRequestDTO;
import com.selling.dto.UserDto;
import com.selling.dto.get.CustomerDtoGet;

import java.util.List;

public interface CustomerService {
    Object saveCustomer(CustomerRequestDTO requestDTO, UserDto userDto);

    List<CustomerDtoGet> getAllCustomer();

    List<CustomerDtoGet> getAllCustomerByUserId(UserDto userDto);

    boolean deleteCustomer(Integer id);
}
