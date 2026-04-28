package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.InvoiceItemResponse;
import com.lawfirm.application.dto.response.InvoiceResponse;
import com.lawfirm.domain.model.Invoice;
import com.lawfirm.domain.model.InvoiceItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "caseId",     source = "caseEntity.id")
    @Mapping(target = "caseNumber", source = "caseEntity.fullCaseNumber")
    InvoiceResponse toResponse(Invoice invoice);

    InvoiceItemResponse toItemResponse(InvoiceItem item);
}
