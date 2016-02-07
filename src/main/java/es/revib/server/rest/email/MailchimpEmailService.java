package es.revib.server.rest.email;

import org.jvnet.hk2.annotations.Service;

@Service
public class MailchimpEmailService implements IEmailService {
    @Override
    public void sendMail(String from, String to, String subject, String body) {

    }
}
