package es.revib.server.rest.email;

import org.jvnet.hk2.annotations.Contract;

@Contract
public interface IEmailService {

    public void sendMail(String from,String to,String subject,String body);

}
