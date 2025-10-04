package com.selling.util;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.selling.dto.get.UserDtoForGet;
import com.selling.model.User;

@Configuration
public class ModelMapperConfig {
  @Bean
  public ModelMapper modelMapper() {
    ModelMapper modelMapper = new ModelMapper();
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    // Use strict matching for predictable field mappings
    modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    // Avoid mapping JPA lazy collections by default. Only map direct scalar fields
    // and explicitly configured nested types to prevent LazyInitializationException
    modelMapper.getConfiguration().setPropertyCondition(context -> {
      Object source = context.getSource();
      if (source == null)
        return false;
      // skip mapping collections to avoid fetching lazy collections
      return !(source instanceof java.util.Collection);
    });
    // Ensure nested Product -> ProductDto mapping is applied when mapping User ->
    // UserDtoForGet
    TypeMap<User, UserDtoForGet> userMap = modelMapper.createTypeMap(User.class, UserDtoForGet.class);
    userMap.addMappings(map -> map.map(src -> src.getProduct(), UserDtoForGet::setProductId));
    return modelMapper;
  }
}
