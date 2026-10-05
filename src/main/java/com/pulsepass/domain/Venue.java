package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "venues")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "venue")
    private Set<Event> events = new LinkedHashSet<>();

    protected Venue() {

    }

    public Venue(String code, String name, String city, String address, int capacity) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.address = address;
        this.capacity = capacity;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Set<Event> getEvents() { return events; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Venue other)) return false;
        return code != null && Objects.equals(code, other.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }

    @Override
    public String toString() {
        return "Venue{id=" + id + ", code='" + code + "', city='" + city + "'}";
    }
}
