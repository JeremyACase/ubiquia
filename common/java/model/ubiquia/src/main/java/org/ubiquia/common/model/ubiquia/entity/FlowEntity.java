package org.ubiquia.common.model.ubiquia.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.validation.annotation.Validated;

@Validated
@Entity
public class FlowEntity extends AbstractModelEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "graph_flow_join_id", nullable = false)
    private GraphEntity graph;

    @OneToMany(mappedBy = "flow", fetch = FetchType.LAZY, cascade = CascadeType.REFRESH)
    private Set<FlowEventEntity> flowEvents;

    public GraphEntity getGraph() {
        return graph;
    }

    public void setGraph(GraphEntity graph) {
        this.graph = graph;
    }

    public Set<FlowEventEntity> getFlowEvents() {
        return flowEvents;
    }

    public void setFlowEvents(Set<FlowEventEntity> flowEvents) {
        this.flowEvents = flowEvents;
    }
}
