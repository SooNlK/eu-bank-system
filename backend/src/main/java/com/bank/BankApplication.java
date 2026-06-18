package com.bank;

import com.bank.config.EuPaymentsProperties;
import com.bank.config.SwiftProperties;
import com.bank.config.KlikProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({EuPaymentsProperties.class, SwiftProperties.class, KlikProperties.class})
public class BankApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankApplication.class, args);
    }

}
