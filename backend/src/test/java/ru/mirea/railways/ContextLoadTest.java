package ru.mirea.railways;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.jwt.secret-base64=ZmFrZS1zZWNyZXQtYmFzZTY0LXN0cmluZy1mb3ItcmFpbHdheS1ib29raW5nLXN5c3RlbS0yMDI2",
        "app.bootstrap.email=admin@test.local",
        "app.bootstrap.password=Adm1!ChangeMe2026",
        "app.bootstrap.name=Admin"
})
class ContextLoadTest {
    @Test void contextLoads() {}
}