public class FileObject {
    String fileId;
    String fileName;
    String type;
    String owner;

    public FileObject(String fileId,String owner, String fileName,String type){
        this.fileId=fileId;
        this.fileName=fileName;
        this.type=type;
        this.owner=owner;
    }
}
