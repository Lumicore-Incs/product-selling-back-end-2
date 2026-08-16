package com.selling.repository;

import com.selling.model.StockDetails;
import org.springframework.data.jpa.repository.JpaRepository;


public interface StockDetailsRepo extends JpaRepository<StockDetails, Integer> {

}
