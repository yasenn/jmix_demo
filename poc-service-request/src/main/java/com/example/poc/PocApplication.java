package com.example.poc;

import org.flowable.engine.ProcessEngine;
import org.flowable.image.ProcessDiagramGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class PocApplication {
    public static void main(String[] args) {
        SpringApplication.run(PocApplication.class, args);
    }

    @Bean
    public ProcessDiagramGenerator processDiagramGenerator(ProcessEngine processEngine) {
        return processEngine.getProcessEngineConfiguration().getProcessDiagramGenerator();
    }
}
