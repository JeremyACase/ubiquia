package org.ubiquia.common.library.dao.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.ubiquia.common.library.api.interfaces.InterfaceEntityToDtoMapper;
import org.ubiquia.common.library.dao.component.EntityDao;
import org.ubiquia.common.library.implementation.service.telemetry.MicroMeterHelper;
import org.ubiquia.common.library.implementation.service.visitor.PageValidator;
import org.ubiquia.common.model.ubiquia.dto.FlowEvent;
import org.ubiquia.common.model.ubiquia.entity.FlowEventEntity;

@ExtendWith(MockitoExtension.class)
public class GenericUbiquiaDaoControllerTest {

    static class TestController extends GenericUbiquiaDaoController<FlowEventEntity, FlowEvent> {

        private static final Logger logger = LoggerFactory.getLogger(TestController.class);

        private final EntityDao<FlowEventEntity> entityDao;
        private final InterfaceEntityToDtoMapper<FlowEventEntity, FlowEvent> mapper;

        TestController(
            EntityDao<FlowEventEntity> entityDao,
            InterfaceEntityToDtoMapper<FlowEventEntity, FlowEvent> mapper) {
            this.entityDao = entityDao;
            this.mapper = mapper;
        }

        @Override
        public Logger getLogger() {
            return logger;
        }

        @Override
        public EntityDao<FlowEventEntity> getDataAccessObject() {
            return this.entityDao;
        }

        @Override
        public InterfaceEntityToDtoMapper<FlowEventEntity, FlowEvent> getDataTransferObjectMapper() {
            return this.mapper;
        }
    }

    @Mock
    private MicroMeterHelper microMeterHelper;

    @Mock
    private EntityDao<FlowEventEntity> entityDao;

    @Mock
    private InterfaceEntityToDtoMapper<FlowEventEntity, FlowEvent> mapper;

    @Mock
    private PageValidator pageValidator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Timer.Sample timerSample;

    @Mock
    private HttpServletRequest httpServletRequest;

    private TestController controller;

    @BeforeEach
    public void setup() {
        this.controller = new TestController(this.entityDao, this.mapper);
        ReflectionTestUtils.setField(this.controller, "microMeterHelper", this.microMeterHelper);
        ReflectionTestUtils.setField(this.controller, "tags", new ArrayList<>());
        ReflectionTestUtils.setField(this.controller, "pageValidator", this.pageValidator);
        ReflectionTestUtils.setField(this.controller, "objectMapper", this.objectMapper);
    }

    @Test
    public void assertMicroMeterSamplesWhenQueryingWithParams_isValid()
        throws NoSuchFieldException, JsonProcessingException {

        when(this.microMeterHelper.startSample()).thenReturn(this.timerSample);
        when(this.httpServletRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(this.entityDao.getPage(anyMap(), anyInt(), anyInt(), anyBoolean(), anyList(), any()))
            .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1), 0));
        when(this.objectMapper.valueToTree(any())).thenReturn(NullNode.getInstance());
        when(this.mapper.map(anyList())).thenReturn(new ArrayList<>());

        this.controller.queryWithParams(0, 25, true, new ArrayList<>(), this.httpServletRequest);

        verify(this.microMeterHelper).startSample();
        verify(this.microMeterHelper).endSample(this.timerSample, "queryWithParams", new ArrayList<>());
    }

    @Test
    public void assertMicroMeterSamplesWhenQueryingById_isValid()
        throws NoSuchFieldException, JsonProcessingException {

        when(this.microMeterHelper.startSample()).thenReturn(this.timerSample);
        when(this.entityDao.getPage(anyMap(), anyInt(), anyInt(), anyBoolean(), anyList(), any()))
            .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1), 0));

        this.controller.queryModelWithId("test-id");

        verify(this.microMeterHelper).startSample();
        verify(this.microMeterHelper).endSample(this.timerSample, "queryModelWithId", new ArrayList<>());
    }

    @Test
    public void assertNoSamplesWhenMicroMeterHelperIsNull_isValid()
        throws NoSuchFieldException, JsonProcessingException {

        ReflectionTestUtils.setField(this.controller, "microMeterHelper", null);
        when(this.httpServletRequest.getParameterMap()).thenReturn(new HashMap<>());
        when(this.entityDao.getPage(anyMap(), anyInt(), anyInt(), anyBoolean(), anyList(), any()))
            .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 1), 0));
        when(this.objectMapper.valueToTree(any())).thenReturn(NullNode.getInstance());
        when(this.mapper.map(anyList())).thenReturn(new ArrayList<>());

        Assertions.assertDoesNotThrow(() ->
            this.controller.queryWithParams(0, 25, true, new ArrayList<>(), this.httpServletRequest));
        Assertions.assertDoesNotThrow(() ->
            this.controller.queryModelWithId("test-id"));
    }
}
