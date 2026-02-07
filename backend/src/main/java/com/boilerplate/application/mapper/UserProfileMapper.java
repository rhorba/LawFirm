package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.request.UpdateProfileRequest;
import com.boilerplate.application.dto.response.UserProfileResponse;
import com.boilerplate.domain.model.UserProfile;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    
    UserProfileResponse toResponse(UserProfile entity);
    
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @org.mapstruct.Mapping(target = "id", ignore = true)
    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    @org.mapstruct.Mapping(target = "updatedAt", ignore = true)
    @org.mapstruct.Mapping(target = "version", ignore = true)
    @org.mapstruct.Mapping(target = "user", ignore = true)
    void updateEntityFromRequest(UpdateProfileRequest request, @MappingTarget UserProfile entity);
}
