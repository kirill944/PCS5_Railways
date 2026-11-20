package ru.mirea.railways.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.railways.domain.Passenger;
import ru.mirea.railways.dto.PassengerDto;
import ru.mirea.railways.dto.PassengerRequest;
import ru.mirea.railways.error.BusinessException;
import ru.mirea.railways.error.NotFoundException;
import ru.mirea.railways.repository.PassengerRepository;

import java.util.List;
import java.util.UUID;

@Service
public class PassengerService {
    private final PassengerRepository repo;
    public PassengerService(PassengerRepository repo) { this.repo = repo; }

    public List<PassengerDto> list() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional
    public PassengerDto create(PassengerRequest r) {
        if (repo.existsByPassportNumber(r.passportNumber()))
            throw new BusinessException("Пассажир с таким паспортом уже существует");
        if (repo.existsByEmail(r.email()))
            throw new BusinessException("Пассажир с таким email уже существует");
        Passenger p = new Passenger();
        apply(p, r);
        return toDto(repo.save(p));
    }

    @Transactional
    public PassengerDto update(UUID id, PassengerRequest r) {
        Passenger p = repo.findById(id).orElseThrow(() -> new NotFoundException("Пассажир не найден"));
        apply(p, r);
        return toDto(repo.save(p));
    }

    @Transactional
    public void toggle(UUID id, boolean active) {
        Passenger p = repo.findById(id).orElseThrow(() -> new NotFoundException("Пассажир не найден"));
        p.setActive(active);
        repo.save(p);
    }

    public Passenger require(UUID id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Пассажир не найден"));
    }

    private void apply(Passenger p, PassengerRequest r) {
        p.setFullName(r.fullName());
        p.setPassportNumber(r.passportNumber());
        p.setEmail(r.email());
        p.setPhone(r.phone());
        p.setBirthDate(r.birthDate());
    }

    public PassengerDto toDto(Passenger p) {
        return new PassengerDto(p.getId(), p.getFullName(), p.getPassportNumber(),
                p.getEmail(), p.getPhone(), p.getBirthDate(), p.isActive());
    }
}