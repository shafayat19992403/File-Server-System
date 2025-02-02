import java.io.Serializable;

public class AckObject implements Serializable {
    Boolean ack;
    Boolean isCompleted;

    public AckObject(Boolean ack,Boolean isCompleted){
        this.ack=ack;
        this.isCompleted=isCompleted;
    }

}
