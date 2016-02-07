package es.revib.server.rest.broker;

import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.messaging.Message;
import org.jvnet.hk2.annotations.Contract;

/**
 * generic interface that implements a broker system that routes/delivers messages to consumers for notifications, chat etc
 *
 */
@Contract
public interface IBroker {

    void sendMessage(User user,Message message);
    void close();
    void sendNotification(User user,Info notification);
    void sendStream(User user,Info stream);

}
