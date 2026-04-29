package com.bank.repository;

import com.bank.domain.customer.Customer;
import com.bank.domain.customer.CustomerStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void shouldSaveAndFindCustomer() {
        // given
        Customer customer = Customer.builder()
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .passwordHash("hashed_password_123")
                .status(CustomerStatus.ACTIVE)
                .build();

        // when
        Customer savedCustomer = customerRepository.saveAndFlush(customer);
        Optional<Customer> foundCustomer = customerRepository.findById(savedCustomer.getId());

        // then
        assertThat(foundCustomer).isPresent();
        assertThat(foundCustomer.get().getFirstName()).isEqualTo("Jan");
        assertThat(foundCustomer.get().getEmail()).isEqualTo("jan.kowalski@example.com");
        assertThat(foundCustomer.get().getId()).isNotNull();
        assertThat(foundCustomer.get().getCreatedAt()).isNotNull();
    }
}
