package org.ubiquia.common.library.dao.model.entity;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.ubiquia.common.library.dao.model.enums.Color;
import org.ubiquia.common.model.ubiquia.embeddable.KeyValuePair;

@Entity
public class Animal {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private String id = null;

    private Color color;

    @CreationTimestamp
    @Column(updatable = false)
    private OffsetDateTime createdAt = null;

    @UpdateTimestamp
    private OffsetDateTime updatedAt = null;

    private String name;

    private Boolean vaccinated;

    private Float weight;

    private Double height;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private Person owner;

    @ElementCollection
    @Valid
    private List<KeyValuePair> listTags;

    @ElementCollection
    @Valid
    private Set<KeyValuePair> setTags;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public List<KeyValuePair> getListTags() {
        return listTags;
    }

    public void setListTags(List<KeyValuePair> listTags) {
        this.listTags = listTags;
    }

    public Set<KeyValuePair> getSetTags() {
        return setTags;
    }

    public void setSetTags(Set<KeyValuePair> setTags) {
        this.setTags = setTags;
    }

    public Boolean getVaccinated() {
        return vaccinated;
    }

    public void setVaccinated(Boolean vaccinated) {
        this.vaccinated = vaccinated;
    }

    public Person getOwner() {
        return owner;
    }

    public void setOwner(Person owner) {
        this.owner = owner;
    }
}
