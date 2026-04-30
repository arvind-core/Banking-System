package com.BankingSystem.entity.card;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "billing_cycles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingCycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "credit_card_id",nullable = false)
    private CreditCard creditCard;

    @Column(nullable = false)
    private LocalDate cycleStartDate;

    @Column(nullable = false)
    private LocalDate cycleEndDate;

    @Column(nullable = false)
    private LocalDate paymentDueDate;

    @Column(nullable = false)
    private BigDecimal totalSpend;

    @Column(nullable = false)
    private BigDecimal minimumDue;

    @Column(nullable = false)
    private BigDecimal openingOutstanding;

    @Column(nullable = false)
    private BigDecimal closingOutstanding;

    @Column(nullable = false)
    private BigDecimal totalPaid;

    @Column(nullable = false)
    private BigDecimal interestCharged;

    @Column(nullable = false)
    private BigDecimal totalRewardPointsEarned;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void  onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void  onUpdate(){
        updatedAt = LocalDateTime.now();
    }


}

















