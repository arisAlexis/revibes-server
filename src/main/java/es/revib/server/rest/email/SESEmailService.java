package es.revib.server.rest.email;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.*;
import es.revib.server.rest.util.Globals;
import org.jvnet.hk2.annotations.Service;

@Service
public class SESEmailService implements IEmailService{


    @Override
    public void sendMail(String from, String to,String subject, String body) {

        // Construct an object to contain the recipient address.
        Destination destination = new Destination().withToAddresses(new String[]{to});

        // Create the subject and body of the message.
        Content subjectObject = new Content().withData(subject);
        Content textBody = new Content().withData(body);
        Body bodyObject = new Body().withText(textBody);

        // Create a message with the specified subject and body.
        Message message = new Message().withSubject(subjectObject).withBody(bodyObject);

        // Assemble the email.
        SendEmailRequest request = new SendEmailRequest().withSource(from).withDestination(destination).withMessage(message);

        AWSCredentials awsCredentials = new BasicAWSCredentials(Globals.AWS_ACCESS_KEY,Globals.AWS_SECRET_KEY);
        AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient(awsCredentials);
        Region REGION = Region.getRegion(Regions.US_WEST_2);
        client.setRegion(REGION);
        client.sendEmail(request);

    }
}
