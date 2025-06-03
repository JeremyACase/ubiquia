package org.ubiquia.common.library.dao.model.entity;

import jakarta.persistence.Entity;

@Entity
public class Dachschund extends Dog {

    public Boolean apexPredator = true;

    public Boolean landShark = true;

    public Boolean getApexPredator() {
        return apexPredator;
    }

    public void setApexPredator(Boolean apexPredator) {
        this.apexPredator = apexPredator;
    }

    public Boolean getLandShark() {
        return landShark;
    }

    public void setLandShark(Boolean landShark) {
        this.landShark = landShark;
    }
}
