package com.bank.repository;

import com.bank.domain.card.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {

    @Query("""
            select c from Card c
            join fetch c.account a
            join fetch a.customer owner
            left join fetch a.parentAccount parent
            left join fetch parent.customer parentCustomer
            where owner.id = :customerId
               or parentCustomer.id = :customerId
            """)
    List<Card> findAccessibleByCustomerId(UUID customerId);
}
