import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.EmbedCreateSpec;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class BotUtils {
    static final String BOT_PREFIX = "^";
    static final Random random = new Random();

    static final long aopdFirstTime = 808012800000L;

    static boolean isPositiveInteger(String s){
        return s.matches("\\d+?");
    }

    static boolean isNumeric(String s){
        return s.matches("-?[\\d]*\\.?[\\d][\\d]*");
    }

    static boolean isInteger(String s){
        return s.matches("[-\\d]\\d+?");
    }

    static boolean isAlphaNumeric(String s){
        return s.matches("-?[a-z\\d]+?");
    }


    static Message sendMessage(MessageChannel channel, String message){
        if(message.length() > 2000){
            return channel.createMessage("```Resultant message greater than 2000 characters```").block();
        }
        else{
            return channel.createMessage(message).block();
        }
    }

    static Message sendMessage(MessageChannel channel, Consumer<EmbedCreateSpec> embed){
        return channel.createEmbed(embed).block();
    }

    static void sendArgumentsError(MessageChannel channel , String command, String... argType){
        String out = "Invalid arguments! Syntax is: `" + command;
        for (String s : argType) {
            out += " [" + s + "]";
        }
        sendMessage(channel, out + "`");
    }

    static HashMap<Integer,Integer> primeFactor(int n){
        if(n < 1){
            return new HashMap<>();
        }

        HashMap<Integer, Integer> numberMap = new HashMap<>();

        int factorCounter = 0;


        while (n%2 == 0){
            factorCounter++;
            n /= 2;
        }
        if(factorCounter > 0){numberMap.put(2,factorCounter);}

        for (int i = 3; i <= Math.sqrt(n); i++) {
            factorCounter = 0;
            while (n % i == 0){
                factorCounter++;
                n /= i;
            }

            if(factorCounter > 0){numberMap.put(i,factorCounter);}
        }

        if(n > 2){
            numberMap.put(n, 1);
        }

        return numberMap;
    }

    static double evalMath(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm(); // addition
                    else if (eat('-')) x -= parseTerm(); // subtraction
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor(); // multiplication
                    else if (eat('/')) x /= parseFactor(); // division
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -parseFactor(); // unary minus

                double x;
                int startPos = this.pos;
                if (eat('(')) { // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') { // functions
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    switch (func) {
                        case "sqrt":
                            x = Math.sqrt(x);
                            break;
                        case "sin":
                            x = Math.sin(Math.toRadians(x));
                            break;
                        case "cos":
                            x = Math.cos(Math.toRadians(x));
                            break;
                        case "tan":
                            x = Math.tan(Math.toRadians(x));
                            break;
                        default:
                            throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }

                if (eat('^')) x = Math.pow(x, parseFactor()); // exponentiation

                return x;
            }
        }.parse();
    }

    static String removeCommand(String s, String cmd){
        return s.replaceFirst(Pattern.quote(cmd), "").substring(1);
    }

    static String getUserFromMention(String s){
        return s.replaceAll("[^\\d]", "");
    }

    static String hexFromColor(Color color){
        String hex = Integer.toHexString(color.getRGB() & 0xffffff);
        if (hex.length() < 6) {
            hex = "0" + hex;
        }
        return "#" + hex;
    }

    static String capitalizeFirst(String s){
        String[] x = s.split(" ");
        StringBuilder out = new StringBuilder();
        for (String y : x) {
            y = y.toLowerCase();
            if(y.length() == 1){ out.append(y.toUpperCase());}
            else{
                out.append(y.substring(0,1).toUpperCase() + y.substring(1));
            }

            out.append(" ");
        }

        return out.toString().trim();
    }

    static String getPublicIP(){
        URL whatismyip = null;
        try {
            whatismyip = new URL("http://checkip.amazonaws.com");
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));

            String ip = in.readLine();
            return ip;
        } catch (Exception e){}
        return "0.0.0.0";
    }
}
