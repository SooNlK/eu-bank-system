package com.bank.security;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    // W przyszłości należy wstrzyknąć tu repozytorium użytkowników, np. UserRepository
    // @Autowired
    // private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Przykładowa implementacja. Zastąpić przez pobieranie użytkownika z bazy danych.
        // UserEntity user = userRepository.findByUsername(username)
        //         .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        if ("admin".equals(username)) {
            return new User("admin", "{noop}admin", new ArrayList<>());
        } else {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}
