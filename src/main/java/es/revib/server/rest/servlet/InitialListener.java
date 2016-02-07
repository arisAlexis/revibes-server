package es.revib.server.rest.servlet;

import com.corundumstudio.socketio.Configuration;
import es.revib.server.rest.broker.SocketIOServerImpl;
import es.revib.server.rest.database.OrientDatabase;
import es.revib.server.rest.util.Globals;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class InitialListener implements ServletContextListener
    {

        @Override
        public void contextDestroyed(ServletContextEvent arg0) {

            OrientDatabase.getInstance().getGraphTx().shutdown();
            OrientDatabase.getInstance().getDocumentDb().close();
            OrientDatabase.shutdown();
            SocketIOServerImpl.getInstance().getServer().stop();
        }

        //Run this before web application is started
        @Override
        public void contextInitialized(ServletContextEvent arg0) {

            OrientDatabase.setHTTP_URL(arg0.getServletContext().getInitParameter("ORIENT_HTTP_URL"));
            OrientDatabase.setUser(arg0.getServletContext().getInitParameter("ORIENT_USER"));
            OrientDatabase.setPassword(arg0.getServletContext().getInitParameter("ORIENT_PASSWORD"));
            OrientDatabase.setUrl(arg0.getServletContext().getInitParameter("ORIENT_CONFIGURATION"));
            OrientDatabase.getInstance().getGraphTx(); //prime on startup

            //socket io server

            Configuration configuration=new Configuration();
            configuration.setHostname("localhost");
            configuration.setPort(8444);
            configuration.setKeyStorePassword(arg0.getServletContext().getInitParameter("SSL_PASSWORD"));

            InputStream stream = null;
            try {
                stream = new FileInputStream(arg0.getServletContext().getInitParameter("SSL_CERT_PATH"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            configuration.setKeyStore(stream);
            SocketIOServerImpl.configuration=configuration;
            SocketIOServerImpl.getInstance();

            Globals.TMP_DIR=arg0.getServletContext().getInitParameter("TMP_DIR");
            Globals.S3_BUCKET=arg0.getServletContext().getInitParameter("S3_BUCKET");
            Globals.S3_FOLDER=arg0.getServletContext().getInitParameter("S3_FOLDER");
            Globals.AWS_ACCESS_KEY=arg0.getServletContext().getInitParameter("AWS_S3_ACCESS");
            Globals.AWS_SECRET_KEY=arg0.getServletContext().getInitParameter("AWS_S3_SECRET");
            Globals.SERVER_URL=arg0.getServletContext().getInitParameter("SERVER_URL");
        }
}
