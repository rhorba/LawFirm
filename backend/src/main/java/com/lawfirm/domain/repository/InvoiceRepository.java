package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    Page<Invoice> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Invoice> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByInvoiceNumber(String invoiceNumber);
}
