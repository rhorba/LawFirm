package com.lawfirm.application.service;

import com.lawfirm.domain.model.CaseSequence;
import com.lawfirm.domain.repository.CaseSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaseSequenceService {

    private final CaseSequenceRepository caseSequenceRepository;

    @Transactional
    public synchronized int getNextSequence(int year, String caseTypeCode) {
        CaseSequence sequence = caseSequenceRepository
            .findByYearAndCaseTypeCodeForUpdate(year, caseTypeCode)
            .orElseGet(() -> {
                CaseSequence newSeq = CaseSequence.builder()
                    .year(year)
                    .caseTypeCode(caseTypeCode)
                    .lastSequence(0)
                    .build();
                return caseSequenceRepository.save(newSeq);
            });

        int nextSequence = sequence.getLastSequence() + 1;
        sequence.setLastSequence(nextSequence);
        caseSequenceRepository.save(sequence);

        return nextSequence;
    }
}
