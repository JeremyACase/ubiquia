package org.ubiquia.common.library.logic.service.builder;

import org.springframework.web.bind.annotation.RequestMethod;

public record EndpointRecord(String path, RequestMethod method) {

}
