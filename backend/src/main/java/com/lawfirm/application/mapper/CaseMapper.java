package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CaseResponse;
import com.lawfirm.application.dto.response.CaseSummary;
import com.lawfirm.application.dto.response.FinancialSummary;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.FinancialTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring", uses = {
    TribunalMapper.class,
    CaseTypeMapper.class,
    CaseCategoryMapper.class,
    LawyerMapper.class,
    CaseStatusMapper.class
})
public interface CaseMapper {

    @Mapping(target = "financialSummary", expression = "java(calculateFinancialSummary(caseEntity))")
    CaseResponse toResponse(Case caseEntity);

    @Mapping(target = "tribunalNameFr", source = "tribunal.nameFr")
    @Mapping(target = "caseTypeNameFr", source = "caseType.nameFr")
    @Mapping(target = "lawyerName", expression = "java(caseEntity.getLawyer() != null ? caseEntity.getLawyer().getFullName() : null)")
    @Mapping(target = "statusNameFr", source = "status.nameFr")
    CaseSummary toSummary(Case caseEntity);

    List<CaseResponse> toResponseList(List<Case> cases);

    List<CaseSummary> toSummaryList(List<Case> cases);

    default FinancialSummary calculateFinancialSummary(Case caseEntity) {
        if (caseEntity.getTransactions() == null || caseEntity.getTransactions().isEmpty()) {
            return new FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        BigDecimal totalPayments = caseEntity.getTransactions().stream()
            .filter(t -> t.getTransactionType() == FinancialTransaction.TransactionType.PAYMENT)
            .map(FinancialTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = caseEntity.getTransactions().stream()
            .filter(t -> t.getTransactionType() == FinancialTransaction.TransactionType.EXPENSE)
            .map(FinancialTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalPayments.subtract(totalExpenses);
        int count = caseEntity.getTransactions().size();

        return new FinancialSummary(totalPayments, totalExpenses, balance, count);
    }
}
