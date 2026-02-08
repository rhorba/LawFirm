package com.boilerplate.application.mapper;

import com.boilerplate.application.dto.response.TribunalResponse;
import com.boilerplate.domain.model.Tribunal;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TribunalMapper {

    TribunalResponse toResponse(Tribunal tribunal);

    List<TribunalResponse> toResponseList(List<Tribunal> tribunals);
}
