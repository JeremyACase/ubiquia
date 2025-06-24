package org.ubiquia.common.library.belief.state.libraries.interfaces.service.mapper;

import org.slf4j.Logger;
import org.ubiquia.common.model.acl.dto.AbstractAclModel;
import org.ubiquia.common.model.acl.entity.AbstractAclModelEntity;

public interface InterfaceEgressDtoMapper<F extends AbstractAclModelEntity, T extends AbstractAclModel> {

    String getModelType();

    T getNewDto();

    Logger getLogger();

}