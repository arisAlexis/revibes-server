package config;

import es.revib.server.rest.approval.DummyApprovalService;
import es.revib.server.rest.approval.IApprovalService;
import es.revib.server.rest.auth.LogonService;
import es.revib.server.rest.broker.*;
import es.revib.server.rest.dao.*;
import es.revib.server.rest.email.IEmailService;
import es.revib.server.rest.email.SESEmailService;
import es.revib.server.rest.kv.IKVStore;
import es.revib.server.rest.kv.OrientKVStore;
import es.revib.server.rest.messaging.IMessageDAO;
import es.revib.server.rest.messaging.OrientMessageDAO;
import es.revib.server.rest.storage.IStorageService;
import es.revib.server.rest.storage.S3StorageService;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.util.logging.Logger;

public class MyTestApplication extends ResourceConfig {


    public MyTestApplication() {

        packages("es.revib.server.rest.jersey");

        // Enable LoggingFilter & output entity.
        registerInstances(new LoggingFilter(Logger.getLogger(MyTestApplication.class.getName()), true));

        //this catches weird exceptions that are swallowed by jersey, usually moxy and json objects
        //register(MyExceptionMapper.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(LogonService.class).to(LogonService.class);
                bind(BrokerService.class).to(BrokerService.class);
                bind(SocketIOBroker.class).to(IBroker.class);
                bind(S3StorageService.class).to(IStorageService.class);
                bind(DummyApprovalService.class).to(IApprovalService.class);
                bind(BrokerService.class).to(IBroker.class);
                bind(OrientActivityDAO.class).to(IActivityDAO.class);
                bind(OrientUserDAO.class).to(IUserDAO.class);
                bind(OrientSearchDAO.class).to(ISearchDAO.class);
                bind(OrientMessageDAO.class).to(IMessageDAO.class);
                bind(OrientORM.class).to(IORM.class);
                bind(OrientStreamDAO.class).to(IStreamDAO.class);
                bind(OrientKVStore.class).to(IKVStore.class);
                bind(SESEmailService.class).to(IEmailService.class);
            }
        });

        register(MultiPartFeature.class);
    }
}