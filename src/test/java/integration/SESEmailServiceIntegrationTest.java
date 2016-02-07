package integration;

import es.revib.server.rest.email.IEmailService;
import es.revib.server.rest.email.SESEmailService;
import es.revib.server.rest.util.Globals;
import org.junit.Test;

import java.util.UUID;

public class SESEmailServiceIntegrationTest {

    @Test
    public void sendEmailTest() {

        IEmailService emailService= new SESEmailService();
        String uniqueBody= UUID.randomUUID().toString();
        emailService.sendMail(Globals.NO_REPLY_EMAIL,"success@simulator.amazonses.com","test",uniqueBody);

    }
}
