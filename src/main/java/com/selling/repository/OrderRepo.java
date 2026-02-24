package com.selling.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
          AND o.date >= :start
      """)
  Long countByStatusAndDateBetween(
      @Param("status") String status,
      @Param("start") LocalDateTime startOfMonth);

  @Query("""
          SELECT COALESCE(SUM(od.qty), 0)
          FROM Order o
          JOIN o.orderDetails od
          WHERE o.status = :status
          AND o.date >= :start
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

  @Query(value = """
      SELECT DISTINCT o FROM Order o
      JOIN o.customer c
      WHERE (:status IS NULL OR :status = '' OR :status = 'all' OR o.status = :status)
      AND (:search IS NULL OR :search = '' OR
           LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
           c.contact01 LIKE CONCAT('%', :search, '%') OR
           c.contact02 LIKE CONCAT('%', :search, '%') OR
           LOWER(o.weyBillId) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(o.serialNo) LIKE LOWER(CONCAT('%', :search, '%')))
      ORDER BY o.orderId DESC
      """, countQuery = """
      SELECT COUNT(DISTINCT o) FROM Order o
      JOIN o.customer c
      WHERE (:status IS NULL OR :status = '' OR :status = 'all' OR o.status = :status)
      AND (:search IS NULL OR :search = '' OR
           LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
           c.contact01 LIKE CONCAT('%', :search, '%') OR
           c.contact02 LIKE CONCAT('%', :search, '%') OR
           LOWER(o.weyBillId) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(o.serialNo) LIKE LOWER(CONCAT('%', :search, '%')))
      """)
  Page<Order> findAllWithFilters(
      @Param("status") String status,
      @Param("search") String search,
      Pageable pageable);

  @Query(value = """
      SELECT DISTINCT o FROM Order o
      JOIN o.customer c
      WHERE o.user.id = :userId
      AND (:status IS NULL OR :status = '' OR :status = 'all' OR o.status = :status)
      AND (:search IS NULL OR :search = '' OR
           LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
           c.contact01 LIKE CONCAT('%', :search, '%') OR
           c.contact02 LIKE CONCAT('%', :search, '%') OR
           LOWER(o.weyBillId) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(o.serialNo) LIKE LOWER(CONCAT('%', :search, '%')))
      ORDER BY o.orderId DESC
      """, countQuery = """
      SELECT COUNT(DISTINCT o) FROM Order o
      JOIN o.customer c
      WHERE o.user.id = :userId
      AND (:status IS NULL OR :status = '' OR :status = 'all' OR o.status = :status)
      AND (:search IS NULL OR :search = '' OR
           LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
           c.contact01 LIKE CONCAT('%', :search, '%') OR
           c.contact02 LIKE CONCAT('%', :search, '%') OR
           LOWER(o.weyBillId) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(o.serialNo) LIKE LOWER(CONCAT('%', :search, '%')))
      """)
  Page<Order> findAllWithFiltersByUserId(
      @Param("userId") Long userId,
      @Param("status") String status,
      @Param("search") String search,
      Pageable pageable);

}
