
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Main {

    static DiscordClient client;
    static EventProcessor eventProcessor;

    public static void main(String[] args) throws FileNotFoundException {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application Terminating ...");
        }));

        Scanner scan = new Scanner(new FileReader("token.txt"));

        DiscordClientBuilder builder = DiscordClientBuilder.create(scan.nextLine());
        builder.setInitialPresence(Presence.online(Activity.playing("with wet noodles")));
        client = builder.build();

        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });

        new EventProcessor(client.getEventDispatcher().on(MessageCreateEvent.class));

        URL url;


        try {
            url = new URL("https://api.judge0.com/languages/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                inputLine = response.toString();


                JSONArray jsonArr = new JSONArray(inputLine);
                for (int i = 0; i < jsonArr.length(); i++) {
                    JSONObject o = jsonArr.getJSONObject(i);
                    Data.languageID.put(o.getString("name").split(" ")[0].toLowerCase(), o.getInt("id"));
                }

                System.out.println(Data.languageID);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        client.login().block();



    }
}
