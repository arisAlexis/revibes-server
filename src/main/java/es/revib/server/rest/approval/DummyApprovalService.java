package es.revib.server.rest.approval;

import es.revib.server.rest.util.Globals;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public class DummyApprovalService implements IApprovalService {

    public static int REJECT_ALL=0;
    public static int ACCEPT_ALL=1;

    public static int mode=ACCEPT_ALL;

    @Override
    public void sendForApproval(String key,String value) {

        //directly call the callback on our own endpoint!
        Client client = ClientBuilder.newBuilder()
                .sslContext(getSSLContext())
                .hostnameVerifier((hostname, sslSession) -> true).register(JacksonFeature.class).build();

        String status;
        if (mode==ACCEPT_ALL) status=IApprovalService.APPROVED;
        else status=IApprovalService.REJECTED;

        client.target(Globals.SERVER_URL+"/rest/approval/callback")
                .queryParam("key",key)
                .queryParam("status",status)
                .request().get();
    }

    private SSLContext getSSLContext() {

        SSLContext context= null;
        try {
            context = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            context.init(null,new TrustManager[]{new X509TrustManager(){
                public void checkClientTrusted(      X509Certificate[] x509Certificates,      String authType)  {
                }
                public void checkServerTrusted(      X509Certificate[] x509Certificates,      String authType) {

                }
                public X509Certificate[] getAcceptedIssuers(){
                    return new X509Certificate[0];
                }
            }
            },new SecureRandom());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }

        return  context;
    }

}
