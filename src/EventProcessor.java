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
                    e.addField("Members", "" + members.size(), true);

                    int x = 0;
                    int y = 0;
                    for (Member m : members) {
                        if(m.isBot()) x++;
                        else if(m.getHighestRole().block().getPermissions().contains(Permission.ADMINISTRATOR)) y++;
                    }
                    e.addField("Humans", "" + (members.size() - x), true);
                    e.addField("Bots", "" + x, true);

                    Member owner = guild.getOwner().block();
                    e.addField("Owner", owner.getNicknameMention(), true);
                    e.addField("\u200b", "\u200b", true);
                    e.addField("Human Administrators", "" + y, true);

                    List<Role> roles = guild.getRoles().collectList().block();
                    e.addField("Roles", "" + roles.size(), true);
                    e.addField("Highest Role", roles.get(roles.size() - 1 ).getName(), true);
                    e.addField("Emojis", guild.getEmojis().collectList().block().size() + "", true);
                    e.addField("Region", guild.getRegionId(), true);

                    VoiceChannel afk = guild.getAfkChannel().block();
                    e.addField("AFK Channel", afk == null ? "Not set" : afk.getMention(), true);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                    e.addField("Creation date", sdf.format(new Date(guild.getJoinTime().get().getEpochSecond())), true);

                    //e.setFooter("Created by " + Main.client.getSelf().block().getMention(),Main.client.getSelf().block().getAvatarUrl());
                };
                BotUtils.sendMessage(channel, embedCreateSpec);
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
            case "help":{
                BotUtils.sendMessage(channel, Data.helpEmbed);
                break;
            }
        }

    }
}