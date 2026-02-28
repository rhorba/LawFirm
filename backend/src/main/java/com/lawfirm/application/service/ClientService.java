package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.dto.response.ClientSummary;
import com.lawfirm.application.mapper.ClientMapper;
import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.presentation.exception.BusinessRuleException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

import static com.lawfirm.domain.model.ClientType.INDIVIDUAL;
import static com.lawfirm.domain.model.ClientType.CORPORATE;
import static com.lawfirm.domain.model.ClientType.GOVERNMENT;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientMapper clientMapper;

    // ── Read ────────────────────────────────────────────────────────────────

    public ClientResponse findById(Long id) {
        return clientMapper.toResponse(getOrThrow(id));
    }

    public Page<ClientSummary> search(String search, ClientType type, int page, int size) {
        String term = (search != null && search.isBlank()) ? null : search;
        return clientRepository.search(term, type, PageRequest.of(page, size))
            .map(clientMapper::toSummary);
    }

    // ── Write ───────────────────────────────────────────────────────────────

    @Transactional
    public ClientResponse create(CreateClientRequest request) {
        validateByType(request);
        validateAge(request.clientType(), request.dateOfBirth());
        checkUniqueness(request.cin(), request.taxNumber(), request.email(), null);
        Client saved = clientRepository.save(clientMapper.toEntity(request));
        return clientMapper.toResponse(saved);
    }

    @Transactional
    public ClientResponse update(Long id, UpdateClientRequest request) {
        Client client = getOrThrow(id);
        checkUniquenessOnUpdate(request, client);
        validateAge(client.getClientType(), request.dateOfBirth());
        clientMapper.updateEntity(request, client);
        return clientMapper.toResponse(clientRepository.save(client));
    }

    @Transactional
    public void deactivate(Long id) {
        Client client = getOrThrow(id);
        client.setActive(false);
        clientRepository.save(client);
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    private Client getOrThrow(Long id) {
        return clientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
    }

    private void validateByType(CreateClientRequest r) {
        if (r.clientType() == INDIVIDUAL
                && (r.firstName() == null || r.firstName().isBlank()
                    || r.lastName() == null || r.lastName().isBlank())) {
            throw new BusinessRuleException("Individual clients require first and last name");
        }
        if ((r.clientType() == CORPORATE || r.clientType() == GOVERNMENT)
                && (r.companyName() == null || r.companyName().isBlank())) {
            throw new BusinessRuleException("Corporate/Government clients require a company name");
        }
    }

    private void validateAge(ClientType type, LocalDate dob) {
        if (type == INDIVIDUAL && dob != null) {
            int age = Period.between(dob, LocalDate.now()).getYears();
            if (age < 18 || age > 100) {
                throw new BusinessRuleException("Client must be between 18 and 100 years old");
            }
        }
    }

    private void checkUniqueness(String cin, String taxNumber, String email, Long excludeId) {
        if (cin != null && !cin.isBlank()) {
            clientRepository.findByCin(cin).ifPresent(existing -> {
                if (excludeId == null || !existing.getId().equals(excludeId))
                    throw new BusinessRuleException("CIN already registered: " + cin);
            });
        }
        if (taxNumber != null && !taxNumber.isBlank()) {
            clientRepository.findByTaxNumber(taxNumber).ifPresent(existing -> {
                if (excludeId == null || !existing.getId().equals(excludeId))
                    throw new BusinessRuleException("Tax number already registered: " + taxNumber);
            });
        }
        if (email != null && !email.isBlank()) {
            clientRepository.findByEmail(email).ifPresent(existing -> {
                if (excludeId == null || !existing.getId().equals(excludeId))
                    throw new BusinessRuleException("Email already registered: " + email);
            });
        }
    }

    private void checkUniquenessOnUpdate(UpdateClientRequest r, Client client) {
        checkUniqueness(r.cin(), r.taxNumber(), r.email(), client.getId());
    }
}
