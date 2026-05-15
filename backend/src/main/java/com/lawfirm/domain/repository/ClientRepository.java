package com.lawfirm.domain.repository;

import com.lawfirm.domain.model.Client;
import com.lawfirm.domain.model.ClientType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    Optional<Client> findByCin(String cin);
    Optional<Client> findByTaxNumber(String taxNumber);
    Optional<Client> findByEmail(String email);
    boolean existsByCin(String cin);
    boolean existsByTaxNumber(String taxNumber);
    boolean existsByEmail(String email);

    @Query("SELECT c FROM Client c WHERE " +
           "(:search IS NULL OR " +
           "LOWER(CONCAT(COALESCE(c.firstName,''), ' ', COALESCE(c.lastName,''))) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.cin)         LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.taxNumber)   LIKE LOWER(CONCAT('%',:search,'%')) OR " +
           "LOWER(c.email)       LIKE LOWER(CONCAT('%',:search,'%'))) AND " +
           "(:type IS NULL OR c.clientType = :type)")
    Page<Client> search(
        @Param("search") String search,
        @Param("type") ClientType type,
        Pageable pageable
    );

    @Query("SELECT c FROM Client c WHERE " +
           "LOWER(CONCAT(COALESCE(c.firstName,''), ' ', COALESCE(c.lastName,''))) LIKE LOWER(CONCAT('%',:name,'%')) OR " +
           "LOWER(c.companyName) LIKE LOWER(CONCAT('%',:name,'%'))")
    java.util.List<Client> searchByName(@Param("name") String name);
}
