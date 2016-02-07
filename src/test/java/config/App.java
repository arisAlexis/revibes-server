package config;

import org.apache.catalina.Context;
import org.apache.catalina.Server;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

public class App {

    public static Server server;
    public static void stop() throws Exception {

        server.stop();
        server.destroy();

    }

    /**
     *
     * @param webAppPath alternative web.xml location
     * @param staticWWWPath if set tomcat servers also the static content
     * @param sslCertPath
     * @param sslKeyAlias
     * @param sslKeystorePass
     * @param port
     * @throws Exception
     */
    public static void start(String webAppPath,String staticWWWPath,String sslCertPath,String sslKeyAlias,String sslKeystorePass,int port,int sslPort) throws Exception {

        if (server!=null) {

            return;
     /*       System.out.println("STATE "+ server.getState());
            //wait for the previous instance to shut down correctly
            while (server.getState() != LifecycleState.DESTROYED) {
                Thread.sleep(1000);
            }*/

        }

        Connector httpsConnector = new Connector();
        httpsConnector.setPort(sslPort);
        httpsConnector.setSecure(true);
        httpsConnector.setScheme("https");
        httpsConnector.setAttribute("keyAlias", sslKeyAlias);
        httpsConnector.setAttribute("keystorePass", sslKeystorePass);
        httpsConnector.setAttribute("keystoreFile", sslCertPath);
        httpsConnector.setAttribute("clientAuth", "false");
        httpsConnector.setAttribute("sslProtocol", "TLS");
        httpsConnector.setAttribute("SSLEnabled", true);

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);

        tomcat.enableNaming();

        Context webContext=tomcat.addWebapp("/exchange",webAppPath);

        if (staticWWWPath!=null) {
            tomcat.addWebapp("/", staticWWWPath);
        }

        Connector defaultConnector = tomcat.getConnector();
        defaultConnector.setRedirectPort(8443);

        tomcat.getService().addConnector(httpsConnector);
        tomcat.start();

        server = tomcat.getServer();

    }

}