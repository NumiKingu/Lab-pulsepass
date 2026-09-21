package com.pulsepass.support;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.Venue;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public final class TestData {

    public static final LocalDateTime PURCHASE_DATE = LocalDateTime.of(2026, 9, 1, 10, 0);

    private TestData() {
    }


    public static Venue marinaVenue() {
        return new Venue("VEN-SMR-01", "Marina Convention Center", "Santa Marta",
                "Calle 1 # 1-01, Santa Marta", 5000);
    }

    public static Venue venue(String code, String city) {
        return new Venue(code, "Venue " + code, city, "Direccion " + code, 1000);
    }


    public static Event caribbeanMusicFest(Venue venue) {
        return new Event("CMF-2026", "Caribbean Music Fest 2026", EventCategory.MUSIC,
                EventStatus.PUBLISHED, LocalDateTime.of(2026, 12, 12, 20, 0), venue);
    }

    public static Event event(String code, EventStatus status, LocalDateTime date, Venue venue) {
        return new Event(code, "Evento " + code, EventCategory.MUSIC, status, date, venue);
    }

    public static User user(String username) {
        return new User(username, username + "@pulsepass.test");
    }

    public static Ticket ticket(String code, TicketType type, String price, TicketStatus status,
                                User user, Event event) {
        return new Ticket(code, type, new BigDecimal(price), status, PURCHASE_DATE, user, event);
    }
}
