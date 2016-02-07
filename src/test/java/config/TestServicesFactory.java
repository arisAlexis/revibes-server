package config;

import es.revib.server.rest.dao.BaseGraphHelper;
import es.revib.server.rest.dao.IORM;
import es.revib.server.rest.dao.OrientGraphHelper;
import es.revib.server.rest.dao.OrientORM;
import es.revib.server.rest.database.OrientDatabase;

/**
 * A factory to create the tests instead of using DI which is an overkill just for tests
 */
public class TestServicesFactory {

    public static IORM getORM() { return new OrientORM();}
    public static BaseGraphHelper getGraphHelper() { return new OrientGraphHelper(OrientDatabase.getInstance().getGraphTx());}
}
