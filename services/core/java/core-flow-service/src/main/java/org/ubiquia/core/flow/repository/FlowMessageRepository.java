package org.ubiquia.core.flow.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ubiquia.common.library.api.repository.AbstractEntityRepository;
import org.ubiquia.common.model.ubiquia.entity.FlowMessageEntity;

public interface FlowMessageRepository extends AbstractEntityRepository<FlowMessageEntity> {

    Long countByTargetAdapterId(final String adapterId);

    Page<FlowMessageEntity> findAllByTargetAdapterId(Pageable pageable, final String targetAdapterId);

    List<FlowMessageEntity> findAllByTargetAdapterIdAndFlowEventBatchId(
        final String targetAdapterId,
        final String eventBatchId);

}
