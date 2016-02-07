package es.revib.server.rest.approval;

public interface IApprovalService {

    public static String APPROVED="APPROVED";
    public static String REJECTED="REJECTED";
    public static String TYPE_TEXT="TEXT";
    public static String TYPE_FILE="FILE";
    public static String TYPE_LINK="LINK";

    /**
     *
     * @param value can be either a URL or text
     */
    public void sendForApproval(String key,String value);


}
