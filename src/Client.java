import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Client {

    public static void printFunctions(){
        System.out.println("List of Functions:");
        System.out.println("1.Client List ");
        System.out.println("2.My Files");
        System.out.println("3.Others' Files");
        System.out.println("4.Upload");
        System.out.println("5.Download");
        System.out.println("6.Inbox");
        System.out.println("7.Make A Request");
        System.out.println("8.Exit");
        System.out.println("ENTER A FUNCTION:");
    }

    public static String getFunction(String option){
        if(option.equalsIgnoreCase("1")){
            return "clientList";
        }else if(option.equalsIgnoreCase("2")){
            return "myFiles";
        }else if(option.equalsIgnoreCase("3")){
            return "othersFile";
        }else if(option.equalsIgnoreCase("4")){
            return "upload";
        }else if(option.equalsIgnoreCase("5")){
            return "download";
        }else if(option.equalsIgnoreCase("6")){
            return "inbox";
        }else if(option.equalsIgnoreCase("7")){
            return "makearequest";
        }
        else{
            return "exit";
        }
    }
    public static void interact(String username,Socket socket,NetworkConnection networkConnection,Lock lock,HashMap<String,String> inboxMsgList) throws Exception{
        Character ch = 'a';
        while(true){

            ObjectData objectData=new ObjectData();
            printFunctions();
            Scanner in=new Scanner(System.in);
            String option=in.nextLine();
            option= getFunction(option);

            objectData.function=option;
            objectData.from=username;
            objectData.to="Server";
            objectData.msg="nothing";

            if(option.equalsIgnoreCase("clientList")){
              // objectData.print();

               networkConnection.write(objectData);
               System.out.println("Requesting for Client List....");
               objectData = (ObjectData) networkConnection.read();
               System.out.println(objectData.msg);
            }else if(option.equalsIgnoreCase("exit")){
                //objectData.print();
                System.out.println("Closing the client....");
                networkConnection.write(objectData);
                networkConnection.ois.close();
                networkConnection.oos.close();
                networkConnection.socket.close();
                System.exit(0);
            }else if(option.equalsIgnoreCase("myFiles")){
                //objectData.print();
                System.out.println("Requesting for my files");
                //objectData.print();
                networkConnection.write(objectData);
                objectData = (ObjectData) networkConnection.read();
                System.out.println(objectData.msg);
            }else if(option.equalsIgnoreCase("othersfile")){
                System.out.println("Whose files?");
                objectData.msg=in.nextLine();
                System.out.println("Requesting for "+objectData.msg+" files...");
                networkConnection.write(objectData);
                objectData=(ObjectData) networkConnection.read();
                System.out.println(objectData.msg);
            }else if(option.equalsIgnoreCase("upload")) {
                System.out.println("Enter directory: ");
                String source=in.nextLine();
                File fileToBeUploaded=new File(source);

                while(!fileToBeUploaded.exists()){
                    System.out.println("Enter directory :");
                    source=in.nextLine();
                    fileToBeUploaded=new File(source);
                }

                System.out.println("In response to a file request?");
                System.out.println("1.Yes");
                System.out.println("2.No");
                String type;
                String reqId="0";
                String input=in.nextLine();
                if(input.equalsIgnoreCase("1")){
                    System.out.println("Enter request ID : ");
                    reqId=in.nextLine();
                    while(inboxMsgList.get(reqId)==null){
                        System.out.println("Enter request ID : ");
                        reqId=in.nextLine();
                    };
                    type="public";
                }else {
                    System.out.println("File type?");
                    System.out.println("1.public");
                    System.out.println("2.private");
                    type = in.nextLine();
                    if (type.equalsIgnoreCase("1")) {
                        type = "public";
                    } else {
                        type = "private";
                    }
                }
                String fileName=new String(fileToBeUploaded.getName());
                String fileSize=new String(String.valueOf(fileToBeUploaded.length()));

                objectData.fileName=fileName;
                objectData.fileSize=fileSize;
                objectData.type=type;
                objectData.reqId=reqId;

                networkConnection.write(objectData);
                objectData=(ObjectData) networkConnection.read();

                if(objectData.msg.toLowerCase().equalsIgnoreCase("overflow")){
                    System.out.println("No Space in the buffer");
                }else {
                    System.out.println("Starting upload....");
                    Integer chunkSize = Integer.valueOf(objectData.chunkSize);
                    Integer fileId = Integer.valueOf(objectData.fileId);

                    FileInputStream fileInputStream = new FileInputStream(source);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

                    System.out.println(chunkSize);
                    Queue<byte[]> bufferedChunks = new ArrayDeque<>();
                    Queue<Integer> bytesRead = new ArrayDeque<>();

                    byte[] buffer = new byte[chunkSize];
                    int temp;
                    final boolean[] timeOut = {false};

                    while (true) {
                        if (!((temp = bufferedInputStream.read(buffer)) != -1)) {
                            bufferedChunks.add(buffer);
                            System.out.println(temp);
                            bytesRead.add(temp);
                            break;
                        }
                        System.out.println(temp);
                        bytesRead.add(temp);
                        bufferedChunks.add(buffer);
                        buffer = new byte[chunkSize];
                    }
                    System.out.println("File spilited into chunks...");


                    while (bufferedChunks.size() > 0) {
                        lock.lock();
                        buffer = bufferedChunks.poll();
                        temp = bytesRead.poll();
                        System.out.println(temp);
                        if (temp != -1) {
                            networkConnection.write(new ChunkObject(buffer, "Not Last",temp));

                            //System.out.println("Writing a chunk...");

                            /*AckTimer ackTimer=new AckTimer();
                            ackTimer.startTimer(networkConnection);*/
                            Timer timer = new Timer();

                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    networkConnection.write(new AckObject(false, false));
                                    timeOut[0] = true;
                                }
                            }, 30000);

                            AckObject ackObject = (AckObject) networkConnection.read();


                            if (ackObject.ack && timeOut[0] == false) {
                                //System.out.println("Acknowlegded...");
                                //ackTimer.ackReceived();
                                timer.cancel();
                                networkConnection.write(new AckObject(true, false));
                            }

                            if (timeOut[0] == true) {
                                break;
                            }

                        }
                        lock.unlock();
                    }

                    if (timeOut[0] == false) {
                        lock.lock();

                        networkConnection.write(new ChunkObject(buffer, "Last",temp));

                        AckObject ackObject = (AckObject) networkConnection.read();

                        if (ackObject.ack && ackObject.isCompleted) {
                            System.out.println("File uploaded successfully");
                        } else if(ackObject.ack && !ackObject.isCompleted ){
                            System.out.println("Errors in file uploading...(size misMatch)");
                        }
                        lock.unlock();

                    }else{
                        System.out.println("File uploading is canceled due to time-out..!!!!");
                    }
                }

            }else if(option.equalsIgnoreCase("download")){
                System.out.println("on construction ...");

                System.out.println("Whose File?");
                objectData.to=in.nextLine();
                System.out.println("Enter Directory :");
                objectData.msg=in.nextLine();

                System.out.println("Requesting for "+objectData.msg+" to start the download...");
                networkConnection.write(objectData);

                objectData=(ObjectData) networkConnection.read();
                if(objectData.msg.equalsIgnoreCase("file does not exist")) {
                    System.out.println(objectData.msg);
                }else if(objectData.msg.equalsIgnoreCase("private")){
                    System.out.println(objectData.msg+" files can not be downloaded");
                }else if(objectData.msg.equalsIgnoreCase("starting download")){

                    ArrayDeque<byte[]> bufferedChunks = new ArrayDeque<>();
                    Queue<Integer> bytesReadQ = new ArrayDeque<>();
                    byte[] buffer=new byte[Integer.parseInt(objectData.chunkSize)];
                    int bytesRead;

                    while(true){
                        ChunkObject chunkObject = (ChunkObject) networkConnection.read();

                        if(chunkObject.msg.equalsIgnoreCase("not last")){
                            lock.lock();
                            bufferedChunks.add(chunkObject.buffer);
                            bytesReadQ.add(chunkObject.chunkSize);
                            lock.unlock();
                        }else if(chunkObject.msg.equalsIgnoreCase("last")){
                            System.out.println("Received all the chunks ...");

                            String destination=new String(Server.baseDirectoryPath+"/ClientDir/"+username+"/"+objectData.fileName);
                            FileOutputStream fileOutputStream=new FileOutputStream(destination,true);
                            BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);

                            while(bufferedChunks.size()>0) {
                                buffer = bufferedChunks.poll();
                                bufferedOutputStream.write(buffer, 0, buffer.length - 1);

                                bufferedOutputStream.flush();
                            }
                            break;
                        }
                    }

                }

            }else if(option.equalsIgnoreCase("makearequest")){

                System.out.println("Write a Short Description :");
                objectData.msg=in.nextLine();
                //System.out.println("Requesting for "+objectData.msg+" files...");
                networkConnection.write(objectData);
                objectData=(ObjectData) networkConnection.read();
                System.out.println(objectData.msg);

            }else if(option.equalsIgnoreCase("inbox")){
                networkConnection.write(objectData);
                objectData=(ObjectData) networkConnection.read();
                //System.out.println(objectData.msg);
                System.out.println("Inbox :");
                for(int i=0;i<objectData.fileReqList.size();i++){
                    //System.out.println(objectData.fileReqList.get(i));
                    String[] info=objectData.fileReqList.get(i).split("_");
                    if(info.length==3) {
                        if (inboxMsgList.get(info[2]) == null) {
                            inboxMsgList.put(info[2], objectData.fileReqList.get(i));
                            System.out.println("Req Id. " + info[2] + " <---> " + info[1] + "  (" + info[0] + ")  [UNREAD]");
                        } else {
                            System.out.println(info[2] + "<--->" + info[1] + "  (" + info[0] + ")  [READ]");
                        }
                    }else if(info.length==2){
                        System.out.println("Notification : "+info[1]);
                    }

                }

                //System.out.println("Notifications :");
                /*for(Character i='a';i<='z';i++){
                    if(inboxMsgList.get(i)==null){
                        break;
                    }else{
                        String[] info= inboxMsgList.get(String.valueOf(i)).split("_");
                        System.out.println("Requested File uploaded. Req Id: "+info[2]);
                    }
                }*/



            }/*else if(option.equalsIgnoreCase("notify")){
                System.out.println("Notification Received ..... !!!!");
                String temp = objectData.msg;
                //String[] info = temp.split("_");
                //info[2] = ch++;
                //temp = new String(info[0]+"_"+info[1]+"_"+info[2]);
                inboxMsgList.put(String.valueOf(ch++),temp);
            }*/
        }
    }
    public static void main(String[] args) throws Exception {
        Lock lock=new ReentrantLock();
        String username;
        Socket socket;
        //ArrayList<String> inboxMsgList=new ArrayList<>();
        HashMap<String,String> inboxMsgList=new HashMap<>();


        System.out.println("Client Started....");
        socket=new Socket("127.0.0.1",12345);
        NetworkConnection networkConnection=new NetworkConnection(socket);

        System.out.println("Enter Your Username: ");
        Scanner in=new Scanner(System.in);
        username=in.nextLine();

        ObjectData dataPack=new ObjectData();
        dataPack.msg=username;

        networkConnection.write(dataPack);

        dataPack = (ObjectData) networkConnection.read();

        if(dataPack.function.equalsIgnoreCase("Already Logged in")){
            System.exit(0);
        }else if(dataPack.function.equalsIgnoreCase("logged in")){
            System.out.println("Logged in successfully");

            interact(username,socket,networkConnection,lock,inboxMsgList);
        }else {
            System.out.println("Something is wrong");
            System.exit(0);
        }
    }
}
