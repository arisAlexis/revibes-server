package mocks;

import es.revib.server.rest.broker.IBroker;
import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.messaging.Message;
import org.jvnet.hk2.annotations.Service;

@Service
public class DummyBroker implements IBroker {

    @Override
    public void sendMessage(User user, Message message) {

    }

    @Override
    public void close() {

    }

    @Override
    public void sendNotification(User user, Info notification) {

    }

    @Override
    public void sendStream(User user, Info stream) {

    }
}