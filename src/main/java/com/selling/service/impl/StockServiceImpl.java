package com.selling.service.impl;

import com.selling.dto.StockDto;
import com.selling.model.OrderDetails;
import com.selling.model.Stock;
import com.selling.repository.StockRepo;
import com.selling.service.StockService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepo stockRepo;
    private final ModelMapper modelMapper;

    @Override
    public StockDto saveStock(StockDto stockDto) {

        Stock stock = dtoToEntity(stockDto);

        // Damage stock නම් direct save
        if (stock.getStatus() != null &&
                stock.getStatus().equalsIgnoreCase("DAMAGE")) {

            stockRepo.save(stock);
            return entityToDto(stock);
        }

        // Same product stocks ගන්න
        List<Stock> existingStocks = stockRepo.findAllByType(stock.getType());

        int remainingQty = stock.getQuantity();

        // Existing stock වලින් quantity adjust කරන්න
        for (Stock existingStock : existingStocks) {

            if (existingStock.getStatus() != null &&
                    existingStock.getStatus().equalsIgnoreCase("DAMAGE")) {
                continue;
            }

            int currentQty = existingStock.getQuantity();

            if (currentQty + remainingQty >= 0) {
                existingStock.setQuantity(currentQty + remainingQty);
                remainingQty = 0;
                stockRepo.save(existingStock);
                break;
            } else {
                remainingQty += currentQty;
                existingStock.setQuantity(0);
                stockRepo.save(existingStock);
            }
        }

        // Balance quantity එක අලුත් stock record එකට save කරන්න
        stock.setQuantity(remainingQty);

        stockRepo.save(stock);

        return entityToDto(stock);
    }

    @Override
    public StockDto getStockById(Long aLong) {
        Integer id = Math.toIntExact(aLong);
        return entityToDto(stockRepo.findById(id).get());
    }

    @Override
    public List<StockDto> getAllStock() {
        List<Stock> all = stockRepo.findAllByOrderByDateDesc();
        List<StockDto> stockDtos = new ArrayList<>();
        for (Stock stock : all) {
            entityToDto(stock);
            stockDtos.add(entityToDto(stock));
        }
        return stockDtos;
    }

    @Override
    public StockDto updateStock(Integer id, StockDto stockDto) {
        Optional<Stock> byId = stockRepo.findById(id);
        Stock stock1 = dtoToEntity(stockDto);
        if (byId.isPresent()) {
            Stock stock = byId.get();
            if (stock.getQuantity()==stock.getTotalQuantity()) {
                stock1.setStock_id(stock.getStock_id());
                stockRepo.save(stock1);
                return entityToDto(stock1);
            }
            return null;
        }
        return null;
    }

    @Override
    public boolean deleteStock(Integer id) {
        Optional<Stock> byId = stockRepo.findById(id);
        if (byId.isPresent()) {
            if (byId.get().getQuantity()==byId.get().getTotalQuantity()) {
                stockRepo.deleteById(id);
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public List<StockDto> getAllStockByType(String name) {
        List<Stock> all = stockRepo.findAllByType(name);
        List<StockDto> stockDtos = new ArrayList<>();
        for (Stock stock : all) {
            entityToDto(stock);
            stockDtos.add(entityToDto(stock));
        }
        return stockDtos;
    }

    @Override
    public void updateStockByName(String name, Integer qty) {

        List<Stock> allByType = stockRepo.findAllByType(name);

        int remainingQty = qty;
        Stock lastUpdatedStock = null;

        for (Stock stock : allByType) {

            // DAMAGE stock skip කරන්න
            if (stock.getStatus() != null &&
                    stock.getStatus().equalsIgnoreCase("DAMAGE")) {
                continue;
            }

            lastUpdatedStock = stock;

            if (remainingQty <= 0) {
                break;
            }

            int currentQty = stock.getQuantity();

            if (currentQty >= remainingQty) {
                stock.setQuantity(currentQty - remainingQty);
                remainingQty = 0;
            } else {
                remainingQty -= currentQty;
                stock.setQuantity(0);
            }

            stockRepo.save(stock);
        }

        // Stock මදි නම් අන්තිම stock එක negative කරන්න
        if (remainingQty > 0 && lastUpdatedStock != null) {
            lastUpdatedStock.setQuantity(lastUpdatedStock.getQuantity() - remainingQty);
            stockRepo.save(lastUpdatedStock);
        }
    }
    public Stock dtoToEntity(StockDto stockDto) {
        return modelMapper.map(stockDto, Stock.class);
    }

    public StockDto entityToDto(Stock stock) {
        return modelMapper.map(stock, StockDto.class);
    }
}
