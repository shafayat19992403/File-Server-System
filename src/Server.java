import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

public class Server {
    public static Integer fileIdInc=1;
    public static Integer MAX_BUFFER_SIZE=1024*150000;
    public static Integer CURRENT_BUFFER_SIZE=0;
    public static Integer MIN_CHUNK_SIZE=4;
    public static Integer MAX_CHUNK_SIZE=100;
    //public static String baseDirectoryPath ="/Users/shafayat/Desktop/Tonmay/Intellij/OfflineSecondAttempt Copy";
    public static String baseDirectoryPath = System.getProperty("user.dir");

    public static  Integer requestIdInc=1;
    public static Integer notificationIdInc=10000;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket=new ServerSocket(12345);
        System.out.println("Server Started.....");
        System.out.println(InetAddress.getLocalHost());
        HashMap<String,Information> clientList=new HashMap<>();
        HashMap<String,String> sessionList=new HashMap<>();
        HashMap<String,FileObject> fileList=new HashMap<>();
        HashMap<String,String> requestList=new HashMap<>();

        //HashMap<String,FileObject> fileListByName=new HashMap<>();
        //String baseDirectoryPath = "/Users/shafayat/Desktop/Tonmay/Intellij/OfflineSecondAttempt";
        File f = new File(baseDirectoryPath + "/Resources/clientList.txt");
        Reader rd = new FileReader(f);
        char data[] = new char[(int) f.length()];
        rd.read(data);
        String listOfClient = new String(data);
        String[] clients = listOfClient.split(",");

        for(String client : clients){
            clientList.put(client,new Information(client,null));
        }

        f=new File(baseDirectoryPath+"/Resources/filesInfo.txt");
        rd=new FileReader(f);
        data=new char[(int) f.length()];
        rd.read(data);
        String listOfFiles= new String(data);
        String[] files = listOfFiles.split(",");

        for(String file : files){
            //System.out.println(file);
            String[] infos=file.split("_");
            //System.out.println(infos[0]+" "+infos[1]+" "+infos[2]+" "+infos[3]);
            System.out.println(infos[2]);
            fileList.put(String.valueOf(fileIdInc),new FileObject(String.valueOf(fileIdInc++),infos[1],infos[2],infos[3]));
        }

        while(true){
            Socket socket=serverSocket.accept();
            System.out.println("Socket Accepted");
            NetworkConnection networkConnection=new NetworkConnection(socket);

            new Thread(new ReaderWriterServer(clientList,networkConnection,sessionList,fileList,requestList)).start();
        }
    }
}
