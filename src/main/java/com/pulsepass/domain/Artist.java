package com.pulsepass.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Table(name = "artists")
public class Artist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, unique = true, length = 150)
    private String stageName;

    @Column(length = 100)
    private String country;

    @Column(length = 100)
    private String genre;

    @Column(nullable = false)
    private boolean active = true;


    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new LinkedHashSet<>();

    protected Artist() {
        // requerido por JPA
    }

    public Artist(String stageName, String country, String genre) {
        this.stageName = stageName;
        this.country = country;
        this.genre = genre;
    }

    public Long getId() { return id; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Boolean getActive() {return active;}
    public Set<Event> getEvents() { return events; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Artist other)) return false;
        return stageName != null && Objects.equals(stageName, other.stageName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(stageName);
    }

    @Override
    public String toString() {
        return "Artist{id=" + id + ", stageName='" + stageName + "'}";
    }

}
