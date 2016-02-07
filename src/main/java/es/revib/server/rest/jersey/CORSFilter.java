package es.revib.server.rest.jersey;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

@PreMatching
@Provider
public class CORSFilter implements ContainerResponseFilter {

    @Context
    HttpServletRequest httpRequest;

    @Override
    public void filter(ContainerRequestContext creq, ContainerResponseContext cres) {

        cres.getHeaders().add("Access-Control-Allow-Origin",httpRequest.getHeader("origin"));
        cres.getHeaders().add("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        cres.getHeaders().add("Access-Control-Allow-Credentials", "true");
        cres.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        cres.getHeaders().add("Access-Control-Max-Age", "1209600");
        //cres.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");

    }

}