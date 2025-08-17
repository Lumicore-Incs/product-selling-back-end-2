package com.selling.service.impl;

import com.selling.dto.ProductDto;
import com.selling.model.Product;
import com.selling.repository.ProductRepo;
import com.selling.service.ProductService;
import com.selling.util.ModelMapperConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepo productRepository;
    private final ModelMapperConfig modelMapperConfig;


    @Override
    public ProductDto saveProduct(ProductDto productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setPrice(productDTO.getPrice());
        product.setStatus("active"); // Default status

        productRepository.save(product);
        return entityToDTO(product);
    }

    @Override
    public ProductDto getProductById(Long id) {
        Optional<Product> product = productRepository.findById(id);
        return product.map(this::entityToDTO).orElse(null);
    }

    @Override
    public List<ProductDto> getAllProducts() {
        List<Product> products = productRepository.findAll();
        return products.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDto> getAllProductsUserWise() {
        List<Product> products = productRepository.findAllByStatus("active");
        return products.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ProductDto updateProduct(Integer id, ProductDto productDTO) {
        Optional<Product> productOptional = productRepository.findById(Long.valueOf(id));
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            product.setName(productDTO.getName());
            product.setPrice(productDTO.getPrice());
            product.setStatus(productDTO.getStatus());

            Product updatedProduct = productRepository.save(product);
            return entityToDTO(updatedProduct);
        }
        return null;
    }

    @Override
    public boolean deleteProduct(Integer id) {
        Optional<Product> productOptional = productRepository.findById(Long.valueOf(id));
        if (productOptional.isPresent()) {
            Product product = productOptional.get();
            product.setStatus("remove");
            productRepository.save(product);
            return true;
        }
        return false;
    }

    @Override
    public List<ProductDto> searchProducts(String name, BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products;

        if (name != null && minPrice != null && maxPrice != null) {
            products = productRepository.findByNameContainingAndPriceBetweenAndStatus(
                    name, minPrice, maxPrice, "active");
        } else if (name != null && minPrice != null) {
            products = productRepository.findByNameContainingAndPriceGreaterThanEqualAndStatus(
                    name, minPrice, "active");
        } else if (name != null && maxPrice != null) {
            products = productRepository.findByNameContainingAndPriceLessThanEqualAndStatus(
                    name, maxPrice, "active");
        } else if (minPrice != null && maxPrice != null) {
            products = productRepository.findByPriceBetweenAndStatus(minPrice, maxPrice, "active");
        } else if (name != null) {
            products = productRepository.findByNameContainingAndStatus(name, "active");
        } else if (minPrice != null) {
            products = productRepository.findByPriceGreaterThanEqualAndStatus(minPrice, "active");
        } else if (maxPrice != null) {
            products = productRepository.findByPriceLessThanEqualAndStatus(maxPrice, "active");
        } else {
            products = productRepository.findAllByStatus("active");
        }

        return products.stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }


    private ProductDto entityToDTO(Product product) {
        return (product == null) ? null : modelMapperConfig.modelMapper().map(product, ProductDto.class);
    }
}
