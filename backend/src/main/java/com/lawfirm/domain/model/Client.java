package com.lawfirm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Client extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientType clientType;

    // Common
    @Column(length = 100) private String firstName;
    @Column(length = 100) private String lastName;
    @Column(length = 20)  private String phone;
    @Column(length = 100, unique = true) private String email;
    @Column(columnDefinition = "TEXT") private String address;
    @Column(columnDefinition = "TEXT") private String notes;
    @Column(nullable = false) @Builder.Default private Boolean active = true;

    // INDIVIDUAL only
    @Column(length = 20, unique = true) private String cin;
    @Enumerated(EnumType.STRING)
    @Column(length = 10) private Gender gender;
    @Column private LocalDate dateOfBirth;

    // CORPORATE / GOVERNMENT only
    @Column(length = 200) private String companyName;
    @Column(length = 50, unique = true) private String taxNumber;

    @OneToMany(mappedBy = "client")
    @Builder.Default
    private List<Case> cases = new ArrayList<>();

    public String getFullName() {
        return clientType == ClientType.INDIVIDUAL
            ? firstName + " " + lastName
            : companyName;
    }

    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
