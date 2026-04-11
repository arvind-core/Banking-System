package com.BankingSystem.entity.users;

public enum Role {
    // This Enum will hold the roles a user can have, this is not hard coded because we can make mistakes when hardcoding it everytime.
    // If we use Enum to hold roles we just refer to the Enum, We don't actaul hard code the values so we make minimal error.
    CUSTOMER,
    BRANCH_MANAGER,
    ADMIN
}