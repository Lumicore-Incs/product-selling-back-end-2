package com.selling.repository;

import com.selling.dto.get.ExcelTypeDto;
import com.selling.model.Customer;
import com.selling.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CustomerRepo extends JpaRepository<Customer, Integer> {
    List<Customer> findAllByUserId(Long id);

    @Query("SELECT new com.selling.dto.get.ExcelTypeDto(" +
            "c.customerId, c.name, c.address, c.contact01, c.contact02,null) " +
            "FROM Customer c " +
            "JOIN c.orders o " +
            "JOIN o.orderDetails od " +
            "WHERE c.status = 'print'" +
            "ORDER BY od.qty asc ")
    List<ExcelTypeDto> findPendingOrdersWithQuantities();

    @Query("SELECT o " +
            "FROM Customer c " +
            "JOIN c.orders o " +
            "JOIN o.orderDetails od " +
            "WHERE c.status = 'pending'" +
            "ORDER BY od.qty ASC")
    List<Order> findPendingOrdersWithQuantities(@Param("productId") Integer productId);


    int countByUserId(Long id);

    List<Customer> findByUser_Id(Long id);


    List<Customer> findByContact01(String contact01);
}
