import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkConnection {
    public Socket socket;
    ObjectInputStream ois;
    ObjectOutputStream oos;

    public NetworkConnection(Socket socket) throws IOException{
        this.socket=socket;
        oos=new ObjectOutputStream(socket.getOutputStream());
        ois=new ObjectInputStream(socket.getInputStream());
    }

    public void write(Object obj){
        try{
            oos.writeObject(obj);
        }catch(IOException e){
            //System.out.println("Failed to write");
        }
    }

    public Object read(){
        Object obj;
        try{
            obj=ois.readObject();
        }catch(Exception ex){
            //System.out.println("Failed to read");
            return null;
        }
        return obj;
    }
}
