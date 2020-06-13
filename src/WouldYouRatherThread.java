import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WouldYouRatherThread extends Thread{
       HashMap<String, Integer> reactionList;
       Message m;
       long sendid;

       public WouldYouRatherThread(HashMap<String, Integer> reactionList, Message message, long senderId){
           this.reactionList = reactionList;
           m = message;
           sendid = senderId;
       }

        @Override
        public void run() {
            ProcessorThread p = new ProcessorThread(reactionList, m, sendid);
            p.start();
            try {
                p.join(TimeUnit.MINUTES.toMillis(10));
                if(p.isAlive()){
                    p.interrupt();
                    Data.runningPolls.remove(m.getChannel().block().getId().asLong());
                    BotUtils.sendMessage(m.getChannel().block(), "Active WYR has expired!");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        class ProcessorThread extends Thread {
            HashMap<String, Integer> reactionList;
            ArrayList<Long> reacted = new ArrayList<>();
            Message m;
            long id;
            MessageChannel c;
            long sendid;

            int totalResponses = 0;

            ProcessorThread(HashMap<String, Integer> reactionList, Message message, long senderid){
                this.reactionList = reactionList;
                m = message;
                c = message.getChannel().block();
                id = c.getId().asLong();
                sendid = senderid;
            }

            public void run(){

                Message lastMsg = null;

                while (true){
                    Message m1 = Data.messageQueue.element();
                    if(m1.getChannel().block().getId().asLong() == id && lastMsg != m1){
                        if(m1.getAuthor().get().getId().asLong() == sendid && m1.getContent().get().trim().equalsIgnoreCase("^endwyr")){
                            Consumer<EmbedCreateSpec> spec = e -> {
                                StringBuilder s = new StringBuilder();
                                s.append("1) ");
                                s.append(String.format("%.0f", 100.0f * reactionList.get("1") / totalResponses));
                                s.append("%\n2) ");
                                s.append(String.format("%.0f", 100.0f * reactionList.get("2") / totalResponses));
                                s.append("%\n\u200b\nTotal responses: ");
                                s.append(totalResponses);

                                e.addField("Results", s.toString(), false);
                            };
                            BotUtils.sendMessage(c, spec);
                            Data.runningPolls.remove(id);
                            break;
                        }
                        else if(reactionList.containsKey(m1.getContent().get().trim().toLowerCase())){

                            if(reacted.contains(m1.getAuthor().get().getId().asLong())){
                                BotUtils.sendMessage(c, "Do not vote multiple times!");
                            }
                            else{
                                reacted.add(m1.getAuthor().get().getId().asLong());
                                String key = m1.getContent().get().toLowerCase();
                                reactionList.put(key, reactionList.get(key) + 1);
                                totalResponses++;

                                float percent = Float.parseFloat(String.format("%.0f", reactionList.get(key) / (float) totalResponses * 100));

                                Consumer<EmbedCreateSpec> spec = e -> {
                                    e.setDescription( percent > 50 ? "You voted with the majority, along with " + percent + "% of the participants" :
                                            "You voted against the majority, along with " + percent + "% of the participants");
                                    e.setFooter("Total votes: " + totalResponses, "");
                                };
                                BotUtils.sendMessage(c, spec);
                            }


                        }
                        lastMsg = m1;
                    }
                }
            }
        }
}
