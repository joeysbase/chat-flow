package chatflow.consumer;

import java.io.File;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;

import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;

public class ServerApp {
  public static void main(String[] args) throws Exception {
    ConsumerManager consumerManager = new ConsumerManager(20, 20, 10, 10000, 5);
    Thread consumerManagerThread = new Thread(consumerManager);
    consumerManagerThread.start();

    CacheMonitor cacheMonitor = new CacheMonitor(10000 * 6);
    Thread cacheMonitorThread = new Thread(cacheMonitor);
    cacheMonitorThread.start();

    try {
      Tomcat tomcat = new Tomcat();
        tomcat.setPort(9090);
        tomcat.getConnector();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());

        Context context = tomcat.addContext("", new File(".").getAbsolutePath());
        context.addServletContainerInitializer(new WsSci(), Set.of());
        Tomcat.addServlet(context, "default", new org.apache.catalina.servlets.DefaultServlet());
        context.addServletMappingDecoded("/", "default");

        tomcat.start();

        ServerContainer serverContainer = (ServerContainer) context.getServletContext()
                .getAttribute("jakarta.websocket.server.ServerContainer");

        serverContainer.addEndpoint(ReceiveEndPoint.class);

        System.out.println("Consumer Service started.");

        tomcat.getServer().await();
    } catch (DeploymentException | LifecycleException e) {
      consumerManager.stop();
      cacheMonitor.stop();
    }
  }
}
