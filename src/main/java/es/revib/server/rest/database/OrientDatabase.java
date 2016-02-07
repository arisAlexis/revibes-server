package es.revib.server.rest.database;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePool;
import com.orientechnologies.orient.core.db.OPartitionedDatabasePoolFactory;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;

public class OrientDatabase  {

    private static OrientGraphFactory graphFactory;
    private static OPartitionedDatabasePoolFactory documentFactory;
    private static OPartitionedDatabasePool pool;
    private static OrientDatabase instance;
    private static String url;
    private static String HTTP_URL;
    private static String user;
    private static String password;

    public static void shutdown() {
        instance=null;
        pool.close();
        documentFactory.close();
        graphFactory.close();
    }

    public static String getHTTP_URL() {
        return HTTP_URL;
    }

    public static void setHTTP_URL(String HTTP_URL) {
        OrientDatabase.HTTP_URL = HTTP_URL;
    }

    public static String getUser() {
        return user;
    }

    public static void setUser(String user) {
        OrientDatabase.user = user;
    }

    public static String getPassword() {
        return password;
    }

    public static void setPassword(String password) {
        OrientDatabase.password = password;
    }

    public static void setUrl(String u) {
        url =u;
    }

    public OrientGraphFactory getGraphFactory() {
        return graphFactory;
    }

    public OrientDatabase(String url) {

        OGlobalConfiguration.ENVIRONMENT_ALLOW_JVM_SHUTDOWN.setValue(false);

        graphFactory=new OrientGraphFactory(url+"/exchange",user,password);
        documentFactory=new OPartitionedDatabasePoolFactory();
        pool=documentFactory.get(url+"/messaging",user,password);
    }

    public static OrientDatabase getInstance() {
        if (instance==null) {
            instance=new OrientDatabase(url);
        }
        return instance;
    }

    public ODatabaseDocumentTx getDocumentDb() {

        return pool.acquire();
    }

    public OrientGraphNoTx getGraphNoTx() {
        return graphFactory.getNoTx();
    }
    public TransactionalGraph getGraphTx() {

        return this.getGraphFactory().getTx();
    }

}
