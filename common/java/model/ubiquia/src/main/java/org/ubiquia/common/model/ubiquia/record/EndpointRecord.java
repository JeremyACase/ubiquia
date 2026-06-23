package org.ubiquia.common.model.ubiquia.record;

import org.springframework.web.bind.annotation.RequestMethod;

/** EndpointRecord model. */
public record EndpointRecord(String path, RequestMethod method) {

}