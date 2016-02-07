package es.revib.server.rest.jersey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
@Restricted
public class RestrictedFilter implements ContainerRequestFilter {

    //get these two from servlet container so we have sessions in them
    @Context
    HttpServletRequest httpRequest;
    @Context
    HttpServletResponse httpResponse;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws IOException {

        if (httpRequest.isRequestedSessionIdValid()) {
            return;
        }
        else {
            HttpSession session = httpRequest.getSession(false);
            if (session != null) session.invalidate();
                containerRequestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
        }
    }
}