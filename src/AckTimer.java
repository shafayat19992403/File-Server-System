import java.util.Timer;
import java.util.TimerTask;
public class AckTimer {
    private Timer timer;
    private boolean isAck;

    private NetworkConnection networkConnection;

    public void startTimer(NetworkConnection networkConnection){
        timer = new Timer();
        isAck=false;
        this.networkConnection=networkConnection;
        timer.schedule(new AckTask(),3000);
    }

    public void ackReceived(){
        isAck=true;
        timer.cancel();
    }


    public void handleError(){
        networkConnection.write(new AckObject(false,false));

    }

    private class AckTask extends TimerTask {
        @Override
        public void run() {
            if (!isAck) {
                handleError();
            }
        }
    }
}
