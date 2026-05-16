package com.lawfirm.application.service;

import com.lawfirm.application.dto.request.CreateClientRequest;
import com.lawfirm.application.dto.request.UpdateClientRequest;
import com.lawfirm.application.dto.response.ClientResponse;
import com.lawfirm.application.mapper.ClientMapper;
import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import com.lawfirm.domain.repository.ClientRepository;
import com.lawfirm.presentation.exception.BusinessRuleException;
import com.lawfirm.presentation.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ClientMapper clientMapper;

    @InjectMocks
    private ClientService clientService;

    private Client testClient;
    private ClientResponse testClientResponse;

    @BeforeEach
    void setUp() {
        testClient = new Client();
        testClient.setId(1L);
        testClient.setClientType(ClientType.INDIVIDUAL);
        testClient.setFirstName("Mohammed");
        testClient.setLastName("Benali");
        testClient.setCin("AB123456");
        testClient.setActive(true);

        testClientResponse = mock(ClientResponse.class);
        lenient().when(testClientResponse.id()).thenReturn(1L);
        lenient().when(testClientResponse.clientType()).thenReturn(ClientType.INDIVIDUAL);
        lenient().when(testClientResponse.firstName()).thenReturn("Mohammed");
        lenient().when(testClientResponse.lastName()).thenReturn("Benali");
    }

    @Test
    void findById_ShouldReturnClient_WhenExists() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));
        when(clientMapper.toResponse(testClient)).thenReturn(testClientResponse);

        ClientResponse result = clientService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Mohammed");
    }

    @Test
    void findById_ShouldThrow_WhenNotFound() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Client not found");
    }

    @Test
    void create_ShouldSave_WhenIndividualWithValidNames() {
        CreateClientRequest request = new CreateClientRequest(
            ClientType.INDIVIDUAL, "Mohammed", "Benali", null, null,
            null, null, "AB123456", null,
            LocalDate.of(1990, 1, 1), null, null
        );

        when(clientRepository.findByCin(anyString())).thenReturn(Optional.empty());
        when(clientMapper.toEntity(request)).thenReturn(testClient);
        when(clientRepository.save(testClient)).thenReturn(testClient);
        when(clientMapper.toResponse(testClient)).thenReturn(testClientResponse);

        ClientResponse result = clientService.create(request);

        assertThat(result).isNotNull();
        verify(clientRepository).save(testClient);
    }

    @Test
    void create_ShouldThrow_WhenIndividualMissingFirstName() {
        CreateClientRequest request = new CreateClientRequest(
            ClientType.INDIVIDUAL, null, "Benali", null, null,
            null, null, null, null,
            LocalDate.of(1990, 1, 1), null, null
        );

        assertThatThrownBy(() -> clientService.create(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("first and last name");

        verify(clientRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrow_WhenCorporateMissingCompanyName() {
        CreateClientRequest request = new CreateClientRequest(
            ClientType.CORPORATE, null, null, null, null,
            null, null, null, null,
            null, null, null
        );

        assertThatThrownBy(() -> clientService.create(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("company name");

        verify(clientRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrow_WhenClientUnder18() {
        CreateClientRequest request = new CreateClientRequest(
            ClientType.INDIVIDUAL, "Young", "Client", null, null,
            null, null, null, null,
            LocalDate.now().minusYears(17), null, null
        );

        assertThatThrownBy(() -> clientService.create(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("18 and 100");

        verify(clientRepository, never()).save(any());
    }

    @Test
    void create_ShouldThrow_WhenDuplicateCin() {
        CreateClientRequest request = new CreateClientRequest(
            ClientType.INDIVIDUAL, "Mohammed", "Benali", null, null,
            null, null, "AB123456", null,
            LocalDate.of(1990, 1, 1), null, null
        );

        when(clientRepository.findByCin("AB123456")).thenReturn(Optional.of(new Client()));

        assertThatThrownBy(() -> clientService.create(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("CIN already registered");

        verify(clientRepository, never()).save(any());
    }

    @Test
    void deactivate_ShouldSetActiveFalse() {
        when(clientRepository.findById(1L)).thenReturn(Optional.of(testClient));

        clientService.deactivate(1L);

        assertThat(testClient.getActive()).isFalse();
        verify(clientRepository).save(testClient);
    }

    @Test
    void deactivate_ShouldThrow_WhenNotFound() {
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.deactivate(99L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_ShouldThrow_WhenClientNotFound() {
        UpdateClientRequest request = mock(UpdateClientRequest.class);
        when(clientRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.update(99L, request))
            .isInstanceOf(ResourceNotFoundException.class);
    }
}
