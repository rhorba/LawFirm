package com.lawfirm.application.mapper;

import com.lawfirm.application.dto.response.CaseResponse;
import com.lawfirm.application.dto.response.CaseSummary;
import com.lawfirm.application.dto.response.CaseSummaryResponse;
import com.lawfirm.application.dto.response.FinancialSummary;
import com.lawfirm.domain.model.Case;
import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.FinancialTransaction;
import com.lawfirm.domain.model.Lawyer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {
    TribunalMapper.class,
    CaseTypeMapper.class,
    CaseCategoryMapper.class,
    LawyerMapper.class,
    CaseStatusMapper.class
})
public interface CaseMapper {

    @Mapping(target = "financialSummary", expression = "java(calculateFinancialSummary(caseEntity))")
    @Mapping(target = "lawyers", source = "lawyers")
    @Mapping(target = "parentCase", source = "parentCase", qualifiedByName = "toCaseSummaryResponse")
    @Mapping(target = "clientId",   source = "client.id")
    @Mapping(target = "clientName", source = "client", qualifiedByName = "clientFullName")
    CaseResponse toResponse(Case caseEntity);

    @Mapping(target = "tribunalNameFr", source = "tribunal.nameFr")
    @Mapping(target = "caseTypeNameFr", source = "caseType.nameFr")
    @Mapping(target = "lawyerName", source = "lawyers", qualifiedByName = "toLawyerNames")
    @Mapping(target = "statusNameFr", source = "status.nameFr")
    CaseSummary toSummary(Case caseEntity);

    List<CaseResponse> toResponseList(List<Case> cases);

    List<CaseSummary> toSummaryList(List<Case> cases);

    @Named("clientFullName")
    default String clientFullName(Client client) {
        return client != null ? client.getFullName() : null;
    }

    @Named("toCaseSummaryResponse")
    default CaseSummaryResponse toCaseSummaryResponse(Case parentCase) {
        if (parentCase == null) return null;
        return new CaseSummaryResponse(parentCase.getId(), parentCase.getFullCaseNumber());
    }

    @Named("toLawyerNames")
    default String toLawyerNames(Set<Lawyer> lawyers) {
        if (lawyers == null || lawyers.isEmpty()) return null;
        return lawyers.stream()
            .map(l -> l.getFirstName() + " " + l.getLastName())
            .collect(Collectors.joining(", "));
    }

    default FinancialSummary calculateFinancialSummary(Case caseEntity) {
        if (caseEntity.getTransactions() == null || caseEntity.getTransactions().isEmpty()) {
            return new FinancialSummary(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
        }

        BigDecimal totalRevenue = caseEntity.getTransactions().stream()
            .filter(t -> t.getDeletedAt() == null
                      && t.getDirection() == FinancialTransaction.Direction.REVENUE)
            .map(FinancialTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = caseEntity.getTransactions().stream()
            .filter(t -> t.getDeletedAt() == null
                      && t.getDirection() == FinancialTransaction.Direction.EXPENSE)
            .map(FinancialTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal balance = totalRevenue.subtract(totalExpenses);
        int count = (int) caseEntity.getTransactions().stream()
            .filter(t -> t.getDeletedAt() == null).count();

        return new FinancialSummary(totalRevenue, totalExpenses, balance, count);
    }
}
