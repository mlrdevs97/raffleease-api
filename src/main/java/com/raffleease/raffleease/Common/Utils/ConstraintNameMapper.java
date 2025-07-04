package com.raffleease.raffleease.Common.Utils;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public final class ConstraintNameMapper {
    private static final Map<String, String> CONSTRAINT_TO_FIELD = Map.ofEntries(
            Map.entry("uk_user_email", "userData.email"),
            Map.entry("uk_user_username", "userData.userName"),
            Map.entry("uk_user_phone", "userData.phoneNumber"),
            Map.entry("uk_association_name", "associationData.associationName"),
            Map.entry("uk_association_email", "associationData.email"),
            Map.entry("uk_association_phone", "associationData.phoneNumber"),
            Map.entry("email", "email")
    );

    public String mapToField(String constraintName) {
        return CONSTRAINT_TO_FIELD.getOrDefault(constraintName, "unknown");
    }
}
