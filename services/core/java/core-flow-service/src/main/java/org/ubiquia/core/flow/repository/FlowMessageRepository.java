package org.ubiquia.core.flow.repository;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;

/** Spring Data repository for flow message entities. */
public interface FlowMessageRepository extends AbstractEntityRepository<FlowMessageEntity> {

    /** Counts messages queued for the given target node. */
    Long countByTargetNodeId(final String adapterId);

    /** Returns a page of messages for the given target node. */
    Page<FlowMessageEntity> findAllByTargetNodeId(
        Pageable pageable,
        final String targetNodeId);

    /** Returns all messages for the given target node within a specific flow. */
    List<FlowMessageEntity> findAllByTargetNodeIdAndFlowEventFlowId(
        final String targetNodeId,
        final String flowId);

    /** Returns a page of messages whose target node is not in the provided set of node IDs. */
    Page<FlowMessageEntity> findAllByTargetNodeIdNotIn(
        Collection<String> localNodeIds,
        Pageable pageable);

}
