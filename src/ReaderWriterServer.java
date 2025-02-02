import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReaderWriterServer implements Runnable{
    HashMap<String,Information> clientList;
    NetworkConnection networkConnection;
    String username;
    HashMap<String,String> sessionList;
    HashMap<String,FileObject> fileList;
    HashMap<String,String> requestList;
    Lock lock;
    Queue<byte[]> bufferedChunks ;

    public ReaderWriterServer(HashMap<String,Information> clientList,NetworkConnection networkConnection,HashMap<String,String> sessionList,HashMap<String,FileObject> fileList , HashMap<String,String> requestList){
        this.clientList=clientList;
        this.networkConnection=networkConnection;
        username = "";
        this.sessionList = sessionList;
        this.fileList=fileList;
        lock=new ReentrantLock();
        bufferedChunks=new ArrayDeque<>();
        this.requestList=requestList;
    }
    public Integer generateChunkSize(){
        Random random=new Random();
        Integer randomNumber = random.nextInt(Server.MAX_CHUNK_SIZE - Server.MIN_CHUNK_SIZE + 1) + Server.MIN_CHUNK_SIZE;

        return randomNumber;
    }
    public void makeDirectory(){
        String directoryPath=Server.baseDirectoryPath+"/Files/"+username;

        File directory = new File(directoryPath);
        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Directory created successfully.");
            } else {
                System.out.println("Failed to create the directory.");
            }
        } else {
            System.out.println("Directory already exists.");
        }

        directoryPath=Server.baseDirectoryPath+"/ClientDir/"+username;
        directory=new File(directoryPath);

        if (!directory.exists()) {
            if (directory.mkdirs()) {
                System.out.println("Directory created successfully.");
            } else {
                System.out.println("Failed to create the directory.");
            }
        } else {
            System.out.println("Directory already exists.");
        }

    }
    public void writeClientDetailsInFile(String name){
        String directory=Server.baseDirectoryPath;
        directory=new String(directory+"/Resources/clientList.txt");
        name =new String(name+",");
        char buffer[] =new char[name.length()];
        name.getChars(0,name.length(),buffer,0);

        try {
            Writer f0 = new FileWriter( directory , true);
            f0.write(buffer);
            f0.close();
        }catch (Exception e){

        }
    }
    public void writeFileDetailsInFile(String name){
        String directory=Server.baseDirectoryPath;
        directory=new String(directory+"/Resources/filesInfo.txt");
        name =new String(name+",");
        char buffer[] =new char[name.length()];
        name.getChars(0,name.length(),buffer,0);

        try {
            Writer f0 = new FileWriter( directory , true);
            f0.write(buffer);
            f0.close();
        }catch (Exception e){

        }
    }
    public String perforLogIn(String username,Information information){
        try{
            if(clientList.containsKey(username)==false){
                clientList.put(username,information);
                writeClientDetailsInFile(username);
                System.out.println("ClientList Hashmap is updated "+clientList);
                return "okay";
            }else if(clientList.containsKey(username)){
                clientList.remove(username);
                clientList.put(username, information);
                return "okay";
            }
            return null;
        }catch (Exception e){

        }
        return null;
    }
    @Override
    public void run(){
        Object userObj=networkConnection.read();
        ObjectData dataPack=(ObjectData) userObj;

        username=dataPack.msg;

        System.out.println("User :"+username+" is connected....");
        Information info=new Information(username,networkConnection);

        String sessionState=perforLogIn(username,info);

        if(sessionState == null){
            ObjectData objectData=new ObjectData();
            objectData.msg="Problem ";
            objectData.from="Server";
            objectData.function="Problem";
            info.networkConnection.write(objectData);
        }else if(sessionList.containsKey(username)){
            ObjectData objectData=new ObjectData();
            objectData.msg="Already logged in";
            objectData.from="Server";
            objectData.function="Already logged in";
            info.networkConnection.write(objectData);
        }else{
            sessionList.put(username,username);
            System.out.println("Logged in ......");
            makeDirectory();
            ObjectData objectData=new ObjectData();
            objectData.msg="Logged in";
            objectData.from="Server";
            objectData.function="Logged in";
            info.networkConnection.write(objectData);

            try {
                serve();
            } catch (IOException e) {
                //throw new RuntimeException(e);
                sessionList.remove(username);
                System.out.println(username +"is disconnected...");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (NullPointerException e){
                sessionList.remove(username);
                System.out.println(username +" is disconnected...");
                bufferedChunks.clear();
                e.printStackTrace();
            }
        }
    }

    public void serve() throws IOException, InterruptedException {
        while(true){
            ObjectData objectData=new ObjectData();
            System.out.println("wating for command");
            objectData = (ObjectData) networkConnection.read();

            //if(objectData==null) continue;

            String msg=objectData.msg;
            String function=objectData.function;
            String from=objectData.from;
            String to = objectData.to;

            if(function.toLowerCase().equalsIgnoreCase("clientlist")){
                Information information=clientList.get(from);
                String msgToSend = new String("List of Cliens ... \n");

                int i=0;
                for(Map.Entry<String,Information> entry : clientList.entrySet()){
                    i++;
                    msgToSend = new String(msgToSend +i+". "+ entry.getKey()+"\n");
                }

                msgToSend= new String(msgToSend + "Online Clients :\n");

                i=0;
                for(Map.Entry<String,String> entry : sessionList.entrySet()){
                    i++;
                    msgToSend = new String(msgToSend+i+". "+entry.getKey()+ "\n");

                }
                ObjectData dataPackToSend = new ObjectData();
                dataPackToSend.msg=msgToSend;
                dataPackToSend.function="print only";
                dataPackToSend.from="Server";
                dataPackToSend.to=dataPackToSend.from;
                information.networkConnection.write(dataPackToSend);
            }else if(function.toLowerCase().equalsIgnoreCase("exit")){
                sessionList.remove(username);
            }else if(function.toLowerCase().equalsIgnoreCase("myfiles")){
                Information information=clientList.get(from);
                String msgToSend = new String("List of My Files : \n");

                String directoryName=Server.baseDirectoryPath+"/Files/"+from;

                File f = new File(directoryName);
                String s[]=f.list();


                //System.out.println(s.length);

                int count=0;

                for(int i=1;i<=s.length;i=i+1){
                    //System.out.println(i);
                    FileObject fileInfo=null;
                    //System.out.println(s[i-1]);
                    for(String key: fileList.keySet()){
                        FileObject temp=fileList.get(key);
                        //System.out.println(i);
                       // System.out.println(temp.fileId+" "+temp.fileName+" "+temp.owner+" "+temp.type);
                        if(temp!=null) {

                            if (temp.fileName.equalsIgnoreCase(s[i-1])) {
                                fileInfo = temp;
                            }
                        }
                    }
                    //String[] infos=fileInfo.split("_");
                    //String type

                    if(fileInfo!=null) {
                        System.out.println(i);
                        String str = new String(++count + ". " + s[i - 1] + " <------> " + fileInfo.type + "\n");
                        msgToSend = new String(msgToSend + str);
                    }
                }

                ObjectData dataPackToSend = new ObjectData();
                dataPackToSend.msg = msgToSend;
                dataPackToSend.function = "print only";
                dataPackToSend.from = "Server";
                dataPackToSend.to = from;

                information.networkConnection.write(dataPackToSend);
            }else if(function.toLowerCase().equalsIgnoreCase("othersfile")){
                Information information=clientList.get(from);
                String msgToSend;
                if(clientList.containsKey(msg)){
                    msgToSend = new String("List of "+msg+"s "+" Files : \n");

                    String directoryName=Server.baseDirectoryPath+"/Files/"+msg;

                    File f = new File(directoryName);
                    String s[]=f.list();

                    //System.out.println(s.length);

                    int count=0;
                    for(int i=1;i<=s.length;i++){
                        FileObject fileInfo=null;

                        for(String key : fileList.keySet()){
                            FileObject temp=fileList.get(key);
                            System.out.println(temp.fileId+" "+temp.fileName+" "+temp.owner+" "+temp.type);

                            if(temp!=null) {
                                if(temp.fileName.equalsIgnoreCase(s[i-1])){
                                    fileInfo = temp;
                                }
                            }
                        }

                        if(fileInfo!=null && fileInfo.type.equalsIgnoreCase("public")){
                            String str = new String(++count + ". " + s[i - 1] + " <------> " + fileInfo.type + "\n");
                            msgToSend = new String(msgToSend + str);
                        }
                    }
                }else{
                    msgToSend = new String("List of"+msg+"s "+" Files : \n");
                    msgToSend = new String(msgToSend + "No Files To Show \n");
                }
                ObjectData dataPackToSend = new ObjectData();
                dataPackToSend.msg=msgToSend;
                dataPackToSend.function="print only";
                dataPackToSend.from="Server";
                dataPackToSend.to=objectData.from;
                information.networkConnection.write(dataPackToSend);
            }else if(function.toLowerCase().equalsIgnoreCase("upload")) {
                Information information=clientList.get(from);
                String reqId= objectData.reqId;
                if(Integer.parseInt(objectData.fileSize) + Server.CURRENT_BUFFER_SIZE > Server.MAX_BUFFER_SIZE){
                    ObjectData dataPackToSend =new ObjectData();
                    dataPackToSend.msg="overflow";
                    dataPackToSend.function="print only";
                    dataPackToSend.from="Server";
                    dataPackToSend.to=objectData.from;
                    information.networkConnection.write(dataPackToSend);
                }else{
                    ObjectData dataPackToSend=new ObjectData();
                    dataPackToSend.msg="not overflow";
                    dataPackToSend.function="print only";
                    dataPackToSend.from="Server";
                    dataPackToSend.to=objectData.from;
                    dataPackToSend.fileId=String.valueOf(Server.fileIdInc++);
                    dataPackToSend.chunkSize=String.valueOf(generateChunkSize());
                    dataPackToSend.fileName=objectData.fileName;
                    dataPackToSend.fileSize= objectData.fileSize;
                    dataPackToSend.type= objectData.type;
                    information.networkConnection.write(dataPackToSend);



                    String destination=new String(Server.baseDirectoryPath+"/Files/"+username+"/"+dataPackToSend.fileName);




                    bufferedChunks = new ArrayDeque<>();
                    Queue<Integer> bytesReadQ = new ArrayDeque<>();
                    byte[] buffer=new byte[Integer.parseInt(dataPackToSend.chunkSize)];
                    int bytesRead;
                    boolean timeOut=false;
                    boolean sizeMismatch=false;

                    while(true){
                        ChunkObject chunkObject = (ChunkObject) networkConnection.read();

                        if(chunkObject.msg.toLowerCase().equalsIgnoreCase("not last")){
                            lock.lock();
                           /* bufferedOutputStream.write(chunkObject.buffer,0,chunkObject.buffer.length-1);

                            bufferedOutputStream.flush();

                            networkConnection.write(new AckObject(true,false));*/

                            bufferedChunks.add(chunkObject.buffer);
                            bytesReadQ.add(chunkObject.chunkSize);
                            //Thread.sleep(2500);

                            networkConnection.write(new AckObject(true,false));
                            AckObject ackObject = (AckObject) networkConnection.read();

                            if(ackObject.ack){
                                lock.unlock();
                                continue;
                            }else{
                                System.out.println("Connection Time-out > 30 secs");
                                timeOut = true;
                                bufferedChunks.clear();
                                lock.unlock();
                                break;
                            }



                        }else{
                            //this is after receiving the last chunk
                            lock.lock();

                            Integer size=0;
                            for(Integer t : bytesReadQ){
                                size+=t;
                            }

                            //intentionally mismatching size
                            //size++;


                            System.out.println(size +" "+objectData.fileSize);

                            if(size==Integer.parseInt(objectData.fileSize)) {
                                networkConnection.write(new AckObject(true, true));
                            }else{
                                networkConnection.write(new AckObject(true,false));
                                bufferedChunks.clear();
                                sizeMismatch=true;
                            }

                            lock.unlock();
                            break;

                        }
                    }

                    try {
                       // bufferedOutputStream.flush();
                        if(timeOut==true)
                            System.out.println("File could not be received successfully due to time-out.");
                        else if(sizeMismatch==true){
                            System.out.println("File could not be received successfully due to size mismatch.");
                        }
                        else {

                            FileOutputStream fileOutputStream=new FileOutputStream(destination,true);
                            BufferedOutputStream bufferedOutputStream=new BufferedOutputStream(fileOutputStream);

                            while(bufferedChunks.size()>0) {
                                buffer = bufferedChunks.poll();
                                bufferedOutputStream.write(buffer, 0, buffer.length - 1);

                                bufferedOutputStream.flush();
                            }

                            fileList.put(dataPackToSend.fileId,new FileObject(dataPackToSend.fileId, username, dataPackToSend.fileName,dataPackToSend.type));

                            writeFileDetailsInFile(new String(dataPackToSend.fileId+"_"+username+"_"+dataPackToSend.fileName+"_"+dataPackToSend.type));

                            System.out.println("file received successfully");

                            if(reqId!=null) {
                                /*String req = requestList.get(reqId);
                                String[] info = req.split("_");
                                Information informationTemp = clientList.get(info[0]);
                                ObjectData notifyObject = new ObjectData();
                                notifyObject.function = "notify";
                                notifyObject.from = "Server";
                                notifyObject.to = info[0];
                                notifyObject.msg = req;
                                notifyObject.print();
                                informationTemp.networkConnection.write(notifyObject);*/

                                requestList.put(String.valueOf(Server.notificationIdInc++), requestList.get(reqId)+"_"+username);
                            }


                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                }
            }else if(function.toLowerCase().equalsIgnoreCase("download")){

                String source=new String(Server.baseDirectoryPath+"/Files"+msg);
                File fileToBeDownloaded=new File(source);

                if(!fileToBeDownloaded.exists()){
                    ObjectData objectDataToSend=new ObjectData();
                    objectDataToSend.msg=new String("file does not exist");
                    objectDataToSend.function=new String("file does not exist");
                    networkConnection.write(objectDataToSend);
                }else{

                   // FileObject fileObject = fileList.get();
                   // System.out.println(fileObject.type);
                   // System.out.println(fileToBeDownloaded.getName());
                    FileObject fileInfo=null;

                    for(String key: fileList.keySet()){
                        FileObject temp=fileList.get(key);
                        if(temp!=null) {

                            if (temp.fileName.equalsIgnoreCase(fileToBeDownloaded.getName()) && temp.owner.equalsIgnoreCase(to)) {
                                fileInfo = temp;
                            }
                        }
                    }

                    System.out.println(fileInfo.type+" "+fileInfo.fileName);

                    if(fileInfo.type.equalsIgnoreCase("private")){
                        ObjectData objectDataToSend=new ObjectData();
                        objectDataToSend.msg="private";
                        objectDataToSend.function="private";
                        networkConnection.write(objectDataToSend);
                    }else{
                        ObjectData objectDataToSend=new ObjectData();
                        objectDataToSend.msg="starting download";
                        objectDataToSend.function="starting download";
                        objectDataToSend.chunkSize=String.valueOf(Server.MAX_CHUNK_SIZE);
                        objectDataToSend.fileName=fileToBeDownloaded.getName();
                        networkConnection.write(objectDataToSend);


                        FileInputStream fileInputStream = new FileInputStream(source);
                        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

                        bufferedChunks=new ArrayDeque<>();
                        Queue<Integer> bytesRead = new ArrayDeque<>();

                        byte[] buffer = new byte[Server.MAX_CHUNK_SIZE];
                        int temp;

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
                            buffer = new byte[Server.MAX_CHUNK_SIZE];
                        }
                        System.out.println("File spilited into chunks...");

                        while(bufferedChunks.size()>0){
                            lock.lock();
                            buffer = bufferedChunks.poll();
                            temp = bytesRead.poll();
                            System.out.println(temp);

                            if(temp!=-1){
                                networkConnection.write(new ChunkObject(buffer,"not last",temp));
                            }else{
                                networkConnection.write(new ChunkObject(buffer,"last",temp));
                            }
                            lock.unlock();
                        }

                        System.out.println("Successfully downloaded to client "+username);
                    }


                }
            }else if(function.toLowerCase().equalsIgnoreCase("makearequest")){
                Information information=clientList.get(from);
                requestList.put(String.valueOf(Server.requestIdInc),new String(from+"_"+msg+"_"+(Server.requestIdInc++)));
                System.out.println(requestList.get(String.valueOf(Server.requestIdInc-1)));

                ObjectData dataPackToSend = new ObjectData();
                dataPackToSend.msg="File Request sent successfully";
                dataPackToSend.function="print only";
                dataPackToSend.from="Server";
                dataPackToSend.to=objectData.from;
                information.networkConnection.write(dataPackToSend);
            }else if(function.toLowerCase().equalsIgnoreCase("inbox")){
                Information information=clientList.get(from);
               // String msgToSend=new String("Inbox: \n");
                ArrayList<String> fileReq=new ArrayList<>();

                int i=1;
                for(String key : requestList.keySet()){

                    String req = requestList.get(key);
                   /* String[] info = req.split("_");
                    msgToSend = new String(msgToSend+key+". "+info[1]+"  ("+info[0]+")\n");*/
                    if(Integer.parseInt(key) >= 1000){
                        String[] info=req.split("_");
                        req=new String("n_"+"File has been been uploaded by "+info[3]+" <---> Req Id. "+info[2]);
                        if(info[0].equalsIgnoreCase(from)){
                            fileReq.add(req);
                        }
                    }else{
                        fileReq.add(req);
                    }

                }

                ObjectData dataPackToSend = new ObjectData();
                //dataPackToSend.msg=msgToSend;
                dataPackToSend.function="print only";
                dataPackToSend.from = "Server";
                dataPackToSend.to = objectData.from;
                dataPackToSend.fileReqList = fileReq;
                information.networkConnection.write(dataPackToSend);
            }
        }
    }
}
