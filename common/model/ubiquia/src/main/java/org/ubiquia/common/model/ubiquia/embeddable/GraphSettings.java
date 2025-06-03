package org.ubiquia.common.model.ubiquia.embeddable;


import jakarta.persistence.Embeddable;

@Embeddable
public class GraphSettings {

    private String flag;

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

}
