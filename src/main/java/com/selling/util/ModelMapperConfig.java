package com.selling.util;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        // Avoid mapping JPA lazy collections by default. Only map direct scalar fields
        // and explicitly configured nested types to prevent LazyInitializationException
        modelMapper.getConfiguration().setPropertyCondition(context -> {
            Object source = context.getSource();
            if (source == null) return false;
            // skip mapping collections to avoid fetching lazy collections
            return !(source instanceof java.util.Collection);
        });
        return modelMapper;
    }
}
