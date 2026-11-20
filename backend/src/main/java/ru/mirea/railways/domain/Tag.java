package ru.mirea.railways.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "tags")
public class Tag {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    public Tag() {}
    public Tag(String name) { this.name = name; }

    @PrePersist
    void onCreate() { if (id == null) id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}