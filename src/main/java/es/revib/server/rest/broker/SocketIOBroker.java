package es.revib.server.rest.broker;

import com.corundumstudio.socketio.SocketIOServer;
import es.revib.server.rest.entities.Info;
import es.revib.server.rest.entities.User;
import es.revib.server.rest.messaging.Message;

public class SocketIOBroker implements IBroker {

    SocketIOServer server=SocketIOServerImpl.getInstance().getServer();

    @Override
    public void sendMessage(User user, Message message) {
        server.getNamespace("exchange").getRoomOperations(user.getId()).sendEvent("message",message);
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
