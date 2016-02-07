package es.revib.server.rest.storage;

import java.io.File;

public interface IStorageService {

    /**
     * this function returns the filename prefixes with the full url that you can access it
     * @param filename
     * @return
     */
    public String urlize(String filename);

    /**
     * extract the filename from the full url
     * @param url
     * @return
     */
    public String deUrlize(String url);
    public boolean delete(String filename);
    public boolean store(String filename,File file);
    public File get(String file);

}
