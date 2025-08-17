package com.selling.repository;

import com.selling.model.Order;
import com.selling.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Integer> {
    @EntityGraph(attributePaths = {"customer", "orderDetails", "orderDetails.product"})
    List<Order> findByCustomerUser(User userId);

    List<Order> findAllByOrderByOrderIdDesc();

    List<Order> findTop200ByOrderByOrderIdDesc();

    int countByStatusAndDateBetween(String deliver, LocalDateTime startOfMonth, LocalDateTime now);

    int countByCustomerUserEmailAndStatusAndDateBetween(String email, String deliver, LocalDateTime startOfMonth, LocalDateTime now);
}
