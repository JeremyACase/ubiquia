package org.ubiquia.common.model.ubiquia.dto;

import java.util.List;

public class Network extends AbstractModel {

    private List<Agent> agents;

    @Override
    public String getModelType() {
        return "Network";
    }

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }
}
