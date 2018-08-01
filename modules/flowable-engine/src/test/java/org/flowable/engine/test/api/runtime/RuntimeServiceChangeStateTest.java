/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.test.api.runtime;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.FlowableActivityCancelledEvent;
import org.flowable.engine.delegate.event.FlowableActivityEvent;
import org.flowable.engine.delegate.event.FlowableCancelledEvent;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.DataObject;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.task.api.Task;
import org.flowable.variable.api.event.FlowableVariableEvent;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class RuntimeServiceChangeStateTest extends PluggableFlowableTestCase {

    private ChangeStateEventListener changeStateEventListener = new ChangeStateEventListener();
    @Override
    protected void initializeProcessEngine() {
        super.initializeProcessEngine();
        processEngine.getRuntimeService().addEventListener(changeStateEventListener);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        changeStateEventListener.clear();
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityBackwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("secondTask", "firstTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentExecutionBackwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "firstTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentActivityForwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("firstTask", "secondTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml" })
    public void testSetCurrentExecutionForwardForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "secondTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job job = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();

        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("firstTask", "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("secondTask", "firstTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionToActivityWithTimerForSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcess");

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "firstTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksProcessWithTimers.bpmn20.xml" })
    public void testSetCurrentActivityWithTimerToActivityWithTimerSimpleProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksProcessWithTimers");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());
        Execution execution = runtimeService.createExecutionQuery().parentId(task.getExecutionId()).singleResult();
        Job timerJob1 = managementService.createTimerJobQuery().executionId(execution.getId()).singleResult();
        assertNotNull(timerJob1);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("firstTask", "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());
        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob2);
        assertTrue(!timerJob1.getExecutionId().equals(timerJob2.getExecutionId()));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("firstTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        entityEvent = (FlowableEngineEntityEvent)event;
        timer = (Job) entityEvent.getEntity();
        assertEquals("secondTimerEvent", getActivityId(timer));

        assertTrue(!iterator.hasNext());

        Job job = managementService.moveTimerToExecutableJob(timerJob2.getId());
        managementService.executeJob(job.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("thirdTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityIntoSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionIntoSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityIntoSubProcessWithModeledDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess")
            .singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "name", String.class));
        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionIntoSubProcessWithModeledDataObject() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess")
            .singleResult();
        assertNotNull(runtimeService.getVariableLocal(subProcessExecution.getId(), "name", String.class));

        DataObject nameDataObject = runtimeService.getDataObjectLocal(subProcessExecution.getId(), "name");
        assertNotNull(nameDataObject);
        assertEquals("John", nameDataObject.getValue());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("name", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("John", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityOutOfSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentExecutionToTaskInSubProcessWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcessWithTimer.bpmn20.xml" })
    public void testSetCurrentActivityToTaskInSubProcessAndExecuteTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        Job executableJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableJob.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfSubProcessTaskWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "subTask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfSubProcessTaskWithTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.JOB_CANCELED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    // TODO
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToTaskWithTimerInSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionToTaskWithTimerInSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();

        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask2", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskWithTimerInSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityToTaskWithTimerInSubProcessAndExecuteTimer() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("taskBefore", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(4, executions.size());
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertNotNull(timerJob);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.TIMER_SCHEDULED, event.getType());
        FlowableEngineEntityEvent entityEvent = (FlowableEngineEntityEvent)event;
        Job timer = (Job) entityEvent.getEntity();
        assertEquals("boundaryTimerEvent", getActivityId(timer));

        assertTrue(!iterator.hasNext());

        Job executableTimerJob = managementService.moveTimerToExecutableJob(timerJob.getId());
        managementService.executeJob(executableTimerJob.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfNestedSubProcessExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask", "subTaskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTaskAfter", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfNestedSubProcessExecution() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTaskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTaskAfter", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityOutOfNestedSubProcessExecutionIntoContainingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask", "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskNestedSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionOutOfNestedSubProcessExecutionIntoContainingSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startNestedSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("nestedSubProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nestedSubTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTaskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/taskTwoSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityFromSubProcessToAnotherSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcesses");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subtask", "subtask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/taskTwoSubProcesses.bpmn20.xml" })
    public void testSetCurrentExecutionFromSubProcessToAnotherSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoSubProcesses");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "subtask2")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subtask2", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentActivityForSubProcessWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask", "taskBefore")
            .processVariable("processVar1", "test")
            .processVariable("processVar2", 10)
            .localVariable("taskBefore", "localVar1", "test2")
            .localVariable("taskBefore", "localVar2", 20)
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());
        assertEquals("test", processVariables.get("processVar1"));
        assertEquals(10, processVariables.get("processVar2"));
        assertNull(processVariables.get("localVar1"));
        assertNull(processVariables.get("localVar2"));

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("taskBefore").singleResult();
        Map<String, Object> localVariables = runtimeService.getVariablesLocal(execution.getId());
        assertEquals("test2", localVariables.get("localVar1"));
        assertEquals(20, localVariables.get("localVar2"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar2", ((FlowableVariableEvent)event).getVariableName());
        assertEquals(10, ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar1", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("test", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar2", ((FlowableVariableEvent)event).getVariableName());
        assertEquals(20, ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar1", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("test2", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskSubProcess.bpmn20.xml" })
    public void testSetCurrentExecutionForSubProcessWithVariables() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startSimpleSubProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(task.getExecutionId(), "taskBefore")
            .processVariable("processVar1", "test")
            .processVariable("processVar2", 10)
            .localVariable("taskBefore", "localVar1", "test2")
            .localVariable("taskBefore", "localVar2", 20)
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, executions.size());

        Map<String, Object> processVariables = runtimeService.getVariables(processInstance.getId());
        assertEquals("test", processVariables.get("processVar1"));
        assertEquals(10, processVariables.get("processVar2"));
        assertNull(processVariables.get("localVar1"));
        assertNull(processVariables.get("localVar2"));

        Execution execution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("taskBefore").singleResult();
        Map<String, Object> localVariables = runtimeService.getVariablesLocal(execution.getId());
        assertEquals("test2", localVariables.get("localVar1"));
        assertEquals(20, localVariables.get("localVar2"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar2", ((FlowableVariableEvent)event).getVariableName());
        assertEquals(10, ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("processVar1", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("test", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar2", ((FlowableVariableEvent)event).getVariableName());
        assertEquals(20, ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.VARIABLE_CREATED, event.getType());
        assertEquals("localVar1", ((FlowableVariableEvent)event).getVariableName());
        assertEquals("test2", ((FlowableVariableEvent)event).getVariableValue());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("subTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("task1");
        newActivityIds.add("task2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("task1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("task2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleActivitiesToSingleActivityAfterParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("task1");
        currentActivityIds.add("task2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("task1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("task2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskAfter", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetCurrentExecutionToMultipleActivitiesForParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        Execution taskBeforeExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().singleResult();

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("task1");
        newActivityIds.add("task2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleExecutionToActivityIds(taskBeforeExecution.getId(), newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("task1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("task2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelTask.bpmn20.xml" })
    public void testSetMultipleExecutionsToSingleActivityAfterParallelGateway() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        List<String> currentExecutionIds = new ArrayList<>();
        currentExecutionIds.add(executions.get(0).getId());
        currentExecutionIds.add(executions.get(1).getId());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionsToSingleActivityId(currentExecutionIds, "taskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        Set<String> expectedTaskActivityIds = new HashSet<>(Arrays.asList("task2", "task1"));
        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        String taskActivityId = ((FlowableActivityEvent)event).getActivityId();
        assertTrue(expectedTaskActivityIds.contains(taskActivityId));
        expectedTaskActivityIds.remove(taskActivityId);

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        taskActivityId = ((FlowableActivityEvent)event).getActivityId();
        assertTrue(expectedTaskActivityIds.contains(taskActivityId));

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskAfter", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("subtask");
        newActivityIds.add("subtask2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelSubProcesses.bpmn20.xml" })
    public void testSetMultipleActivitiesToSingleActivityAfterParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("subtask");
        currentActivityIds.add("subtask2");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        assertEquals("taskAfter", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskAfter", ((FlowableActivityEvent)event).getActivityId());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/parallelSubProcessesMultipleTasks.bpmn20.xml" })
    public void testMoveCurrentActivityInParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        Execution subProcessExecution = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).activityId("subProcess1")
            .singleResult();
        String subProcessExecutionId = subProcessExecution.getId();
        runtimeService.setVariableLocal(subProcessExecutionId, "subProcessVar", "test");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subtask", "subtask2")
            .changeState();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        subProcessExecution = runtimeService.createExecutionQuery().executionId(subProcessExecutionId).singleResult();
        assertNotNull(subProcessExecution);
        assertEquals("test", runtimeService.getVariableLocal(subProcessExecutionId, "subProcessVar"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(tasks.get(0).getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(3, executions.size());

        taskService.complete(tasks.get(0).getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivityToMultipleActivitiesForInclusiveAndParallelSubProcesses() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", Collections.singletonMap("var1", "test2"));
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        List<String> newActivityIds = new ArrayList<>();
        newActivityIds.add("taskInclusive3");
        newActivityIds.add("subtask");
        newActivityIds.add("subtask3");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveSingleActivityIdToActivityIds("taskBefore", newActivityIds)
            .changeState();

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(5, executions.size());

        Execution parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNull(parallelJoinExecution);

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskBefore", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subProcess1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask3", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskInclusive3", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").singleResult();
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask2").singleResult();
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, executions.size());

        parallelJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("parallelJoin")) {
                parallelJoinExecution = execution;
                break;
            }
        }

        assertNotNull(parallelJoinExecution);
        assertTrue(!((ExecutionEntity) parallelJoinExecution).isActive());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").singleResult();
        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivitiesToSingleActivityForInclusiveAndParallelSubProcesses() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("var1", "test2");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", variableMap);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive1").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").count());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("taskInclusive3");
        currentActivityIds.add("subtask");
        currentActivityIds.add("subtask3");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskAfter")
            .changeState();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(1, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("taskInclusive3", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask3", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskAfter", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());
        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/multipleParallelSubProcesses.bpmn20.xml" })
    public void testSetCurrentActivitiesToSingleActivityInInclusiveGateway() {
        Map<String, Object> variableMap = new HashMap<>();
        variableMap.put("var1", "test2");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("startParallelProcess", variableMap);
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskBefore", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        List<Task> tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive1").singleResult();
        assertNotNull(task);
        taskService.complete(task.getId());

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(3, tasks.size());

        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").count());

        List<String> currentActivityIds = new ArrayList<>();
        currentActivityIds.add("subtask");
        currentActivityIds.add("subtask3");

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdsToSingleActivityId(currentActivityIds, "taskInclusive1")
            .changeState();

        tasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).list();
        assertEquals(2, tasks.size());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subtask3", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("subProcess2", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("taskInclusive1", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive3").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("taskInclusive1").singleResult();
        taskService.complete(task.getId());

        executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(5, executions.size());

        Execution inclusiveJoinExecution = null;
        for (Execution execution : executions) {
            if (execution.getActivityId().equals("inclusiveJoin")) {
                inclusiveJoinExecution = execution;
                break;
            }
        }

        assertNotNull(inclusiveJoinExecution);
        assertTrue(!((ExecutionEntity) inclusiveJoinExecution).isActive());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask3").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).taskDefinitionKey("subtask2").singleResult();
        taskService.complete(task.getId());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("taskAfter", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInParentProcess() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("theTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(subProcessInstance.getId())
            .moveActivityIdToParentActivityId("theTask", "secondTask")
            .changeState();

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        assertEquals(0, runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).count());
        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count());

        List<Execution> executions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, executions.size());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("theTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.PROCESS_CANCELLED, event.getType());
        assertEquals(subProcessInstance.getId(), ((FlowableCancelledEvent)event).getProcessInstanceId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("callActivity", ((FlowableActivityEvent)event).getActivityId());
        assertEquals(processInstance.getId(), ((FlowableActivityEvent)event).getProcessInstanceId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("secondTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = { "org/flowable/engine/test/api/twoTasksParentProcess.bpmn20.xml", "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testSetCurrentActivityInSubProcessInstance() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("twoTasksParentProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("firstTask", task.getTaskDefinitionKey());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdToSubProcessInstanceActivityId("firstTask", "theTask", "callActivity")
            .changeState();

        ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
        assertNotNull(subProcessInstance);

        assertEquals(0, taskService.createTaskQuery().processInstanceId(processInstance.getId()).count());
        assertEquals(1, taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).count());

        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count());
        assertEquals(1, runtimeService.createExecutionQuery().processInstanceId(subProcessInstance.getId()).onlyChildExecutions().count());


        task = taskService.createTaskQuery().processInstanceId(subProcessInstance.getId()).singleResult();
        assertEquals("theTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("firstTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("callActivity", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.PROCESS_CREATED, event.getType());
        assertEquals(subProcessInstance.getId(), ((ExecutionEntity)((FlowableEntityEvent)event).getEntity()).getId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("theTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertEquals(0, runtimeService.createProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).count());

        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("secondTask", task.getTaskDefinitionKey());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentActivityOfSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 5)
            .start();

        List<Execution> seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, seqExecutions.size());
        List<Task> activeSeqTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(1, activeSeqTasks.size());

        //First in the loop
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(0, taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        //Second in the loop
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(1, taskService.getVariable(task.getId(), "loopCounter"));

        Execution taskInstanceExecution = seqExecutions.stream().filter(e -> !((DelegateExecution)e).isMultiInstanceRoot()).findFirst().orElse(null);
        Execution multiInstanceRootExecution = seqExecutions.stream().filter(e -> ((DelegateExecution)e).isMultiInstanceRoot()).findFirst().orElse(null);
        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("seqTasks", "nextTask")
            .changeState();

        seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, seqExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        assertNull(taskService.getVariable(task.getId(), "loopCounter"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("seqTasks", ((FlowableActivityEvent)event).getActivityId());
        FlowableActivityEvent activityEvent = (FlowableActivityEvent)event;
        assertEquals(taskInstanceExecution.getId(), activityEvent.getExecutionId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        activityEvent = (FlowableActivityEvent)event;
        assertEquals("seqTasks", activityEvent.getActivityId());
        assertEquals(multiInstanceRootExecution.getId(), activityEvent.getExecutionId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nextTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceSequential.bpmn20.xml")
    public void testSetCurrentParentExecutionOfSequentialMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("sequentialMultiInstance")
            .variable("nrOfLoops", 5)
            .start();

        List<Execution> seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(2, seqExecutions.size());
        List<Task> activeSeqTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(1, activeSeqTasks.size());

        //First in the loop
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(0, taskService.getVariable(task.getId(), "loopCounter"));
        taskService.complete(task.getId());

        //Second in the loop
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("seqTasks", task.getTaskDefinitionKey());
        assertEquals(1, taskService.getVariable(task.getId(), "loopCounter"));

        //move the parent execution - otherwise the parent multi instance execution remains, although active==false.
        String parentExecutionId = runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult().getParentId();

        Execution taskInstanceExecution = seqExecutions.stream().filter(e -> !((DelegateExecution)e).isMultiInstanceRoot()).findFirst().orElse(null);
        Execution multiInstanceRootExecution = seqExecutions.stream().filter(e -> ((DelegateExecution)e).isMultiInstanceRoot()).findFirst().orElse(null);
        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(parentExecutionId, "nextTask")
            .changeState();

        seqExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, seqExecutions.size());
        task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());
        assertNull(taskService.getVariable(task.getId(), "loopCounter"));

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("seqTasks", ((FlowableActivityEvent)event).getActivityId());
        FlowableActivityEvent activityEvent = (FlowableActivityEvent)event;
        assertEquals(taskInstanceExecution.getId(), activityEvent.getExecutionId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        activityEvent = (FlowableActivityEvent)event;
        assertEquals("seqTasks", activityEvent.getActivityId());
        assertEquals(multiInstanceRootExecution.getId(), activityEvent.getExecutionId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nextTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentActivityOfParallelMultiInstanceTask() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        List<Execution> parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        List<Task> activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(3, activeParallelTasks.size());

        //Complete one of the tasks
        taskService.complete(activeParallelTasks.get(1).getId());
        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        activeParallelTasks = taskService.createTaskQuery().processInstanceId(processInstance.getId()).active().list();
        assertEquals(2, activeParallelTasks.size());

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelTasks", "nextTask")
            .changeState();

        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, parallelExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("parallelTasks", ((FlowableActivityEvent)event).getActivityId());
        FlowableActivityEvent activityEvent = (FlowableActivityEvent)event;

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        activityEvent = (FlowableActivityEvent)event;
        assertEquals("parallelTasks", activityEvent.getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        activityEvent = (FlowableActivityEvent)event;
        assertEquals("parallelTasks", activityEvent.getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nextTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallel.bpmn20.xml")
    public void testSetCurrentParentExecutionOfParallelMultiInstanceTask() {
        ProcessInstance parallelTasksProcInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstance")
            .variable("nrOfLoops", 3)
            .start();

        List<Execution> parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(parallelTasksProcInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        List<Task> activeParallelTasks = taskService.createTaskQuery().processInstanceId(parallelTasksProcInstance.getId()).active().list();
        assertEquals(3, activeParallelTasks.size());

        //Complete one of the tasks
        taskService.complete(activeParallelTasks.get(1).getId());
        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(parallelTasksProcInstance.getId()).onlyChildExecutions().list();
        assertEquals(4, parallelExecutions.size());
        activeParallelTasks = taskService.createTaskQuery().processInstanceId(parallelTasksProcInstance.getId()).active().list();
        assertEquals(2, activeParallelTasks.size());

        //Fetch the parent execution of the multi instance task execution
        String parentExecutionId = runtimeService.createExecutionQuery().executionId(activeParallelTasks.get(0).getExecutionId()).singleResult().getParentId();

        changeStateEventListener.clear();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(parentExecutionId, "nextTask")
            .changeState();

        parallelExecutions = runtimeService.createExecutionQuery().processInstanceId(parallelTasksProcInstance.getId()).onlyChildExecutions().list();
        assertEquals(1, parallelExecutions.size());
        Task task = taskService.createTaskQuery().processInstanceId(parallelTasksProcInstance.getId()).singleResult();
        assertEquals("nextTask", task.getTaskDefinitionKey());

        // Verify events
        Iterator<FlowableEvent> iterator = changeStateEventListener.iterator();
        assertTrue(iterator.hasNext());

        FlowableEvent event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        assertEquals("parallelTasks", ((FlowableActivityEvent)event).getActivityId());
        FlowableActivityEvent activityEvent = (FlowableActivityEvent)event;

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        activityEvent = (FlowableActivityEvent)event;
        assertEquals("parallelTasks", activityEvent.getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_CANCELLED, event.getType());
        activityEvent = (FlowableActivityEvent)event;
        assertEquals("parallelTasks", activityEvent.getActivityId());

        assertTrue(iterator.hasNext());
        event = iterator.next();
        assertEquals(FlowableEngineEventType.ACTIVITY_STARTED, event.getType());
        assertEquals("nextTask", ((FlowableActivityEvent)event).getActivityId());

        assertTrue(!iterator.hasNext());

        taskService.complete(task.getId());

        assertProcessEnded(parallelTasksProcInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentExecutionWithinMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Move one of the executions within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(subTask1Executions.get(0).getId(), "subTask2")
            .changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Complete one of the parallel subProcesses "subTask2"
        Task task = taskService.createTaskQuery().executionId(subTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(0, subTask2Executions.size());

        //Move the other two executions, one by one
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        subTask1Executions.forEach(e -> changeActivityStateBuilder.moveExecutionToActivityId(e.getId(), "subTask2"));
        changeActivityStateBuilder.changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(5, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(2, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(0, subTask1Executions.size());
        subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(2, subTask2Executions.size());

        //Complete the rest of the SubProcesses
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(2, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentActivityWithinMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Move one of the executions within the multiInstance subProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("subTask1", "subTask2")
            .changeState();

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(0, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the parallel subProcesses "subTask2"
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        Task task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentExecutionWithinNestedMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Move one of the executions within of the nested multiInstance subProcesses
        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(nestedSubTask1Executions.get(0).getId(), "nestedSubTask2")
            .moveExecutionToActivityId(nestedSubTask1Executions.get(3).getId(), "nestedSubTask2")
            .moveExecutionToActivityId(nestedSubTask1Executions.get(6).getId(), "nestedSubTask2")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the outer subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move the rest of the nestedSubTask1 executions
        ChangeActivityStateBuilder changeActivityStateBuilder = runtimeService.createChangeActivityStateBuilder();
        nestedSubTask1Executions.forEach(e -> changeActivityStateBuilder.moveExecutionToActivityId(e.getId(), "nestedSubTask2"));
        changeActivityStateBuilder.changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(0, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(8, nestedSubTask2Executions.size());

        //Complete all the nestedSubTask2
        tasks = taskService.createTaskQuery().taskDefinitionKey("nestedSubTask2").list();
        assertEquals(8, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentActivityWithinNestedMultiInstanceParallelSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete one task for each nestedSubProcess
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Moving the nestedSubTask1 activity should move all its executions
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("nestedSubTask1", "nestedSubTask2")
            .changeState();

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(0, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(9, nestedSubTask2Executions.size());

        //Complete all the nestedSubTask2
        tasks = taskService.createTaskQuery().taskDefinitionKey("nestedSubTask2").list();
        assertEquals(9, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        Task task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentMultiInstanceSubProcessParentExecutionWithinProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Complete one of the Tasks
        Task task = taskService.createTaskQuery().executionId(subTask1Executions.get(1).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Move the parallelSubProcess via the parentExecution Ids
        String ParallelSubProcessParentExecutionId = runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId("parallelSubProcess")
            .list()
            .stream()
            .findFirst()
            .map(e -> e.getParentId())
            .get();

        runtimeService.createChangeActivityStateBuilder()
            .moveExecutionToActivityId(ParallelSubProcessParentExecutionId, "lastTask")
            .changeState();

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceParallelSubProcess.bpmn20.xml")
    public void testSetCurrentMultiInstanceSubProcessParentActivityWithinProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelMultiInstanceSubProcess")
            .variable("nrOfLoops", 3)
            .start();

        //One of the child executions is the parent of the multiInstance "loop"
        long executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Complete one of the Tasks
        Task task = taskService.createTaskQuery().executionId(subTask1Executions.get(1).getId()).singleResult();
        taskService.complete(task.getId());

        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, executionsCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcess").count();
        assertEquals(3, parallelSubProcessCount);
        subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(2, subTask1Executions.size());
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(1, subTask2Executions.size());

        //Move the parallelSubProcess
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelSubProcess", "lastTask")
            .changeState();

        //There's no multiInstance anymore
        executionsCount = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, executionsCount);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentMultiInstanceNestedSubProcessParentExecutionWithinSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete some of the Nested tasks
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the nested subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move each nested multiInstance parent
        Stream<String> parallelNestedSubProcessesParentIds = runtimeService.createExecutionQuery()
            .processInstanceId(processInstance.getId())
            .activityId("parallelNestedSubProcess")
            .list()
            .stream()
            .map(e -> e.getParentId())
            .distinct();

        parallelNestedSubProcessesParentIds.forEach(parentId -> {
            runtimeService.createChangeActivityStateBuilder()
                .moveExecutionToActivityId(parentId, "subTask2")
                .changeState();
        });

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    @Deployment(resources = "org/flowable/engine/test/api/multiInstanceNestedParallelSubProcesses.bpmn20.xml")
    public void testSetCurrentMultiInstanceNestedSubProcessParentActivityWithinSubProcess() {
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder().processDefinitionKey("parallelNestedMultiInstanceSubProcesses").start();

        long totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        long parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask1Executions = runtimeService.createExecutionQuery().activityId("subTask1").list();
        assertEquals(3, subTask1Executions.size());

        //Start the nested subProcesses by completing the first task of the outer subProcess
        List<Task> tasks = taskService.createTaskQuery().taskDefinitionKey("subTask1").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        // 3 instances of the outerSubProcess and each have 3 instances of a nestedSubProcess, for a total of 9 nestedSubTask executions
        // 9 nestedSubProcess instances and 3 outerSubProcesses instances -> 12 executions
        // 1 Parent execution for the outerSubProcess and 1 parent for each nestedSubProcess -> 4 extra parent executions
        // Grand Total ->
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        List<Execution> nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(9, nestedSubTask1Executions.size());

        //Complete some of the Nested tasks
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(0).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(3).getId()).singleResult().getId());
        taskService.complete(taskService.createTaskQuery().executionId(nestedSubTask1Executions.get(6).getId()).singleResult().getId());

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(25, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(9, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        List<Execution> nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(3, nestedSubTask2Executions.size());

        //Complete one of the nested subProcesses
        Task task = taskService.createTaskQuery().executionId(nestedSubTask2Executions.get(0).getId()).singleResult();
        taskService.complete(task.getId());

        //One less task execution and one less nested instance
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(23, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelNestedSubProcess").count();
        assertEquals(8, parallelSubProcessCount);
        nestedSubTask1Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask1").list();
        assertEquals(6, nestedSubTask1Executions.size());
        nestedSubTask2Executions = runtimeService.createExecutionQuery().activityId("nestedSubTask2").list();
        assertEquals(2, nestedSubTask2Executions.size());

        //Move the activity nested multiInstance parent
        runtimeService.createChangeActivityStateBuilder()
            .processInstanceId(processInstance.getId())
            .moveActivityIdTo("parallelNestedSubProcess", "subTask2")
            .changeState();

        //Nested subProcesses have completed, only outer subProcess remain
        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(7, totalChildExecutions);
        parallelSubProcessCount = runtimeService.createExecutionQuery().activityId("parallelSubProcessOuter").count();
        assertEquals(3, parallelSubProcessCount);
        List<Execution> subTask2Executions = runtimeService.createExecutionQuery().activityId("subTask2").list();
        assertEquals(3, subTask2Executions.size());

        //Complete the outer subProcesses
        tasks = taskService.createTaskQuery().taskDefinitionKey("subTask2").list();
        assertEquals(3, tasks.size());
        tasks.forEach(t -> taskService.complete(t.getId()));

        totalChildExecutions = runtimeService.createExecutionQuery().processInstanceId(processInstance.getId()).onlyChildExecutions().count();
        assertEquals(1, totalChildExecutions);

        task = taskService.createTaskQuery().active().singleResult();
        assertEquals("lastTask", task.getTaskDefinitionKey());
        taskService.complete(task.getId());

        assertProcessEnded(processInstance.getId());
    }

    class ChangeStateEventListener extends AbstractFlowableEngineEventListener {
        private List<FlowableEvent> events = new ArrayList<>();

        public ChangeStateEventListener() {

        }

        @Override
        protected void activityStarted(FlowableActivityEvent event) {
            List<String> types = Arrays.asList("userTask", "subProcess", "callActivity");

            if(types.contains(event.getActivityType())) {
                events.add(event);
            }
        }

        @Override
        protected void activityCancelled(FlowableActivityCancelledEvent event) {
            List<String> types = Arrays.asList("userTask", "subProcess", "callActivity");

            if(types.contains(event.getActivityType())) {
                events.add(event);
            }
        }

        @Override
        protected void timerScheduled(FlowableEngineEntityEvent event) {
            events.add(event);
        }

        @Override
        protected void processCreated(FlowableEngineEntityEvent event) {
            events.add(event);
        }

        @Override
        protected void jobCancelled(FlowableEngineEntityEvent event) {
            events.add(event);
        }

        @Override
        protected void processCancelled(FlowableCancelledEvent event) {
            events.add(event);
        }

        @Override
        protected void variableUpdatedEvent(FlowableVariableEvent event) {
            events.add(event);
        }

        @Override
        protected void variableCreated(FlowableVariableEvent event) {
            events.add(event);
        }

        public void clear() {
            events.clear();
        }

        public Iterator<FlowableEvent> iterator() {
            return events.iterator();
        }

        public boolean hasEvents() {
            return events.isEmpty();
        }

        public int eventCount() {
            return events.size();
        }
    }

    private String getActivityId(Job job) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map<String, Object> jobConfigurationMap = objectMapper.readValue(job.getJobHandlerConfiguration(), new TypeReference<Map<String, Object>>() {
            });
            return (String) jobConfigurationMap.get("activityId");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
