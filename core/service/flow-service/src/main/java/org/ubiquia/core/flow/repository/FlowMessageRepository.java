package org.ubiquia.core.flow.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ubiquia.common.models.entity.FlowMessage;

public interface FlowMessageRepository extends AbstractEntityRepository<FlowMessage> {

    Long countByTargetAdapterId(final String adapterId);

    Page<FlowMessage> findAllByTargetAdapterId(Pageable pageable, final String targetAdapterId);

    List<FlowMessage> findAllByTargetAdapterIdAndFlowEventBatchId(
        final String targetAdapterId,
        final String eventBatchId);

}
