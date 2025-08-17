package com.selling.repository;

import com.selling.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepo extends JpaRepository<Product, Long> {
    List<Product> findAllByStatus(String status);

    List<Product> findByNameContainingAndStatus(String name, String status);
    List<Product> findByPriceGreaterThanEqualAndStatus(BigDecimal minPrice, String status);
    List<Product> findByPriceLessThanEqualAndStatus(BigDecimal maxPrice, String status);
    List<Product> findByPriceBetweenAndStatus(BigDecimal minPrice, BigDecimal maxPrice, String status);

    List<Product> findByNameContainingAndPriceGreaterThanEqualAndStatus(
            String name, BigDecimal minPrice, String status);
    List<Product> findByNameContainingAndPriceLessThanEqualAndStatus(
            String name, BigDecimal maxPrice, String status);
    List<Product> findByNameContainingAndPriceBetweenAndStatus(
            String name, BigDecimal minPrice, BigDecimal maxPrice, String status);

    Product findByName(String name);

    Product findAllByProductId(Integer productId);
}
