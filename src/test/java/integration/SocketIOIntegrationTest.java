package integration;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import es.revib.server.rest.broker.SocketIOServerImpl;
import integration.IntegrationTest;
import org.junit.*;

import java.net.URISyntaxException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SocketIOIntegrationTest extends IntegrationTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        customInit();
    }
    @AfterClass
    public static void afterClass() throws Exception {
        customShutdown();
    }

    @Before
    public void before() throws Exception {
        setUp();
    }
    @After
    public void after() throws Exception {
        tearDown();
    }

    @Test
    public void connectWithAuth() throws URISyntaxException, InterruptedException {

        IO.Options opts = new IO.Options();
        opts.forceNew = true;
        opts.query = "user=testuser1@test.com&password=wrong";
        Socket socket = IO.socket("https://localhost:8444");
        socket.connect();
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {

                System.out.println("connected to local server");
            }

        });
        assertFalse(socket.connected());

        opts.query = "user=testuser1@test.com&password=ioioio";
        socket = IO.socket("https://localhost:8444/exchange",opts);

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {

            @Override
            public void call(Object... args) {

                System.out.println("connected to local server");
            }

        }).on("event", new Emitter.Listener() {

            @Override
            public void call(Object... args) {

            }

        });

        socket.connect();
        assertTrue(socket.connected());

    }

}