package ru.mirea.railways.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.Set;
import java.util.UUID;
public record BulkRequest(
        @NotEmpty Set<UUID> ids,
        @Pattern(regexp = "ARCHIVE|RESTORE|STATUS|ADD_TAG|REMOVE_TAG") String action,
        String status,
        String tag
) {}