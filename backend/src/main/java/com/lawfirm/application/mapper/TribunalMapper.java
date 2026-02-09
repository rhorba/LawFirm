package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.TribunalResponse;
import com.lawfirm.domain.model.Tribunal;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TribunalMapper {

    TribunalResponse toResponse(Tribunal tribunal);

    List<TribunalResponse> toResponseList(List<Tribunal> tribunals);
}
