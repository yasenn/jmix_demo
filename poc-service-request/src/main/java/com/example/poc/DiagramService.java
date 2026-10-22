package com.example.poc;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DiagramService {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ProcessDiagramGenerator diagramGenerator;

    public DiagramService(RepositoryService repositoryService,
                          RuntimeService runtimeService,
                          HistoryService historyService,
                          ProcessDiagramGenerator diagramGenerator) {
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.historyService = historyService;
        this.diagramGenerator = diagramGenerator;
    }

    public InputStream getDiagram(String processInstanceId) {
        ProcessInstance pi = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (pi != null) {
            BpmnModel model = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
            List<String> active = runtimeService.getActiveActivityIds(processInstanceId);
            return diagramGenerator.generateDiagram(
                    model, "png", active, Collections.emptyList(), 1.0, true);
        }
        HistoricProcessInstance hpi = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        if (hpi == null) throw new IllegalStateException("Process not found");
        List<String> highlighted = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId).finished().list().stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());
        BpmnModel model = repositoryService.getBpmnModel(hpi.getProcessDefinitionId());
        return diagramGenerator.generateDiagram(
                model, "png", highlighted, Collections.emptyList(), 1.0, true);
    }

    public InputStream getPreview(String processDefinitionKey) {
        ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(processDefinitionKey)
                .latestVersion()
                .singleResult();
        if (pd == null) throw new IllegalStateException("Process definition not found: " + processDefinitionKey);
        BpmnModel model = repositoryService.getBpmnModel(pd.getId());
        return diagramGenerator.generateDiagram(
                model, "png", Collections.emptyList(), Collections.emptyList(), 1.0, true);
    }
}
