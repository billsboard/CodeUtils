
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.User;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.util.Snowflake;

import java.io.FileNotFoundException;
import java.io.FileReader;
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

        client.login().block();
    }
}
