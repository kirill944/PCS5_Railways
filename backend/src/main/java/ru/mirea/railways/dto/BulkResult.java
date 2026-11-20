package ru.mirea.railways.dto;

import java.util.List;
import java.util.UUID;

public record BulkResult(
        int successCount,
        int failedCount,
        List<UUID> succeeded,
        List<String> errors
) {}