package org.ubiquia.core.flow.controller.advice;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.ubiquia.common.library.advice.exception.AbstractGlobalExceptionHandler;

/**
 * Global exception handler that will catch REST errors and communicate them
 * appropriately.
 */
@ControllerAdvice
public class GlobalExceptionHandler extends AbstractGlobalExceptionHandler {

}
