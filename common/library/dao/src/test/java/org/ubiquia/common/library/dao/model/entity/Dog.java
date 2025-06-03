package org.ubiquia.common.library.dao.model.entity;

import jakarta.persistence.Entity;

@Entity
public class Dog extends Animal {

    public Float barkDecibels;

    public Float getBarkDecibels() {
        return barkDecibels;
    }

    public void setBarkDecibels(Float barkDecibels) {
        this.barkDecibels = barkDecibels;
    }
}
