import java.io.Serializable;
import java.util.ArrayList;

public class ObjectData implements Serializable {
    public String msg;

    public String from;
    public String to;

    public String function;

    public String fileName;
    public String fileSize;

    public String chunkSize;
    public String fileId;
    public String type;

    public String reqId;

    public ArrayList<String> fileReqList=new ArrayList<>();

    public void print(){
        System.out.println("msg :"+msg);
        System.out.println("From :"+from);
        System.out.println("To :"+to);
        System.out.println("Function :"+function);
        System.out.println("Filename: "+fileName);
        System.out.println("FileSize: "+fileSize);
        System.out.println("Chunk Size "+chunkSize);
        System.out.println("FIle Id :"+fileId);
    }
}
