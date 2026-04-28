package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.FinancialTransactionResponse;
import com.lawfirm.domain.model.FinancialTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FinancialTransactionMapper {

    @Mapping(target = "caseId",     source = "caseEntity.id")
    @Mapping(target = "caseNumber", source = "caseEntity.fullCaseNumber")
    @Mapping(target = "invoiceId",  source = "invoice.id")
    FinancialTransactionResponse toResponse(FinancialTransaction transaction);
}
