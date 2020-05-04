import discord4j.core.spec.EmbedCreateSpec;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class Data {
    static String[] stUrls = {"https://drive.google.com/uc?export=download&id=1xNhQCFL3DT1GrtbUK979G9BvUo-G_o6K",
        "https://drive.google.com/uc?export=download&id=1yln-t4sh26TnHcXZCPLPfedHA6I6Sm4I",
        "https://drive.google.com/uc?export=download&id=1SChDUY9C0vbYkxwub3XTEZHX8Hd5-pLC",
        "https://drive.google.com/uc?export=download&id=1Y4IFnHJl8aBlSsenk7LTsSdJDhIRkiI1",
        "https://drive.google.com/uc?export=download&id=1pqge0J03k3dWnMsF4em61lnB9LPvHAUm",
        "https://drive.google.com/uc?export=download&id=1wXlJ52-kwSgv8KQjXszAZz0MwYHQq2aW",
        "https://drive.google.com/uc?export=download&id=1YE3jL_bkOJ_Zs-kFfGuQzyx0TVZ-7JLf",
        "https://drive.google.com/uc?export=download&id=19YfyRfe0Rc6DAfZKwuxaQGqVFQcTlxY-"};

    static Consumer<EmbedCreateSpec> helpEmbed = x -> {
        x.setTitle("Help page");

        x.addField("**Math**", "`pfact [integer]`\n" +
                    "`calc [expression]`\n" +
                    "`base [from] [to] [value]`\n" +
                    "`round [number]`\n" +
                    "`floor [number]`\n" +
                    "`ceiling [number]`\n" +
                    "`constant` [constant]", true)
                .addField("\u200b", "Gets prime factorization\n" +
                        "Evaluates math expression\n" +
                        "Converts number between bases\n" +
                        "Round number to nearest integer\n" +
                        "Round decimal down\n" +
                        "Round decimal up\n" +
                        "Gets a mathematical constant", true)
                .addField("\u200b", "\u200b", true);

        x.addField("**String**", "`ascii [string]`\n" +
                "`alpha [string]`\n" +
                "`length [string]`", true)
                .addField("\u200b", "Gets string's ascii value\n" +
                        "Decodes ascii to string\n" +
                        "Gets length of string\n", true)
                .addField("\u200b", "\u200b", true);

        x.addField("**Discord**", "`avatar (user)`\n" +
                "`serverinfo`", true)
                .addField("\u200b", "Gets a profile picture\n" +
                        "Displays server data\n", true)
                .addField("\u200b", "\u200b", true);

        x.addField("**Programming**", "`time (timeZone)`\n" +
                "`jclass [string]`\n" +
                "`stackoverflow [string]`", true)
                .addField("\u200b", "Get the current time\n" +
                        "Get data for a Java class\n" +
                        "Search StackOverflow for answers", true)
                .addField("\u200b", "\u200b", true);

        x.addField("**Other**", "`help`\n" +
                "`info`\n" +
                "`squaretwitter`", true)
                .addField("\u200b", "Displays this text\n" +
                        "Returns bot client info\n" +
                        "Displays a random square twitter", true)
                .addField("\u200b", "\u200b", true);

        x.setFooter("Parentheses \"()\" denote optional parameter. Leaving it blank will cause the bot to default to the sender's context", "");
    };

    static HashMap<String, String> constants = new HashMap<>(){
        {
            put("pi", Double.toString(Math.PI));
            put("e", Double.toString(Math.E));
            put("tau", 2*Math.PI + "");
            put("intmax", Integer.toString(Integer.MAX_VALUE));
            put("intmin", Integer.toString(Integer.MIN_VALUE));
            put("longmax", Long.toString(Long.MAX_VALUE));
            put("longmin", Long.toString(Long.MIN_VALUE));
            put("bytemax", "" + Byte.MAX_VALUE);
            put("bytemin", "" + Byte.MIN_VALUE);
            put("shortmax", "" + Short.MAX_VALUE);
            put("shortmin", "" + Short.MIN_VALUE);
            put("uintmax", "4294967295");
            put("uintmin", "0");
            put("zero", "0");
            put("phi", "1.618033988749894848");
            put("infinity", "∞");
            put("rkpa", "8.3145");
            put("ratm", "0.08205");
            put("sqrt2", "1.41421356237309504");
            put("g", "9.81");
            put("L", "6.02214076 E23");
            put("G", "6.67 E-11");
            put("one", "1");
        }
    };

}
