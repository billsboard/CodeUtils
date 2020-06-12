import com.fasterxml.jackson.annotation.JsonAlias;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.data.stored.ChannelBean;
import discord4j.core.object.entity.*;
import discord4j.core.object.reaction.Reaction;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Image;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.EmbedCreateSpec;
import com.sun.management.OperatingSystemMXBean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

    private void onMessageReceived(Message message) throws Exception{

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
            case "calc": case "calculate": case "c":{
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

                    e.addField("Server information", "Roles\nEmojis\nChannels\nHighest Role\nRegion\nAFK Channel\nBot Join Date", true);


                    VoiceChannel afk = guild.getAfkChannel().block();

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                    e.addField("\u200b", roles.size() + "\n" + guild.getEmojis().collectList().block().size() + "\n" + guild.getChannels().collectList().block().size()
                             + "\n" + roles.get(roles.size() - 1 ).getName() + "\n" + guild.getRegionId() + "\n" + (afk == null ? "Not set" : afk.getMention()) +
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
                        Collections.reverse(roles);
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
                    Collections.reverse(roles);
                    String x = ""; String y  = "", l1 = "";

                    String z = ""; String a = "", l2 = "";

                    x += "";
                    y += "";


                    for (Role r : roles) {
                        if(r.isManaged()){
                            z += r.getMention() + "\n";
                            a += r.getPermissions().contains(Permission.ADMINISTRATOR) ? "Yes\n" : "No\n";
                            l2 += r.getPermissions().isEmpty() ? "Yes\n" : "No\n";
                        }
                        else if(r.isEveryone()){
                            x += "@everyone\n";
                            y += r.getPermissions().contains(Permission.ADMINISTRATOR) ? "Yes\n" : "No\n";
                            l1 += r.getPermissions().isEmpty() ? "Yes\n" : "No\n";
                        }
                        else{
                            x += r.getMention() + "\n";
                            y += r.getPermissions().contains(Permission.ADMINISTRATOR) ? "Yes\n" : "No\n";
                            l1 += r.getPermissions().isEmpty() ? "Yes\n" : "No\n";
                        }

                    }


                    boolean done = false;

                    if(!x.isEmpty() && !y.isEmpty()){
                        e.addField("Regular Roles", x, true);
                        e.addField("Administrator?", y, true);
                        e.addField("Empty (Permissionless)?", l1, true);
                        done = true;
                    }

                    if(!z.isEmpty() && !a.isEmpty()){
                        e.addField("Integrated Roles", z, true);
                        e.addField("Administrator?", a, true);
                        e.addField("Empty (Permissionless)?", l2, true);
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


                    String x = "";

                    e.addField("Basic information", "Global Username\nID\nBot?", true);
                    e.addField("\u200b", userToUse.getUsername() + "#" + userToUse.getDiscriminator() + "\n" +
                            m.getId().asLong() + "\n" + (userToUse.isBot() ? "Yes" : "No"), true);
                    e.addField("\u200b", "\u200b", true);


                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                    x += m.getHighestRole().block().getMention() + "\n";
                    x += sdf.format(m.getJoinTime().toEpochMilli()) + "\n";



                    e.addField("Server-specific information", "Top Role\nServer join time (UTC)", true);
                    e.addField("\u200b", x, true);

                    e.setColor(m.getColor().block());
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
            case "weather":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "weather", "location");
                }
                else{
                    try{
                        String loc = BotUtils.removeCommand(body, rawArgs[0]);
                        URL url = new URL(("https://api.openweathermap.org/data/2.5/weather?q=" + loc + "&appid=" + Data.apiKeys.get("openweather")));
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


                            JSONObject obj = new JSONObject(inputLine);

                            Consumer<EmbedCreateSpec> embedCreateSpecConsumer = e -> {

                                try {
                                    JSONObject coords = obj.getJSONObject("coord");


                                    URL u = new URL("http://open.mapquestapi.com/geocoding/v1/reverse?key=" + Data.apiKeys.get("geocoding") +
                                            "&location=" + coords.getFloat("lat") + "," + coords.getFloat("lon"));
                                    HttpURLConnection uCon = (HttpURLConnection) u.openConnection();
                                    uCon.setRequestMethod("GET");

                                    String descriptor = obj.getJSONObject("sys").getString("country");

                                    if(uCon.getResponseCode() == HttpURLConnection.HTTP_OK){
                                        BufferedReader br = new BufferedReader(new InputStreamReader(
                                                uCon.getInputStream()));
                                        String line;
                                        StringBuilder res = new StringBuilder();
                                        while ((line = br.readLine()) != null) {
                                            res.append(line);
                                        }
                                        line = res.toString();

                                        descriptor = new JSONObject(line).getJSONArray("results").getJSONObject(0).getJSONArray("locations")
                                            .getJSONObject(0).getString("adminArea3");
                                    }


                                    JSONObject sys = obj.getJSONObject("sys");
                                    String t1;
                                    String t2;

                                    e.setTitle("Weather data for " + obj.getString("name") + ", " + descriptor);

                                    t1 = coords.getInt("lat") < 0 ? -1 * coords.getFloat("lat") + "°S" : coords.getFloat("lat") + "°N";
                                    t2 = coords.getInt("lon") < 0 ? -1 * coords.getFloat("lon") + "°W" : coords.getFloat("lon") + "°E";

                                    e.setDescription(t1 + " " + t2);


                                    JSONObject bWeather = obj.getJSONArray("weather").getJSONObject(0);
                                    e.addField("Basic weather data", bWeather.getString("main"), true);
                                    e.addField("\u200b", bWeather.getString("description"), true);
                                    e.addField("\u200b", "\u200b", true);

                                    JSONObject temp = obj.getJSONObject("main");
                                    e.addField("Temperature data", "Temperature\nHigh\nLow\nFeels Like", true);
                                    e.addField("\u200b", String.format("%.2f", temp.getFloat("temp") - 273.15) + "°C\n" +
                                            String.format("%.2f", temp.getFloat("temp_max") - 273.15) + "°C\n" +
                                            String.format("%.2f", temp.getFloat("temp_min") - 273.15) + "°C\n" +
                                            String.format("%.2f", temp.getFloat("feels_like") - 273.15) + "°C\n", true);
                                    e.addField("\u200b", "\u200b", true);

                                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
                                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                    e.addField("Other data", "Pressure\nHumidity\nVisibility\nSunrise\nSunset", true);
                                    e.addField("\u200b", temp.getInt("pressure") + " hPa\n" +
                                            temp.getInt("humidity") + "\n" + obj.getInt("visibility") + " meters\n" +
                                            sdf.format(sys.getLong("sunrise") + obj.getLong("timezone")) + "\n" +
                                            sdf.format(sys.getLong("sunset") + obj.getLong("timezone")), true
                                    );
                                }catch (Exception exc){
                                    return;
                                }

                            };

                            BotUtils.sendMessage(channel, embedCreateSpecConsumer);
                        }
                        else{
                            BotUtils.sendMessage(channel, "Weather data for " + loc + " not found (check spelling?)");
                        }
                    } catch (Exception e){
                        e.printStackTrace();
                    }


                }

                break;
            }
            case "geolocation": case "geoloc":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "geolocation", "location");
                }
                else{
                    try{
                        String loc = BotUtils.removeCommand(body, rawArgs[0]);
                        URL u = new URL("http://open.mapquestapi.com/geocoding/v1/address?key=" + Data.apiKeys.get("geocoding") +
                                "&location=" + loc);
                        HttpURLConnection uCon = (HttpURLConnection) u.openConnection();
                        uCon.setRequestMethod("GET");

                        if(uCon.getResponseCode() == HttpURLConnection.HTTP_OK) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    uCon.getInputStream()));
                            String inputLine;
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            inputLine = response.toString();

                            JSONObject obj = new JSONObject(inputLine);
                            JSONObject res = obj.getJSONArray("results").getJSONObject(0);
                            JSONObject firstLoc = res.getJSONArray("locations").getJSONObject(0);

                            Consumer<EmbedCreateSpec> embedCreateSpecConsumer = e -> {
                                e.setDescription("Geocode data for \"" + res.getJSONObject("providedLocation").getString("location") + "\"");
                                e.addField("Basic address info", firstLoc.getString("street") + " " +
                                        firstLoc.getString("adminArea5") + ", " + firstLoc.getString("adminArea3") + ", " + firstLoc.getString("adminArea1"),
                                        false);

                                JSONObject coords = firstLoc.getJSONObject("latLng");
                                String t1 = coords.getInt("lat") < 0 ? -1 * coords.getFloat("lat") + "°S" : coords.getFloat("lat") + "°N";
                                String t2 = coords.getInt("lng") < 0 ? -1 * coords.getFloat("lng") + "°W" : coords.getFloat("lng") + "°E";
                                e.addField("Geolocation Coordinate Data", t1, true);
                                e.addField("\u200b", t2, true);
                            };

                            BotUtils.sendMessage(channel, embedCreateSpecConsumer);

                        }


                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
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
            case "element":{
                if(lowerArgs.length < 2){
                    BotUtils.sendArgumentsError(channel, "element", "identifier");
                }
                else{
                    String inputLine = "{\"message\":\"does not exists\"}";

                    if(BotUtils.isPositiveInteger(lowerArgs[1])){
                        URL url = new URL("https://neelpatel05.pythonanywhere.com/element/atomicnumber?atomicnumber=" + lowerArgs[1]);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");

                        if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    con.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            inputLine = response.toString();
                        }
                    }
                    else if(lowerArgs[1].length() < 3){
                        URL url = new URL("https://neelpatel05.pythonanywhere.com/element/symbol?symbol=" + lowerArgs[1]);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");

                        if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    con.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            inputLine = response.toString();
                        }
                    }
                    else {
                        URL url = new URL("https://neelpatel05.pythonanywhere.com/element/atomicname?atomicname=" + lowerArgs[1]);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.setRequestMethod("GET");

                        if(con.getResponseCode() == HttpURLConnection.HTTP_OK){
                            BufferedReader in = new BufferedReader(new InputStreamReader(
                                    con.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            inputLine = response.toString();
                        }
                    }

                    if(inputLine.equals("{\"message\":\"does not exists\"}")){
                        BotUtils.sendMessage(channel, "The request element could not be found");
                    }
                    else{
                        JSONObject res = new JSONObject(inputLine);

                        Consumer<EmbedCreateSpec> embedCreateSpecConsumer = e -> {
                            e.setUrl("https://en.wikipedia.org/wiki/" + res.getString("name"));
                            e.setTitle(res.getString("name"));

                            String stable = "Yes";

                            if(String.valueOf(res.get("atomicMass")).contains("[")){
                                stable = "No";
                                res.put("atomicMass", res.get("atomicMass") + "(1)");
                            }
                            res.put("atomicMass", String.valueOf(res.get("atomicMass")).replace("[", "").replace("]", ""));


                            e.addField("Atomic Number", res.getInt("atomicNumber") + "", true);
                            e.addField("Symbol", res.getString("symbol"), true);
                            e.addField("Mass", res.getString("atomicMass").substring(0, res.getString("atomicMass").indexOf("(")) + " amu", true);

                            e.addField("Basic information", "Protons\nNeutrons\nElectrons\nGroup\nIon Charges\nStable?", true);


                            String d1 = res.getInt("atomicNumber") + "\n" +
                                    (Math.round(Float.parseFloat(res.getString("atomicMass").substring(0, res.getString("atomicMass").indexOf("(")))) - res.getInt("atomicNumber")) +
                                    "\n" + res.getInt("atomicNumber") + "\n" +
                                    BotUtils.capitalizeFirst(res.getString("groupBlock"));

                            String[] oxyStates = String.valueOf(res.get("oxidationStates")).split(", ");
                            if(oxyStates.length < 1 || oxyStates[0].isEmpty()){
                                d1 += "\n0";
                            }
                            else{
                                int x = Integer.parseInt(oxyStates[0]);
                                d1 += "\n" + x;
                                for (int i = 1; i < oxyStates.length; i++) {
                                    int y = Integer.parseInt(oxyStates[i]);
                                    if((y > 0 && x < 0) || (y < 0 && x > 0)){
                                        break;
                                    }

                                    d1 += ", " + y;
                                }
                            }
                            d1 += "\n" + stable;
                            e.addField("\u200b", d1, true);
                            e.addField("\u200b", "\u200b", true);

                            e.addField("Temperature information", "Boiling point\nMelting point\nState at STP", true);
                            e.addField("\u200b",
                                    (String.valueOf(res.get("boilingPoint")).isEmpty() ?  "Unknown" : res.get("boilingPoint") + " K")+ "\n" +
                                            (String.valueOf(res.get("meltingPoint")).isEmpty() ?  "Unknown" : res.get("meltingPoint") + " K") + "\n" +
                                    BotUtils.capitalizeFirst((String.valueOf(res.get("standardState")).isEmpty() ?  "Unknown" : res.get("standardState") + "")), true);
                            e.addField("\u200b", "\u200b", true);

                            e.addField("Other information", "Electronegativity\nRadius\nDensity\nFirst Ionization Energy\nHybridization\nDiscovered in\nForms " +
                                    BotUtils.capitalizeFirst(res.getString("bondingType").isEmpty() ? "unknown" : res.getString("bondingType")) + " bonds", true);

                            String d2 = (String.valueOf(res.get("electronegativity")).isEmpty() ? "0" :  res.get("electronegativity"))
                                    + "\n" + (String.valueOf(res.get("atomicRadius")).isEmpty() ? "Unknown" :  res.get("atomicRadius") + " picometers") + "\n" +
                                    (String.valueOf(res.get("density")).isEmpty() ? "Unknown" :  res.get("density") + " kg/cm^3") + "\n" +
                                    (String.valueOf(res.get("ionizationEnergy")).isEmpty() ? "Unknown" : res.get("ionizationEnergy") + " kJ/mol") + "\n" +
                                    (String.valueOf(res.get("electronicConfiguration")).isEmpty() ? "Unknown" :  res.get("electronicConfiguration")) +
                                    "\n" + res.get("yearDiscovered");

                            e.addField("\u200b", d2, true);

                        };

                        BotUtils.sendMessage(channel, embedCreateSpecConsumer);
                    }
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
            case "clear":{

                if(lowerArgs.length < 2 || !BotUtils.isPositiveInteger(lowerArgs[1])){
                    BotUtils.sendArgumentsError(channel, "clear", "messages");
                }
                else {
                    GuildChannel c = guild.getChannelById(channel.getId()).block();
                    ChannelBean bean = new ChannelBean(c.getId().asLong(), c.getType().getValue());

                    TextChannel ctxt = new TextChannel(Main.client.getServiceMediator(), bean);

                    if(!c.getEffectivePermissions(sender.getId()).block().contains(Permission.MANAGE_MESSAGES)){
                        BotUtils.sendMessage(channel, "You must have the Manage Messages permission");
                        break;
                    }
                    else if(Integer.parseInt(lowerArgs[1]) > 314){
                        BotUtils.sendMessage(channel, "You can only delete at most 314 messages!");
                        break;
                    }

                    int numToDelete = Integer.parseInt(lowerArgs[1]);

                    List<Message> mList = channel.getMessagesBefore(message.getId()).collectList().block();
                    List<Message> deleteList = mList.subList(0, numToDelete);

                    List<Snowflake> flakes = new ArrayList<>();
                    for (Message m : deleteList) {
                        flakes.add(m.getId());
                    }




                    Publisher<Snowflake> ki = Flux.just(flakes.toArray(new Snowflake[0]));

                    Flux<Snowflake> result = ctxt.bulkDelete(ki);
                    while (result.hasElements().block()) {
                        result.blockFirst();
                    }
                    message.delete().block();
                }

                break;
            }
            case "apic": case "apod": case "astronomypic":{

                long time = ThreadLocalRandom.current().nextLong(BotUtils.aopdFirstTime, new Date().getTime());
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

                URL url = new URL("https://api.nasa.gov/planetary/apod?date=" + sdf.format(new Date(time)) + "&api_key=" + Data.apiKeys.get("nasa"));
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

                    JSONObject obj = new JSONObject(inputLine);


                    Consumer<EmbedCreateSpec> spec = e -> {
                        e.setDescription("NASA Astronomy Picture of the Day for " + sdf.format(new Date(time)));
                        e.setImage(obj.getString("url"));
                        e.setFooter(obj.getString("explanation"), "");
                    };

                    BotUtils.sendMessage(channel, spec);
                }

                break;
            }
            case "poll":{
                if(lowerArgs.length < 6){
                    BotUtils.sendArgumentsError(channel, "poll", "question |", "reactiontext |", "reactions");
                }
                else{
                    String[] data = BotUtils.removeCommand(body, rawArgs[0]).replace(" | ", "|").split("\\|");
                    Consumer<EmbedCreateSpec> spec = e -> {
                        e.setDescription(data[0]);
                        e.addField("React with", data[1], false);

                        e.setFooter("Poll requested by " + sender.getUsername() + "#" + sender.getDiscriminator() + " using CodeUtils", sender.getAvatarUrl());
                    };

                    String[] reactions = data[2].split(" ");
                    Message m = BotUtils.sendMessage(channel, spec);
                    message.delete().block();
                    for (int i = 0; i < reactions.length; i++) {
                        String rct = reactions[i];
                        if(rct.length() < 3){
                            m.addReaction(ReactionEmoji.unicode(rct)).block();
                        }
                        else{
                            String[] reactionData = rct.substring(0, rct.length() - 1).split(":");
                            m.addReaction(ReactionEmoji.of(Long.parseLong(reactionData[2]), reactionData[1], false)).block();
                        }
                    }

                }
                break;
            }
            case "help":{
                BotUtils.sendMessage(channel, "Sending help message to your private channel!");
                for (Consumer<EmbedCreateSpec> e : Data.helpEmbeds) {
                    BotUtils.sendMessage(sender.getPrivateChannel().block(), e);
                }
                break;
            }
        }

    }
}