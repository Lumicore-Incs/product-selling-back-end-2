package com.selling.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.selling.model.Customer;
import com.selling.model.Order;
import com.selling.model.User;

public interface OrderRepo extends JpaRepository<Order, Integer> {
  @EntityGraph(attributePaths = { "customer", "orderDetails", "orderDetails.product" })
  List<Order> findByCustomerUser(User userId);

  List<Order> findAllByOrderByOrderIdDesc();

  List<Order> findTop200ByOrderByOrderIdDesc();

  int countByStatusAndDateBetween(String deliver, LocalDateTime startOfMonth, LocalDateTime now);

  int countByCustomerUserEmailAndStatusAndDateBetween(String email, String deliver, LocalDateTime startOfMonth,
      LocalDateTime now);
      
  // Convenience overload for a single customer
  @Query("SELECT o FROM Order o WHERE o.customer = :customer AND o.date > :since ORDER BY o.date DESC")
  List<Order> findRecentOrdersForCustomer(@Param("customer") Customer customer, @Param("since") LocalDateTime since);
}
