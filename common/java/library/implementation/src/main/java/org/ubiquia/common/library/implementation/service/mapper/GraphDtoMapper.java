package org.ubiquia.common.library.implementation.service.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.ubiquia.common.model.ubiquia.dto.Graph;
import org.ubiquia.common.model.ubiquia.dto.GraphEdge;
import org.ubiquia.common.model.ubiquia.entity.GraphEntity;

@Service
@Transactional
public class GraphDtoMapper extends GenericDtoMapper<GraphEntity, Graph> {

    @Autowired
    private NodeDtoMapper nodeDtoMapper;

    @Autowired
    private ComponentDtoMapper componentDtoMapper;

    @Override
    public Graph map(final GraphEntity from) throws JsonProcessingException {

        Graph to = null;
        if (Objects.nonNull(from)) {
            to = new Graph();
            super.setAbstractEntityFields(from, to);

            to.setAuthor(from.getAuthor());
            to.setCapabilities(from.getCapabilities());
            to.setDescription(from.getDescription());
            to.setName(from.getName());
            to.setComponents(this.componentDtoMapper.map(from.getComponents()));
            to.setNodes(this.nodeDtoMapper.map(from.getNodes()));
            to.setEdges(this.getEdges(from));
        }

        return to;
    }

    private List<GraphEdge> getEdges(final GraphEntity from) {
        var edges = new ArrayList<GraphEdge>();

        for (var node : from.getNodes()) {
            if (Objects.nonNull(node.getDownstreamNodes())
                && !node.getDownstreamNodes().isEmpty()) {

                var edge = new GraphEdge();
                edge.setLeftNodeName(node.getName());
                edge.setRightNodeNames(new ArrayList<>());

                for (var downstreamNode : node.getDownstreamNodes()) {
                    edge.getRightNodeNames().add(downstreamNode.getName());
                }

                edges.add(edge);
            }
        }

        return edges;
    }
}
