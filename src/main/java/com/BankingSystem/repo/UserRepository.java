package com.BankingSystem.repo;

import com.BankingSystem.entity.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Boolean existsByEmail(String email);
    
    Boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByEmailAndIsActiveTrue(String email);
}