package com.selling.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
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

  // දවසකට අදාල orders
  @Query("SELECT o FROM Order o WHERE o.user = :user AND DATE(o.date) = DATE(:date)")
  List<Order> findByUserAndDate(
          @Param("user") User user,
          @Param("date") LocalDate date
  );

  // month එකකට අදාල orders
  @Query("SELECT o FROM Order o " +
          "WHERE o.user = :user " +
          "AND YEAR(o.date) = :year " +
          "AND MONTH(o.date) = :month")
  List<Order> findByUserAndMonth(
          @Param("user") User user,
          @Param("year") int year,
          @Param("month") int month
  );

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
          AND o.deliveryDate BETWEEN :start AND :end
      """)
  Long countByStatusAndDateRange(
      @Param("status") String status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.user.id = :userId
          AND o.status = :status
          AND o.deliveryDate BETWEEN :start AND :end
      """)
  Long countByCustomerUserIdAndStatusAndDateBetween(
      @Param("userId") Long userId,
      @Param("status") String status,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);
         
    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.date BETWEEN :start AND :end")
    List<Order> findByUserIdAndDateBetween(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

     @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.deliveryDate BETWEEN :start AND :end AND o.status = 'DELIVERED'")
    List<Order> findByUserIdAndDateBetweenByStatus(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  List<Order> findAllByOrderByOrderIdDesc();

  @Query("SELECT DISTINCT o.orderId FROM Order o JOIN o.orderDetails od WHERE od.product.productId = :productId ORDER BY o.orderId DESC")
  List<Integer> findOrderIdsByProductId(@Param("productId") Integer productId);


  List<Order> findByUserOrderByOrderIdDesc(User map);

  @Query("""
          SELECT COUNT(DISTINCT o.orderId)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.status = :status
          AND o.deliveryDate BETWEEN :start AND :end
      """)
  Long countByProcessingOrdersAndDateRange(
          @Param("status") String status,
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end);

  @Query("""
          SELECT COUNT(DISTINCT o.orderId)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.user.id = :userId
          AND o.status = :status
          AND o.deliveryDate BETWEEN :start AND :end
      """)
  Long countByCustomerUserIdAndProcessingOrdersAndDateBetween(
          @Param("userId") Long userId,
          @Param("status") String status,
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end);

}
