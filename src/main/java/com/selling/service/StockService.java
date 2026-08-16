package com.selling.service;


import com.selling.dto.StockDetailsDto;
import com.selling.dto.StockDto;

import java.util.List;
import java.sql.Date;

public interface StockService {
    StockDto saveStock(StockDetailsDto stockDto);

    StockDto getStockById(Long aLong);

    List<StockDto> getAllStock();

    StockDto updateStock(Integer id, StockDto stockDto);

    boolean deleteStock(Integer id);

    List<StockDto> getAllStockByType(String name);

    void updateStockByName(String name, Integer qty);

    List<StockDetailsDto> getStockDetails(String type, String status, Date date, String month);
}
