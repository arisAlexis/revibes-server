package es.revib.server.rest.util;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
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

/**
 * Convenience factory for getting httpclients
 */
public class HttpClientFactory {
    public static Client getSslClient() {
        Client client= ClientBuilder.newBuilder()
                .sslContext(getSSLContext())
                .hostnameVerifier((hostname1, sslSession) -> true)
                .register(JacksonFeature.class)
                .build();

        return client;
    }

    private static SSLContext getSSLContext() {

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
