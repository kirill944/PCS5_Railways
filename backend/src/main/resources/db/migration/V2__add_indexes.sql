-- Additional indexes for filtering/search
CREATE INDEX ix_booking_departure ON bookings(departure_date);
CREATE INDEX ix_booking_route ON bookings(route_from, route_to);
CREATE INDEX ix_booking_paid ON bookings(paid);
CREATE INDEX ix_passenger_active ON passengers(active);
CREATE INDEX ix_tag_name_lower ON tags(lower(name));