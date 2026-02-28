package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.dto.response.ClientSummary;
import com.lawfirm.domain.model.Client;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    @Mapping(target = "fullName",  expression = "java(client.getFullName())")
    @Mapping(target = "age",       expression = "java(client.getAge())")
    @Mapping(target = "caseCount", expression = "java(client.getCases().size())")
    ClientResponse toResponse(Client client);

    @Mapping(target = "fullName",  expression = "java(client.getFullName())")
    @Mapping(target = "caseCount", expression = "java(client.getCases().size())")
    ClientSummary toSummary(Client client);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version",   ignore = true)
    @Mapping(target = "active",    constant = "true")
    @Mapping(target = "cases",     ignore = true)
    Client toEntity(CreateClientRequest request);

    @Mapping(target = "id",         ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    @Mapping(target = "version",    ignore = true)
    @Mapping(target = "active",     ignore = true)
    @Mapping(target = "clientType", ignore = true)
    @Mapping(target = "cases",      ignore = true)
    void updateEntity(UpdateClientRequest request, @MappingTarget Client client);
}
