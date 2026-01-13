package org.ubiquia.common.model.ubiquia.dto;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Set;
import org.springframework.validation.annotation.Validated;
import org.ubiquia.common.model.ubiquia.entity.AbstractModelEntity;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;

@Validated
public class Flow extends AbstractModel {

    private Graph graph;

    private List<FlowEvent> flowEvents;

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public List<FlowEvent> getFlowEvents() {
        return flowEvents;
    }

    public void setFlowEvents(List<FlowEvent> flowEvents) {
        this.flowEvents = flowEvents;
    }
}
