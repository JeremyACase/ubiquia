package org.ubiquia.common.model.ubiquia.dto;


import java.util.List;
import org.springframework.validation.annotation.Validated;

@Validated
public class Flow extends AbstractModel {

    private Graph graph;

    private List<FlowEvent> flowEvents;

    @Override
    public String getModelType() {
        return "Flow";
    }


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
