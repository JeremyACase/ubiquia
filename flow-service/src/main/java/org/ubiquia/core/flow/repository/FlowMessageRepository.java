package org.ubiquia.core.flow.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.ubiquia.core.flow.model.entity.FlowMessage;

public interface FlowMessageRepository extends AbstractEntityRepository<FlowMessage> {

    Long countByTargetAdapterId(final String adapterId);

    Page<FlowMessage> findAllByTargetAdapterId(Pageable pageable, final String targetAdapterId);

    List<FlowMessage> findAllByTargetAdapterIdAndAmigosEventBatchId(
        final String targetAdapterId,
        final String eventBatchId);

}
