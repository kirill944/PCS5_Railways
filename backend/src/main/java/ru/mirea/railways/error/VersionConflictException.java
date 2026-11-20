package ru.mirea.railways.error;
public class VersionConflictException extends RuntimeException {
    public VersionConflictException(String message) { super(message); }
}