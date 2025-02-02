import java.io.Serializable;

public class ChunkObject implements Serializable {
    byte[] buffer;
    Integer chunkSize;

    String msg;

    public ChunkObject(byte[] buffer,String msg,Integer chunkSize){
        this.buffer=buffer;
        this.msg=msg;
        this.chunkSize=chunkSize;
    }
}
