package com.proyecto.servicios;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;



@SpringBootApplication
@EnableScheduling
@EnableFeignClients
@Slf4j

public class App implements CommandLineRunner {

    @Autowired
    private ApplicationContext context;


    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void abrirSwaggerEnNavegador() {
        String url = "http://localhost:8080/swagger-ui/index.html";
        log.info("Abriendo Swagger UI automáticamente en el navegador: {}", url);
        try {
            if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
                Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
            } else if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            }
        } catch (Exception e) {
            log.warn("No se pudo abrir el navegador automáticamente: {}", e.getMessage());
        }
    }

    @Override
    public void run(String... args) {
        // displayInfo(context.getBean(BuildProperties.class));
    }

    private static void displayInfo(BuildProperties buildProperties) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        String out = formatter.format(buildProperties.getTime());
        log.info("Nombre artefacto: " + buildProperties.getName() + "\n"
                + "Versión: " + buildProperties.getVersion() + "\n"
                + "Fecha Compilación: " + out + "\n"
                + "Artefacto: " + buildProperties.getArtifact() + "\n"
                + "Grupo: " + buildProperties.getGroup());
    }
}
