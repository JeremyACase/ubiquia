package org.ubiquia.common.library.dao.model.entity;

import jakarta.persistence.Entity;

@Entity
public class Poodle extends Dog {

    public Boolean apexPredator = false;

    public Boolean getApexPredator() {
        return apexPredator;
    }

    public void setApexPredator(Boolean apexPredator) {
        this.apexPredator = apexPredator;
    }
}
