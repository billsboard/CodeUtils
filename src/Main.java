
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileReader;
import java.util.Scanner;

public class Main {

    static DiscordClient client;

    public static void main(String[] args) throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Application Terminating ...");
        }));

        Scanner scan = new Scanner(new FileReader("token.txt"));
        DiscordClientBuilder builder = DiscordClientBuilder.create(scan.nextLine());
        builder.setInitialPresence(Presence.online(Activity.playing("with wet noodles")));
        client = builder.build();
        scan.close();


        client.getEventDispatcher().on(ReadyEvent.class)
                .subscribe(event -> {
                    User self = event.getSelf();
                    System.out.println(String.format("Logged in as %s#%s", self.getUsername(), self.getDiscriminator()));
                });

        new EventProcessor(client.getEventDispatcher().on(MessageCreateEvent.class));


        Data.initDataParams();
        scan = new Scanner(new FileReader("keys.txt"));

        while (scan.hasNextLine()){
            String[] s = scan.nextLine().split(" ");
            Data.apiKeys.put(s[0], s[1]);
        }


        System.out.println("Submitting language request to Judge0");

        OkHttpClient cl = new OkHttpClient();
        Request req = new Request.Builder()
                .url("https://judge0.p.rapidapi.com/languages")
                .get()
                .addHeader("x-rapidapi-host", "judge0.p.rapidapi.com")
                .addHeader("x-rapidapi-key", Data.apiKeys.get("judge0"))
                .build();

        Response response = cl.newCall(req).execute();

        if(response.isSuccessful()){
            JSONArray jsonArr = new JSONArray(response.body().string());
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject o = jsonArr.getJSONObject(i);
                Data.languageID.put(o.getString("name").split(" ")[0].toLowerCase(), o.getInt("id"));
            }

            System.out.println(Data.languageID);
        }







        client.login().block();



    }
}
