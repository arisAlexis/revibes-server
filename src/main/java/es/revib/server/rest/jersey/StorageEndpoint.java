package es.revib.server.rest.jersey;

import es.revib.server.rest.storage.IStorageService;
import es.revib.server.rest.util.Globals;
import org.apache.tika.Tika;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("storage")
public class StorageEndpoint {

    @Inject
    IStorageService storageService;

    @Path("/upload")
    @POST
    @Restricted
    public Response uploadFile(
            @FormDataParam("file") InputStream fileInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDisposition)
            throws FileNotFoundException, IOException {

        String originalFilename = fileDisposition.getFileName();

        // save the file to the server
        String filename = UUID.randomUUID().toString();
        String[] tmp=originalFilename.split("\\.");
        //keep the original extension since its an approved file type
        filename+="."+tmp[tmp.length-1];

        String filePath=Globals.TMP_DIR+"/"+filename;

        try (OutputStream fileOutputStream = new FileOutputStream(filePath)) {
            int read = 0;
            final byte[] bytes = new byte[1024];
            while ((read = fileInputStream.read(bytes)) != -1) {
                fileOutputStream.write(bytes, 0, read);
            }
        }

        File tmpFile=new File(filePath);

        Tika tika = new Tika();
        String mimeType = tika.detect(tmpFile);

        //security check
        List<String> allowedTypes = new ArrayList<>();
        allowedTypes.add("image/jpeg");
        allowedTypes.add("image/png");
        allowedTypes.add("image/gif");

        if (mimeType == null || !allowedTypes.contains(mimeType)) return Response.status(Response.Status.BAD_REQUEST).build();

        if (storageService.store(filename,tmpFile)) {
            tmpFile.delete(); //we don't need this anymore
            JSONObject jsonObject=new JSONObject();
            try {
                jsonObject.put("url",storageService.urlize(filename));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return Response.ok(jsonObject.toString()).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Restricted
    @Path("/delete")
    public Response delete(@QueryParam("filename") String filename) {

        if (!storageService.delete(filename)) return Response.serverError().build();
        else return Response.ok().build();
    }

}
