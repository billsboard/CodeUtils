import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.data.stored.ChannelBean;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import com.sun.management.OperatingSystemMXBean;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

class EventProcessor {


    PrintStream logSteam = null;
    PrintStream commandLogStream = null;
    SimpleDateFormat sdf = new SimpleDateFormat("YYYY-MM-DD hh:mm:ss");


    EventProcessor(Flux<MessageCreateEvent> on) {

        File commandLog = new File("commandLog.txt");
        try {
            commandLogStream = new PrintStream(commandLog);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        on.subscribe(messageCreateEvent -> {
            try {
                onMessageReceived(messageCreateEvent.getMessage());
            } catch (Exception e) {
                System.out.println(e.getClass().getSimpleName());
                e.printStackTrace();
            }
        });
    }

    private void onMessageReceived(Message message) {

        if (!message.getContent().isPresent()) return;

        String body = message.getContent().get();
        MessageChannel channel = message.getChannel().block();
        Guild guild = message.getGuild().block();

        String[] lowerArgs = body.toLowerCase().split(" ");
        String[] rawArgs = body.split(" ");

        discord4j.core.object.entity.User sender = message.getAuthor().get();

        if(!lowerArgs[0].startsWith(BotUtils.BOT_PREFIX)) return;
        else if(sender.isBot()) return;

        if(channel == null) return;

        commandLogStream.println(sender.getUsername() + ": " + body);

        switch (lowerArgs[0].substring(1)){
            case "pfact": case "primefact": case "primefactor": {
                if(lowerArgs.length < 2 || !BotUtils.isPositiveInteger(lowerArgs[1])){
                    BotUtils.sendArgumentsError(channel, "pfact", "whole number");
                    break;
                }

                HashMap<Integer, Integer> factors = BotUtils.primeFactor(Integer.parseInt(lowerArgs[1]));

                if(factors.isEmpty()){
                    BotUtils.sendMessage(channel,"No prime factorization exists");
                }
                else {
                    String out = "Prime factors:";
                    for (Integer i : factors.keySet()) {
                        out += " " + i + (factors.get(i) == 1 ? "" : "^" + factors.get(i)) + " *";
                    }
                    BotUtils.sendMessage(channel, out.substring(0, out.length() - 2));
                }
                break;
            }
            case "calc": case "calculate":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "calc", "math expression");
                    break;
                }

                try{
                    String data = body.toLowerCase().replaceFirst(Pattern.quote(rawArgs[0]), "");
                    data = data.replaceAll("pi", Double.toString(Math.PI))
                            .replaceAll("e", Double.toString(Math.E))
                            .replaceAll("tau", Double.toString(2*Math.PI));
                    BotUtils.sendMessage(channel, Double.toString(BotUtils.evalMath(data)));
                } catch (RuntimeException e){
                    BotUtils.sendMessage(channel, e.getMessage());
                }
                break;
            }
            case "round": case "rnd":{
                if(lowerArgs.length < 2 || !BotUtils.isNumeric(lowerArgs[1])){
                    BotUtils.sendArgumentsError(channel, "round", "number");
                }
                else{
                    BotUtils.sendMessage(channel, "Rounded value is: " + new BigDecimal(lowerArgs[1]).setScale(0, RoundingMode.HALF_UP));
                }
                break;
            }
            case "floor":{
                if (lowerArgs.length < 2 || !BotUtils.isNumeric(lowerArgs[1])) {
                    BotUtils.sendArgumentsError(channel, "floor", "number");
                } else {
                    BotUtils.sendMessage(channel, "Floored value is: " + new BigDecimal(lowerArgs[1]).setScale(0, RoundingMode.DOWN));
                }
                break;
            }
            case "ceiling": case "ceil":{
                if (lowerArgs.length < 2 || !BotUtils.isNumeric(lowerArgs[1])) {
                    BotUtils.sendArgumentsError(channel, "ceiling", "number");
                } else {
                    BotUtils.sendMessage(channel, "Rounded value is: " + new BigDecimal(lowerArgs[1]).setScale(0, RoundingMode.UP));
                }
                break;
            }
            case "const": case "constant":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "constant", "constant name");
                }
                else if(!Data.constants.containsKey(rawArgs[1])){
                    BotUtils.sendMessage(channel, "Constant " + rawArgs[1] + " is not known by the bot. Due to nature of mathematical constant names, the argument is case sensitive");
                }
                else{
                    BotUtils.sendMessage(channel, "Value of " + rawArgs[1] + " is\n```" + Data.constants.get(rawArgs[1]) + "```");
                }
                break;
            }
            case "length": case "len":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "len", "string");
                    break;
                }

                String data = BotUtils.removeCommand(body, rawArgs[0]);
                BotUtils.sendMessage(channel,  "Length of the unformatted input string is: " + data.length());
                break;
            }
            case "avatar": case "icon": case "pfp":{
                User userToUse;

                if(lowerArgs.length < 2){
                    userToUse = sender;
                }
                else if(!BotUtils.isPositiveInteger(BotUtils.getUserFromMention(lowerArgs[1]))){
                    BotUtils.sendArgumentsError(channel, "avatar", "optional @user");
                    break;
                }
                else{
                    User u = Main.client.getUserById(Snowflake.of(BotUtils.getUserFromMention(lowerArgs[1]))).block();
                    if(u == null){
                        BotUtils.sendMessage(channel, "User not found!");
                        break;
                    }

                    userToUse = u;
                }

                Consumer<EmbedCreateSpec> e = embed -> {
                    embed.setTitle("Profile picture for user " + userToUse.getUsername() + "#" + userToUse.getDiscriminator())
                            .setImage(userToUse.getAvatarUrl() + "?size=1024")
                            .setUrl(userToUse.getAvatarUrl() + "?size=1024");
                };
                BotUtils.sendMessage(channel, e);
                break;
            }
            case "squaretwitter": case "st":{
                String url = Data.stUrls[BotUtils.random.nextInt(Data.stUrls.length)];

                Consumer<EmbedCreateSpec> e = embed -> {
                    embed.setImage(url);
                };
                BotUtils.sendMessage(channel, e);
                break;
            }
            case "clear":{/*
                if(lowerArgs.length < 2 || !BotUtils.isPositiveInteger(lowerArgs[1]) || Integer.parseInt(lowerArgs[1]) < 1 || Integer.parseInt(lowerArgs[1]) > 50){
                    BotUtils.sendArgumentsError(channel,"clear", "0 < messages < 51");
                    break;
                }
                else if(!guild.getMemberById(sender.getId()).block().getHighestRole().block().getPermissions().contains(Permission.MANAGE_MESSAGES)){
                    BotUtils.sendMessage(channel, "You do not have the `MANAGE_MESSAGES` permission");
                    break;
                }
                else if(!guild.getMemberById(Main.client.getSelfId().get()).block().getHighestRole().block().getPermissions().contains(Permission.MANAGE_MESSAGES)){
                    BotUtils.sendMessage(channel, "Bot does not have the `MANAGE_MESSAGES` permission");
                    break;
                }
                else {
                    TextChannel textChannel = new TextChannel(channel.getClient().getServiceMediator(), new ChannelBean(channel.getId().asLong(), channel.getType().getValue()));
                    List<Message> mL = channel.getMessagesBefore(message.getId()).collectList().block().stream().limit(Integer.parseInt(lowerArgs[1])).collect(Collectors.toList());
                    System.out.println(mL);
                    List<Snowflake> sL = new ArrayList<>();
                    for (Message m : mL) {
                        System.out.println(message);
                        sL.add(m.getId());
                    }

                    Stream<Snowflake> stream = sL.stream();

                    Publisher<Snowflake> p = new Publisher<Snowflake>() {
                        @Override
                        public void subscribe(Subscriber<? super Snowflake> subscriber) {
                            stream.forEach(subscriber::onNext);
                            subscriber.onComplete();
                        }
                    };
                    textChannel.bulkDelete(p).blockFirst();
                }*/
                break;
            }
            case "convertbase": case "base":{
                if(lowerArgs.length < 4 || !BotUtils.isPositiveInteger(lowerArgs[1]) || !BotUtils.isPositiveInteger(lowerArgs[2]) || !BotUtils.isAlphaNumeric(lowerArgs[3])){
                    BotUtils.sendArgumentsError(channel, "base", "1 < origin base < 37", "1 < target base < 37", "number");
                    break;
                }
                else if(Integer.parseInt(lowerArgs[1]) < 2 || Integer.parseInt(lowerArgs[2]) < 2 || Integer.parseInt(lowerArgs[1]) > 36 || Integer.parseInt(lowerArgs[2]) > 36){
                    BotUtils.sendArgumentsError(channel, "base", "1 < origin base < 37", "1 < target base < 37", "number");
                    break;
                }

                int a = Integer.parseInt(lowerArgs[1]), b = Integer.parseInt(lowerArgs[2]);
                String x = lowerArgs[3];
                String[] digits = new StringBuilder(x.replaceAll("-", "")).reverse().toString().split("");
                for (int i = 0; i < digits.length; i++) {
                    if(digits[i].matches("[a-z]")) {
                        digits[i] = Integer.toString(digits[i].toCharArray()[0] - 87);
                        if(Integer.parseInt(digits[i]) >= a) {
                            BotUtils.sendMessage(channel, "Invalid digit `" + (digits[i].toCharArray()[0] + 87) + "` for base `" + a + "`");
                            return;
                        }
                    }
                    else{
                        if(Integer.parseInt(digits[i]) >= a){
                            BotUtils.sendMessage(channel, "Invalid digit `" + digits[i] + "` for base `" + a + "`");
                            return;
                        }
                    }
                }

                BigInteger tempValue = new BigInteger("0");
                for (int i = 0; i < digits.length; i++) {
                    tempValue = tempValue.add(new BigInteger(digits[i]).multiply(new BigInteger(Integer.toString(a)).pow(i)));

                }

                StringBuilder result = new StringBuilder();
                while (tempValue.intValue() != 0){
                    int val = tempValue.mod(new BigInteger(Integer.toString(b))).intValue();
                    if(val >= 10) result.append((char) (val + 87));
                    else result.append(val);
                    tempValue = tempValue.divide(new BigInteger(Integer.toString(b)));
                }

                String out = lowerArgs[3].contains("-") ? "-" + result.reverse() : result.reverse().toString();

                BotUtils.sendMessage(channel, "`" + lowerArgs[3] + "` in base `" + a + "` converted to base `" + b + "` is\n```" + out + "```");
                break;
            }
            case "ascii":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "ascii", "string");
                    break;
                }
                else{
                    String out = "";
                    char[] chars = BotUtils.removeCommand(body, rawArgs[0]).toCharArray();

                    for (char c : chars) {
                        out += ((int) c) + " ";
                    }
                    BotUtils.sendMessage(channel, "\"" + BotUtils.removeCommand(body, rawArgs[0]) + "\" to ascii is\n```" + out.trim() + "```");
                }
                break;
            }
            case "time": case "gettime":{
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                if(lowerArgs.length < 2){
                    Calendar c = Calendar.getInstance();
                    c.setTimeZone(TimeZone.getTimeZone("UTC"));
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    BotUtils.sendMessage(channel, "UTC time is:\n```" + sdf.format(c.getTime()) + "```");
                }
                else{
                    if(!Arrays.stream(TimeZone.getAvailableIDs()).anyMatch(lowerArgs[1].toUpperCase()::equals)){
                        BotUtils.sendMessage(channel, lowerArgs[1].toUpperCase() + " is not a valid timezone identifier!");
                    }
                    else{
                        TimeZone timeZone = TimeZone.getTimeZone(lowerArgs[1].toUpperCase());
                        Calendar c = Calendar.getInstance();
                        c.setTimeZone(timeZone);
                        sdf.setTimeZone(timeZone);
                        BotUtils.sendMessage(channel, lowerArgs[1].toUpperCase() + " time is:\n```" + sdf.format(c.getTime()) + "```");
                    }
                }
                break;
            }
            case "javaclass": case "jclass": case "class":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "jclass", "string");
                }
                else{
                    try{
                        Class c = Class.forName(rawArgs[1]);

                        Consumer<EmbedCreateSpec> e = x -> {
                            x.setTitle("Class " + c.getName());
                            x.addField("Name", "**" + c.getSimpleName() + "**", true);
                            x.addField("Package", "`" + (c.getPackageName().equals("") ? "Default Package" : c.getPackageName())  + "`", true);

                            String type = c.isInterface() ? "Interface" : c.isEnum() ? "Enum" : "Class";
                            x.addField("Type", type, true);

                            x.addField("Inherited", c.getMethods().length + " methods", true);
                            x.addField("Declared",  c.getDeclaredMethods().length + " methods", true);
                            x.addField("Sub-classes", c.getClasses().length + " classes", true);

                            String s = "";
                            for (Class i : c.getInterfaces()) {
                                s += "`" + i.getName() + "`, ";
                            }
                            s = s.trim().replaceAll(",$", "");
                            s = s.equals("") ? "None" : s;

                            x.addField("Superclass",  (c.getSuperclass() == null ? "None" : "`" +c.getSuperclass().getName() + "`"), true);
                            x.addField("Implements",s, true);

                        };

                        BotUtils.sendMessage(channel, e);

                    }catch (ClassNotFoundException e){
                        BotUtils.sendMessage(channel, "Specified class was not found");
                    }
                }
                break;
            }
            case "serverstatus": case "serverinfo": case "server":{
                Consumer<EmbedCreateSpec> embedCreateSpec = e -> {
                    e.setTitle("Information for \"" + guild.getName() + "\"")
                            .setThumbnail(guild.getIconUrl(Image.Format.PNG).get())
                            .setDescription(guild.getDescription().isPresent() ? guild.getDescription().get() : "No description set");

                    List<Member> members = guild.getMembers().collectList().block();

                    e.addField("Server user information", "Members\nHumans\nBots\nOwner\nHuman Administrators", true);

                    int x = 0;
                    int y = 0;
                    for (Member m : members) {
                        if(m.isBot()) x++;
                        List<Role> roles = m.getRoles().collectList().block();
                        if(roles != null && !m.isBot()){
                            for(Role r : roles){
                                if(r.getPermissions().contains(Permission.ADMINISTRATOR)){
                                    y++;
                                    break;
                                }
                            }
                        }

                    }

                    Member owner = guild.getOwner().block();

                    e.addField("\u200b", members.size() + "\n" + (members.size() - x) + "\n" +
                            x + "\n" + owner.getNicknameMention() + "\n" + y, true);
                    e.addField("\u200b", "\u200b", true);


                    List<Role> roles = guild.getRoles().collectList().block();

                    e.addField("Server information", "Roles\nEmojis\nHighest Role\nRegion\nAFK Channel\nCreation Date", true);


                    VoiceChannel afk = guild.getAfkChannel().block();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                    e.addField("\u200b", roles.size() + "\n" + guild.getEmojis().collectList().block().size() + "\n" +
                            roles.get(roles.size() - 1 ).getName() + "\n" + guild.getRegionId() + "\n" + (afk == null ? "Not set" : afk.getMention()) +
                            "\n" + sdf.format(new Date(guild.getJoinTime().get().toEpochMilli())), true);
                    e.addField("\u200b", "\u200b", true);


                    e.addField("Nitro", "Tier\nBoosts", true);
                    e.addField("\u200b", guild.getPremiumTier().name() + "\n" + guild.getPremiumSubcriptionsCount().getAsInt(), true);

                    e.setFooter("Created by " + Main.client.getSelf().block().getMention(),Main.client.getSelf().block().getAvatarUrl());
                };
                BotUtils.sendMessage(channel, embedCreateSpec);
                break;
            }
            case "admin": case "serveradmins": case "admins": {

                Consumer<EmbedCreateSpec> embedCreateSpec = e -> {
                    e.setTitle("List of Administrators for \"" + guild.getName() + "\"");

                    List<Member> members = guild.getMembers().collectList().block();
                    String x = ""; String y  = "";

                    String z = ""; String a = "";

                    x += guild.getOwner().block().getNicknameMention() + "\n";
                    y += "Server Owner\n";

                    members.remove(guild.getOwner().block());

                    for (Member m : members) {
                        List<Role> roles = m.getRoles().collectList().block();
                        if (roles != null) {
                            for (Role r : roles) {
                                if (r.getPermissions().contains(Permission.ADMINISTRATOR)) {
                                    if (m.isBot()) {
                                        z += m.getNicknameMention() + "\n";
                                        a += r.getMention() + "\n";
                                    } else {
                                        x += m.getNicknameMention() + "\n";
                                        y += r.getMention() + "\n";
                                    }
                                    break;
                                }
                            }
                        }
                    }


                    boolean done = false;

                    if(!x.isEmpty() && !y.isEmpty()){
                        e.addField("Human Admins", x, true);
                        e.addField("Granted by", y, true);
                        e.addField("\u200b", "\u200b", true);
                        done = true;
                    }

                    if(!z.isEmpty() && !a.isEmpty()){
                        e.addField("Bot Admins", z, true);
                        e.addField("Granted by", a, true);
                        e.addField("\u200b", "\u200b", true);
                        done = true;
                    }

                    if(!done){
                        e.addField("\u200b", "There are no admins", false);
                    }

                };

                BotUtils.sendMessage(channel, embedCreateSpec);
                break;
            }
            case "serverroles": case "roles":{
                Consumer<EmbedCreateSpec> embedCreateSpec = e -> {
                    e.setTitle("List of roles for \"" + guild.getName() + "\"");

                    List<Role> roles = guild.getRoles().collectList().block();
                    String x = ""; String y  = "";

                    String z = ""; String a = "";

                    x += "";
                    y += "";


                    for (Role r : roles) {
                        if(r.isManaged()){
                            z += r.getMention() + "\n";
                            a += r.getPermissions().contains(Permission.ADMINISTRATOR) ? "Yes\n" : "No\n";
                        }
                        else if(r.isEveryone()){
                            x += "@everyone\n";
                            y += r.getPermissions().contains(Permission.ADMINISTRATOR) ? "Yes\n" : "No\n";
                        }
                        else{
                            x += r.getMention() + "\n";
                            y += r.getPermissions().contains(Permission.ADMINISTRATOR) ? "Yes\n" : "No\n";
                        }
                    }


                    boolean done = false;

                    if(!x.isEmpty() && !y.isEmpty()){
                        e.addField("Regular Roles", x, true);
                        e.addField("Administrator?", y, true);
                        e.addField("\u200b", "\u200b", true);
                        done = true;
                    }

                    if(!z.isEmpty() && !a.isEmpty()){
                        e.addField("Integrated Roles", z, true);
                        e.addField("Administrator?", a, true);
                        e.addField("\u200b", "\u200b", true);
                        done = true;
                    }

                    if(!done){
                        e.addField("\u200b", "There are no roles to show", false);
                    }

                };

                BotUtils.sendMessage(channel, embedCreateSpec);
                break;
            }
            case "roleinfo": case "role":{
                if(lowerArgs.length < 2 || !BotUtils.isPositiveInteger(BotUtils.getUserFromMention(lowerArgs[1]))){
                    BotUtils.sendArgumentsError(channel, "roleinfo", "role");
                }
                else if(guild.getRoleById(Snowflake.of(BotUtils.getUserFromMention(lowerArgs[1]))) == null){
                    BotUtils.sendMessage(channel, "Role not found!");
                }
                else{
                    Role r = guild.getRoleById(Snowflake.of(BotUtils.getUserFromMention(lowerArgs[1]))).block();
                    List<Role> roles = guild.getRoles().collectList().block();

                    Consumer<EmbedCreateSpec> embedCreateSpecConsumer = e -> {
                        e.setTitle("Data for " + r.getName() + " role");

                        e.setColor(r.getColor());

                        List<Member> members = guild.getMembers().collectList().block();
                        int x = 0;
                        for (Member m : members) {
                            if(m.getRoles().collectList().block().contains(r)) x++;
                        }

                        e.addField("Assigned members", "" + x, true);
                        e.addField("Position", "" + (roles.size() - roles.indexOf(r)), true);
                        e.addField("Color", BotUtils.hexFromColor(r.getColor()), true);

                        StringBuilder y = new StringBuilder();
                        Set<Permission> perms = r.getPermissions();
                        for (Permission p : perms) {
                            y.append(
                                    BotUtils.capitalizeFirst(p.toString().replace("_", " ")).replace("Tts", "TTS").
                                            replace("Vad", "Voice Activity") +
                                            "\n");
                        }


                        if(y.toString().isEmpty()) y.append("No permissions explicitly granted");
                        e.addField("Permissions", y.toString(), false);
                    };

                    BotUtils.sendMessage(channel, embedCreateSpecConsumer);

                }

                break;
            }
            case "userinfo": case "user": {

                User userToUse;

                if(lowerArgs.length < 2){
                    userToUse = sender;
                }
                else if(!BotUtils.isPositiveInteger(BotUtils.getUserFromMention(lowerArgs[1]))){
                    BotUtils.sendArgumentsError(channel, "avatar", "optional @user");
                    break;
                }
                else{
                    User u = Main.client.getUserById(Snowflake.of(BotUtils.getUserFromMention(lowerArgs[1]))).block();
                    if(u == null || guild.getMemberById(u.getId()).block() == null){
                        BotUtils.sendMessage(channel, "User not found!");
                        break;
                    }

                    userToUse = u;
                }



                Consumer<EmbedCreateSpec> embedCreateSpecConsumer = e -> {

                    Member m = guild.getMemberById(userToUse.getId()).block();

                    e.setDescription("Data for user " + m.getNicknameMention());
                    List<Role> roles = guild.getRoles().collectList().block();


                    String x = "";

                    e.addField("Basic information", "Global Username\nID\n", true);
                    e.addField("\u200b", userToUse.getUsername() + "#" + userToUse.getDiscriminator() + "\n" +
                            m.getId().asLong(), true);
                    e.addField("\u200b", "\u200b", true);


                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                    x += m.getHighestRole().block().getMention() + "\n";
                    x += sdf.format(m.getJoinTime().toEpochMilli()) + "\n";



                    e.addField("Server-specific information", "Top Role\nServer join time (UTC)", true);
                    e.addField("\u200b", x, true);

                    //e.addField("Permissions", y.toString(), false);
                };

                BotUtils.sendMessage(channel, embedCreateSpecConsumer);



                break;
            }
            case "botinfo": case "info": case "status":{
                Consumer<EmbedCreateSpec> embedCreateSpec = e -> {
                    e.setTitle("Bot information page");

                    e.addField("**Status**", ":green_circle: Online", true);
                    e.addField("**Ping**", Main.client.getResponseTime() + "ms", true);
                    e.addField("**Prefix**", "You already know", true);

                    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                            OperatingSystemMXBean.class);

                    String col1 = "",col2 = "",col3 = "";
                    col1 += "CPU Usage:\n"; col2 += osBean.getSystemCpuLoad() + "%\n"; col3 += "\u200b\n";
                    col1 += "Memory:\n"; col2 += "Free: `" + osBean.getFreePhysicalMemorySize() + "`B\n"; col3 += "Total: `" + osBean.getTotalPhysicalMemorySize() + "`B\n";
                    col1 += "Swap:\n"; col2 += "Free: `" + osBean.getFreeSwapSpaceSize() + "`B\n"; col3 += "Total: `" + osBean.getTotalSwapSpaceSize() + "`B\n";
                    col1 += "Network:\n"; col2 += "IP: `" + BotUtils.getPublicIP() + "`\n"; col3 += "Status: OK" + "\n";



                    e.addField("**System info**", col1, true);
                    e.addField("\u200b", col2, true);
                    e.addField("\u200b", col3, true);

                    User bill = Main.client.getUserById(Snowflake.of(506696814490288128L)).block();
                    e.addField("**Credits**", "Coded by " + bill.getUsername() + "#" + bill.getDiscriminator() +
                            "\nCoded using Discord4j version 3.0.14\n", true);


                };
                BotUtils.sendMessage(channel, embedCreateSpec);
                break;
            }
            case "stackoverflow": case "so": case "overflow":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "stackoverflow", "[error]");
                }
                else{

                    String out = null;
                    try {
                        out = URLEncoder.encode(lowerArgs[1], StandardCharsets.UTF_8.toString());
                    } catch (UnsupportedEncodingException e) { }
                    for (int i = 2; i < lowerArgs.length; i++) {
                        try {
                            out += "+" + URLEncoder.encode(lowerArgs[i], StandardCharsets.UTF_8.toString()) ;
                        } catch (UnsupportedEncodingException e) {}
                    }
                    BotUtils.sendMessage(channel, "StackOverflow link: https://stackoverflow.com/search?q=" + out);
                }
                break;
            }
            case "alpha": case "decodeascii": case "toalpha":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "alpha", "ascii");
                }
                else {
                    String out = "";
                    for (int i = 1; i < lowerArgs.length; i++) {
                        try {
                            out += (char) Short.parseShort(lowerArgs[i]);
                            if(Short.parseShort(lowerArgs[i]) > 255){
                                BotUtils.sendMessage(channel, "Invalid ascii character code " + lowerArgs[i] + "\" at index " + (i-1));
                                return;
                            }
                        } catch (NumberFormatException nfe) {
                            BotUtils.sendMessage(channel, "Invalid ascii character code " + lowerArgs[i] + "\" at index " + (i-1));
                            return;
                        }
                    }
                    BotUtils.sendMessage(channel, "Resultant string is:\n```" + out + "```");
                }
                break;
            }
            case "werk":{
                BotUtils.sendMessage(channel, "No Trevor, you must learn to spell!");
                break;
            }
            case "reverse": case "rev":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "reverse", "string");
                    break;
                }
                else{
                    String reversed = new StringBuilder(BotUtils.removeCommand(body, rawArgs[0])).reverse().toString();
                    BotUtils.sendMessage(channel, "```" + reversed + "```");
                }
                break;
            }
            case "eval": case "exec": case "execute":{
                if(lowerArgs.length < 3){
                    BotUtils.sendArgumentsError(channel, "eval", "language", "code");
                }
                else if(!Data.languageID.containsKey(lowerArgs[1])){
                    BotUtils.sendMessage(channel, "Language was not found!");
                }
                else{
                    BotUtils.sendMessage(channel, "Evaluating... This can take a while");
                    String s = BotUtils.removeCommand(body, rawArgs[0] + " " + rawArgs[1]);
                    new EvalThread(lowerArgs[1], s, channel).start();
                }
                break;
            }
            case "help":{
                BotUtils.sendMessage(channel, Data.helpEmbed);
                break;
            }
        }

    }
}