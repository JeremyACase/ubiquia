package org.ubiquia.common.library.dao.model.entity;

import jakarta.persistence.Entity;

@Entity
public class Cat extends Animal {

    public Integer whiskersCount;

    public Integer getWhiskersCount() {
        return whiskersCount;
    }

    public void setWhiskersCount(Integer whiskersCount) {
        this.whiskersCount = whiskersCount;
    }
}