-- Users and roles
CREATE TABLE user_accounts (
                               id UUID PRIMARY KEY,
                               email VARCHAR(160) NOT NULL UNIQUE,
                               full_name VARCHAR(160) NOT NULL,
                               password_hash VARCHAR(100) NOT NULL,
                               role VARCHAR(32) NOT NULL,
                               enabled BOOLEAN NOT NULL DEFAULT TRUE,
                               must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
                               failed_login_attempts INT NOT NULL DEFAULT 0,
                               locked_until TIMESTAMP,
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE refresh_sessions (
                                  id UUID PRIMARY KEY,
                                  user_id UUID NOT NULL REFERENCES user_accounts(id) ON DELETE CASCADE,
                                  token_hash VARCHAR(128) NOT NULL,
                                  expires_at TIMESTAMP NOT NULL,
                                  revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                  created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_user ON refresh_sessions(user_id);

-- Passengers (справочник)
CREATE TABLE passengers (
                            id UUID PRIMARY KEY,
                            full_name VARCHAR(150) NOT NULL,
                            passport_number VARCHAR(20) NOT NULL UNIQUE,
                            email VARCHAR(100) NOT NULL UNIQUE,
                            phone VARCHAR(20) NOT NULL,
                            birth_date DATE NOT NULL,
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL DEFAULT now(),
                            updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- Bookings (основная сущность)
CREATE TABLE bookings (
                          id UUID PRIMARY KEY,
                          external_code VARCHAR(40) NOT NULL UNIQUE,
                          passenger_id UUID NOT NULL REFERENCES passengers(id),
                          train_number VARCHAR(10) NOT NULL,
                          route_from VARCHAR(100) NOT NULL,
                          route_to VARCHAR(100) NOT NULL,
                          departure_date DATE NOT NULL,
                          departure_time TIME NOT NULL,
                          wagon_number INT NOT NULL CHECK (wagon_number >= 1),
                          seat_number INT NOT NULL CHECK (seat_number >= 1),
                          price NUMERIC(10,2) NOT NULL CHECK (price > 0),
                          status VARCHAR(20) NOT NULL,
                          paid BOOLEAN NOT NULL DEFAULT FALSE,
                          archived BOOLEAN NOT NULL DEFAULT FALSE,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_booking_passenger ON bookings(passenger_id);
CREATE INDEX ix_booking_status ON bookings(status);
CREATE INDEX ix_booking_train ON bookings(train_number);
CREATE INDEX ix_booking_archived ON bookings(archived);

-- Notes
CREATE TABLE booking_notes (
                               id UUID PRIMARY KEY,
                               booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                               author_email VARCHAR(160) NOT NULL,
                               body VARCHAR(2000) NOT NULL,
                               pinned BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_note_booking ON booking_notes(booking_id);

-- Tags
CREATE TABLE tags (
                      id UUID PRIMARY KEY,
                      name VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE booking_tag (
                             booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
                             tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                             PRIMARY KEY (booking_id, tag_id)
);

-- Audit
CREATE TABLE audit_events (
                              id UUID PRIMARY KEY,
                              actor_email VARCHAR(160),
                              event_type VARCHAR(60) NOT NULL,
                              entity_type VARCHAR(60) NOT NULL,
                              entity_id UUID,
                              correlation_id VARCHAR(64),
                              description VARCHAR(500) NOT NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_created ON audit_events(created_at DESC);
CREATE INDEX ix_audit_type ON audit_events(event_type);
CREATE INDEX ix_audit_entity ON audit_events(entity_type, entity_id);