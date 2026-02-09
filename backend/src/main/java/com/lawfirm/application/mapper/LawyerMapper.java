package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.request.CreateLawyerRequest;
import com.lawfirm.application.dto.request.UpdateLawyerRequest;
import com.lawfirm.application.dto.response.LawyerResponse;
import com.lawfirm.domain.model.Lawyer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LawyerMapper {

    @Mapping(target = "fullName", expression = "java(lawyer.getFullName())")
    LawyerResponse toResponse(Lawyer lawyer);

    List<LawyerResponse> toResponseList(List<Lawyer> lawyers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "cases", ignore = true)
    Lawyer toEntity(CreateLawyerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "cases", ignore = true)
    void updateEntity(UpdateLawyerRequest request, @MappingTarget Lawyer lawyer);
}
