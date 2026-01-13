package org.ubiquia.core.flow.service.builder;


import jakarta.transaction.Transactional;
import java.util.HashSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.entity.FlowEntity;
import org.ubiquia.core.flow.component.node.AbstractNode;
import org.ubiquia.core.flow.repository.FlowRepository;
import org.ubiquia.core.flow.repository.GraphRepository;

@Service
public class FlowBuilder {

    @Autowired
    private FlowRepository flowRepository;

    @Autowired
    private GraphRepository graphRepository;

    @Transactional
    public FlowEntity makeFlowFrom(final AbstractNode node) {

        var flow = new FlowEntity();

        var context = node.getNodeContext();
        var graphRecord = this
            .graphRepository
            .findById(context.getGraph().getId());

        if (graphRecord.isEmpty()) {
            throw new IllegalArgumentException("ERROR: Could not find graph with id: "
                + context.getGraph().getId());
        }

        var graphEntity = graphRecord.get();
        flow.setGraph(graphEntity);
        graphEntity.getFlows().add(flow);
        this.graphRepository.save(graphEntity);

        flow.setFlowEvents(new HashSet<>());
        flow = this.flowRepository.save(flow);

        return flow;
    }
}
