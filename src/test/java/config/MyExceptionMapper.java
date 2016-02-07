package config;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * This class catches exceptions that are sometimes eaten up by moxy/jersey and invisible
 */
@Provider
public class MyExceptionMapper implements
        ExceptionMapper<WebApplicationException> {
    @Override
    public Response toResponse(WebApplicationException ex) {
        ex.printStackTrace();
        return Response.status(500).entity(ex.toString()).type("text/plain")
                .build();
    }
}
