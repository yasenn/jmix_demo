package com.example.poc;

import org.flowable.task.api.Task;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.UUID;

@Controller
public class ProcessController {

    private final ProcessService processService;
    private final DiagramService diagramService;
    private final ServiceRequestRepository requestRepository;
    private final Mock1CService mock1CService;

    public ProcessController(ProcessService processService,
                             DiagramService diagramService,
                             ServiceRequestRepository requestRepository,
                             Mock1CService mock1CService) {
        this.processService = processService;
        this.diagramService = diagramService;
        this.requestRepository = requestRepository;
        this.mock1CService = mock1CService;
    }

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("requests", requestRepository.findAll());
        return "index";
    }

    @PostMapping("/requests")
    public String create(@RequestParam String title,
                         @RequestParam String description,
                         @RequestParam String approver) {
        ServiceRequest req = processService.startApproval(title, description, approver);
        return "redirect:/requests/" + req.getId();
    }

    @GetMapping("/requests/{id}")
    public String view(@PathVariable UUID id, Model model) {
        ServiceRequest req = requestRepository.findById(id).orElseThrow();
        model.addAttribute("request", req);
        Task task = null;
        if (req.getProcessInstanceId() != null && !processService.isProcessEnded(req.getProcessInstanceId())) {
            task = processService.getActiveTask(req.getProcessInstanceId());
        }
        model.addAttribute("task", task);
        return "request";
    }

    @PostMapping("/requests/{id}/approve")
    public String approve(@PathVariable UUID id) {
        ServiceRequest req = requestRepository.findById(id).orElseThrow();
        processService.completeTask(req.getProcessInstanceId(), true);
        return "redirect:/requests/" + id;
    }

    @PostMapping("/requests/{id}/reject")
    public String reject(@PathVariable UUID id) {
        ServiceRequest req = requestRepository.findById(id).orElseThrow();
        processService.completeTask(req.getProcessInstanceId(), false);
        return "redirect:/requests/" + id;
    }

    @GetMapping("/requests/{id}/diagram")
    public ResponseEntity<InputStreamResource> diagram(@PathVariable UUID id) {
        ServiceRequest req = requestRepository.findById(id).orElseThrow();
        if (req.getProcessInstanceId() == null) return ResponseEntity.notFound().build();
        InputStream is = diagramService.getDiagram(req.getProcessInstanceId());
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/process/preview")
    public ResponseEntity<InputStreamResource> preview() {
        InputStream is = diagramService.getPreview("serviceRequestApproval");
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new InputStreamResource(is));
    }

    @GetMapping("/mock-1c")
    public String mock1c(Model model) {
        model.addAttribute("payments", mock1CService.payments());
        model.addAttribute("total",   mock1CService.total());
        model.addAttribute("paid",    mock1CService.paidTotal());
        model.addAttribute("pending", mock1CService.pendingTotal());
        return "mock-1c";
    }
}
