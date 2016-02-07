package es.revib.server.rest.storage;

import java.io.File;

public class MongoStorageService implements IStorageService {

    /**
     * this function returns the filename prefixes with the full url that you can access it
     *
     * @param filename
     * @return
     */
    @Override
    public String urlize(String filename) {
        return null;
    }

    /**
     * extract the filename from the full url
     *
     * @param url
     * @return
     */
    @Override
    public String deUrlize(String url) {
        return null;
    }

    @Override
    public boolean delete(String filename) {
        return false;
    }

    @Override
    public boolean store(String filename, File file) {
        return false;
    }

    @Override
    public File get(String file) {
        return null;
    }
}
