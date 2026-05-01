package org.ubiquia.common.model.ubiquia.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.util.List;

@Entity
public class NetworkEntity extends AbstractModelEntity {

    @OneToMany(mappedBy = "network", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private List<AgentEntity> agents;

    @Override
    public String getModelType() {
        return "Network";
    }

    public List<AgentEntity> getAgents() {
        return agents;
    }

    public void setAgents(List<AgentEntity> agents) {
        this.agents = agents;
    }
}
