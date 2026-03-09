package com.selling.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.selling.model.Customer;
import com.selling.model.Order;
import com.selling.model.User;

public interface OrderRepo extends JpaRepository<Order, Integer> {
  @EntityGraph(attributePaths = { "customer", "orderDetails", "orderDetails.product" })
  List<Order> findByUser(User userId);

  Order findAllByOrderId(Integer orderId);

  List<Order> findByUserIdOrderByOrderIdDesc(Long userId);

  List<Order> findTop200ByOrderByOrderIdDesc();

  // Get last saved order (most recent) to determine global serial numeric part
  Order findTopBySerialNoStartingWithOrderBySerialNoDesc(String prefix);

  // Find orders by status
  @Query("""
          SELECT o
          FROM Order o
          JOIN o.orderDetails od
          JOIN o.customer c
          WHERE o.status = :status
          AND c.status = :status
      """)
  List<Order> findByStatus(@Param("status") String status);

  // serialNo මගින් Order සොයාගැනීම
  Optional<Order> findBySerialNo(String serialNo);

  Order findByCustomer(Customer customer);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.date >= :startDate
          AND o.user.id = :userId
      """)
  Long sumQtyByUserAfterDate(
      @Param("userId") Long userId,
      @Param("startDate") LocalDateTime startDate);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.date >= :startDate
      """)
  Long sumQtyAfterDate(@Param("startDate") LocalDateTime startDate);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.date BETWEEN :start AND :end
      """)
  Long sumTodayQty(
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.date BETWEEN :start AND :end
          AND o.user.id = :userId
      """)
  Long sumTodayQtyByUser(
      @Param("userId") Long userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.status = :status
          AND o.deliveryDate >= :start
      """)
  Long countByStatusAndDateBetween(
      @Param("status") String status,
      @Param("start") LocalDateTime startOfMonth);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.status = :status
          AND o.deliveryDate >= :start
          AND o.user.id = :userId
      """)
  Long countByCustomerUserEmailAndStatusAndDateBetween(
      @Param("status") String status,
      @Param("userId") Long userId,
      @Param("start") LocalDateTime startOfMonth);

  @Query("""
          SELECT o
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.user.id = :userId
      """)
  List<Order> findByUserId(@Param("userId") Long userId);

  List<Order> findAllByOrderByOrderIdDesc();

  @Query("SELECT DISTINCT o.orderId FROM Order o JOIN o.orderDetails od WHERE od.product.productId = :productId ORDER BY o.orderId DESC")
  List<Integer> findOrderIdsByProductId(@Param("productId") Integer productId);


  List<Order> findByUserOrderByOrderIdDesc(User map);

}
