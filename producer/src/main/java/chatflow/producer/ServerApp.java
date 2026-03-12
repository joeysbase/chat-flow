package chatflow.producer;

import java.io.File;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;

import jakarta.websocket.server.ServerContainer;

public class ServerApp {
    public static void main(String[] args) throws Exception {
        if(args.length != 1) {
            System.err.println("Usage: java ServerApp <MQHost>");
            System.exit(1);
        }
        ConnectionManager.init(args[0]);
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.getConnector();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());

        Context context = tomcat.addContext("", new File(".").getAbsolutePath());
        context.addServletContainerInitializer(new WsSci(), Set.of());
        Tomcat.addServlet(context, "default", new org.apache.catalina.servlets.DefaultServlet());
        context.addServletMappingDecoded("/", "default");
        Tomcat.addServlet(context, "healthServlet", new HealthServlet());
        context.addServletMappingDecoded("/health", "healthServlet");

        tomcat.start();

        ServerContainer serverContainer = (ServerContainer) context.getServletContext()
                .getAttribute("jakarta.websocket.server.ServerContainer");

        serverContainer.addEndpoint(SendEndPoint.class);

        System.out.println("Producer Service started.");

        tomcat.getServer().await();
    }
}