package es.revib.server.rest.dao;

import es.revib.server.rest.entities.Comment;
import es.revib.server.rest.entities.Info;
import org.jvnet.hk2.annotations.Contract;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;

@Contract
public interface IStreamDAO{

    void stream(@Valid Info stream, List users);

    Set<Info> getByUser(Object user, long timestamp, int size,Double lat,Double lon,Integer radius);

    //sends a like to either a comment or a
    void like(Object user, String streamId);
    Info getStream(Object user, String id);
    Comment newComment(Object user, Comment comment);
    void revibe(Object user,String streamId);

}
