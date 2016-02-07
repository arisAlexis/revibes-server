package es.revib.server.rest.storage;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import es.revib.server.rest.util.Globals;

import java.io.File;

public class S3StorageService implements IStorageService {

    /**
     * this function returns the filename prefixes with the full url that you can access it
     *
     * @param filename
     * @return
     */
    @Override
    public String urlize(String filename) {
        return "http://revib.es.s3.amazonaws.com/"+Globals.S3_FOLDER+"/"+filename;
    }

    /**
     * extract the filename from the full url
     *
     * @param url
     * @return
     */
    @Override
    public String deUrlize(String url) {
        String tmp[]=url.split("\\/");
        return tmp[tmp.length-1];
    }

    @Override
    public boolean delete(String filename) {
        try {
            AWSCredentials awsCredentials = new BasicAWSCredentials(Globals.AWS_ACCESS_KEY, Globals.AWS_SECRET_KEY);
            AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
            s3Client.deleteObject(new DeleteObjectRequest(Globals.S3_BUCKET, Globals.S3_FOLDER + "/" + filename));

        } catch (AmazonServiceException ase) {
            ase.printStackTrace();
            return false;
            //todo logging here

        } catch (AmazonClientException ace) {
            ace.printStackTrace();
            return false;
            //todo logging here
        }
        return true;
    }

    @Override
    public boolean store(String filename,File file) {
        try {
        AWSCredentials awsCredentials = new BasicAWSCredentials(Globals.AWS_ACCESS_KEY, Globals.AWS_SECRET_KEY);
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
        PutObjectResult putObjectResult =
                s3Client.putObject(new PutObjectRequest(Globals.S3_BUCKET, Globals.S3_FOLDER + "/" + filename, file)
                        .withCannedAcl(CannedAccessControlList.PublicRead));

    } catch (AmazonServiceException ase) {
        ase.printStackTrace();
        return false;
        //todo logging here

    } catch (AmazonClientException ace) {
        ace.printStackTrace();
            return false;

        //todo logging here
    }
        return true;
    }

    @Override
    public File get(String filename) {
        return null;
    }
}
