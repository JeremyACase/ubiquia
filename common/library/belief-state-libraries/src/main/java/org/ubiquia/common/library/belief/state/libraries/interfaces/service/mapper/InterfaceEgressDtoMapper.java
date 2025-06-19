package org.ubiquia.common.library.belief.state.libraries.interfaces.service.mapper;

import org.slf4j.Logger;
import org.ubiquia.common.model.acl.dto.AbstractAclEntityDto;
import org.ubiquia.common.model.acl.entity.AbstractAclEntity;

public interface InterfaceEgressDtoMapper<F extends AbstractAclEntity, T extends AbstractAclEntityDto> {

    String getModelType();

    T getNewDto();

    Logger getLogger();

}