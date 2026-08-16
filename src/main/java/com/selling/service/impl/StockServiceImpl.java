package com.selling.service.impl;

import com.selling.dto.StockDetailsDto;
import com.selling.dto.StockDto;
import com.selling.model.Stock;
import com.selling.model.StockDetails;
import com.selling.repository.StockRepo;
import com.selling.repository.StockDetailsRepo;
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
    private final StockDetailsRepo stockDetailsRepo;
    private final ModelMapper modelMapper;

    @Override
    public StockDto saveStock(StockDetailsDto stockDetails) {
        Stock stock = stockRepo.findTopByType(stockDetails.getType());

        if (stock == null) {
            System.out.println("1");
            stock = new Stock();
            stock.setType(stockDetails.getType());
            stock.setTotalQuantity(stockDetails.getQuantity());
            stock.setStatus(stockDetails.getStatus());
            stock = stockRepo.save(stock);
        } else {
            System.out.println("2");
            stock.setTotalQuantity(stock.getTotalQuantity() + stockDetails.getQuantity());
            stock = stockRepo.save(stock);
        }

        StockDetails newDetail = new StockDetails();
        newDetail.setQty(stockDetails.getQuantity());
        newDetail.setStatus(stockDetails.getStatus());
        newDetail.setType(stockDetails.getType());
        newDetail.setDate(stockDetails.getDate());
        newDetail.setStock(stock);
        stockDetailsRepo.save(newDetail);

        return entityToDto(stock);
    }

    @Override
    public StockDto getStockById(Long aLong) {
        Integer id = Math.toIntExact(aLong);
        return entityToDto(stockRepo.findById(id).get());
    }

    @Override
    public List<StockDto> getAllStock() {
        List<Stock> all = stockRepo.findAllByOrderByIdDesc();
        List<StockDto> stockDtos = new ArrayList<>();
        for (Stock stock : all) {
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
            stock1.setStock_id(stock.getStock_id());
            stockRepo.save(stock1);
            return entityToDto(stock1);
        }
        return null;
    }

    @Override
    public boolean deleteStock(Integer id) {
        Optional<Stock> byId = stockRepo.findById(id);
        if (byId.isPresent()) {
            stockRepo.deleteById(id);
            return true;
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

            int currentQty = stock.getTotalQuantity();

            if (currentQty >= remainingQty) {
                stock.setTotalQuantity(currentQty - remainingQty);
                remainingQty = 0;
            } else {
                remainingQty -= currentQty;
                stock.setTotalQuantity(0);
            }

            stockRepo.save(stock);
        }

        // Stock මදි නම් අන්තිම stock එක negative කරන්න
        if (remainingQty > 0 && lastUpdatedStock != null) {
            lastUpdatedStock.setTotalQuantity(lastUpdatedStock.getTotalQuantity() - remainingQty);
            stockRepo.save(lastUpdatedStock);
        }
    }
    public Stock dtoToEntity(StockDto stockDto) {
        return modelMapper.map(stockDto, Stock.class);
    }

    public Stock stockDetailsDtoToEntity(StockDetails stockDto) {
        return modelMapper.map(stockDto, Stock.class);
    }

    public StockDto entityToDto(Stock stock) {
        return modelMapper.map(stock, StockDto.class);
    }
}
