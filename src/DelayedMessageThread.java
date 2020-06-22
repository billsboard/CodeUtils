import discord4j.core.object.entity.MessageChannel;

public class DelayedMessageThread extends Thread{
    MessageChannel c;
    String m;
    long t;

    DelayedMessageThread(MessageChannel channel, String message, long waitInMills){
        c =channel;
        m = message;
        t = waitInMills;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(t);
        } catch (InterruptedException ignored) {
        }

        BotUtils.sendMessage(c, m);

    }
}
