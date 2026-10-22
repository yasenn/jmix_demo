package com.example.poc;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProcessService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final ServiceRequestRepository requestRepository;

    public ProcessService(RuntimeService runtimeService, TaskService taskService,
                          ServiceRequestRepository requestRepository) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.requestRepository = requestRepository;
    }

    @Transactional
    public ServiceRequest startApproval(String title, String description, String approver) {
        ServiceRequest req = new ServiceRequest();
        req.setTitle(title);
        req.setDescription(description);
        req.setApprover(approver);
        req.setStatus("IN_APPROVAL");
        req.setCreatedAt(LocalDateTime.now());
        requestRepository.save(req);

        Map<String, Object> vars = new HashMap<>();
        vars.put("approver", approver);
        vars.put("requestId", req.getId().toString());

        ProcessInstance pi = runtimeService.startProcessInstanceByKey(
                "serviceRequestApproval", req.getId().toString(), vars);
        req.setProcessInstanceId(pi.getId());
        requestRepository.save(req);
        return req;
    }

    public Task getActiveTask(String processInstanceId) {
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstanceId).list();
        return tasks.isEmpty() ? null : tasks.get(0);
    }

    @Transactional
    public void completeTask(String processInstanceId, boolean approved) {
        Task task = getActiveTask(processInstanceId);
        if (task == null) throw new IllegalStateException("No active task");
        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", approved);
        taskService.complete(task.getId(), vars);

        ServiceRequest req = requestRepository.findAll().stream()
                .filter(r -> processInstanceId.equals(r.getProcessInstanceId()))
                .findFirst().orElseThrow();
        req.setStatus(approved ? "APPROVED" : "REJECTED");
        requestRepository.save(req);
    }

    public boolean isProcessEnded(String processInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        return pi == null;
    }
}
