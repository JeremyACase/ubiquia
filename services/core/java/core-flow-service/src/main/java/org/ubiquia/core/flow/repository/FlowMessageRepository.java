package org.ubiquia.core.flow.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;

public interface FlowMessageRepository extends AbstractEntityRepository<FlowMessageEntity> {

    Long countByTargetNodeId(final String adapterId);

    Page<FlowMessageEntity> findAllByTargetNodeId(
        Pageable pageable,
        final String targetNodeId);

    List<FlowMessageEntity> findAllByTargetNodeIdAndFlowEventFlowId(
        final String targetNodeId,
        final String flowId);

}
