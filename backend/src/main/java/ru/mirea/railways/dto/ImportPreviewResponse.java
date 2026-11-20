package ru.mirea.railways.dto;
import java.util.List;
public record ImportPreviewResponse(int totalRows, int validRows, int invalidRows,
                                    List<String> errors, String token) {}