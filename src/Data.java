import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateSpec;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.*;
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

    static ArrayList<Consumer<EmbedCreateSpec>> helpEmbeds = new ArrayList<>();
    static ArrayList<String> helpCategories = new ArrayList<>();
    static HashMap<String, Consumer<EmbedCreateSpec>> subcommandHelpEmbeds = new HashMap<>();

    static ArrayList<String> protectedIDs = new ArrayList<>(){
        {
            add("704852114517655562");
            add("506696814490288128");
            add("519326187491950593");
            add("705825865790914580");
            add("710521453644349450");
        }
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

    static HashMap<String, Integer> languageID = new HashMap<>();

    static HashMap<String, String> apiKeys = new HashMap<>();

    //static Stack<Message> messageQueue = new Stack<>();
    static FixedStack<Message> messageQueue = new FixedStack<>(100);
    static HashMap<Long, Consumer<EmbedCreateSpec>> runningPolls = new HashMap<>();

    static HashMap<String, String> availableCurrencies = new HashMap<>(){
        {
            put("USD", "US Dollar");
            put("CAD", "Canadian Dollar");
            put("JPY", "Japanese Dollar");
            put("BGN", "Bulgarian Lev");
            put("CZK", "Czech Koruna");
            put("DKK", "Danish Krone");
            put("GBP", "Pound Sterling");
            put("HUN", "Hungarian Forint");
            put("PLN", "Polish Zolty");
            put("RON", "Romanian Leu");
            put("SEK", "Swedish Krona");
            put("CHF", "Swiss Franc");
            put("ISK", "Icelandic Krona");
            put("NOK", "Norwegian Krone");
            put("HRK", "Croatian Kuna");
            put("RUB", "Russian Rouble");
            put("TRY", "Turkish Lira");
            put("AUD", "Australian Dollar");
            put("CNY", "Chinese Yuan");
            put("HKD", "Hong Kong Dollar");
            put("IDR", "Indonesian Rupiah");
            put("ILS", "Israeli Shekel");
            put("INR", "Indian Rupee");
            put("KRW", "South Korean Won");
            put("MXN", "Mexican Peso");
            put("MYR", "Malaysian Ringgit");
            put("NZD", "New Zealand Dollar");
            put("PHP", "Philippine Peso");
            put("SGD", "Singapore Dollar");
            put("THB", "Thai Baht");
            put("ZAR", "South African Rand");
        }
    };


    static JSONObject pokemonTypeEffectiveness = null;

    static void initDataParams(){

        /* ---- General help command ---- */
        String s = getResource("DataFiles/help.txt");
        Scanner scan = new Scanner(s);

        String in = scan.nextLine();
        while (scan.hasNextLine()){
            String title = "";

            String[] data;
            StringBuilder temp = new StringBuilder();

            if(in.startsWith("[[")){
                title = in.substring(2, in.length() - 2);
                in = scan.nextLine();
                while (!in.startsWith("[[")){
                    temp.append(in);
                    temp.append("\n\n");
                    if(scan.hasNextLine()){
                        in = scan.nextLine();
                    }
                    else{
                        break;
                    }
                }
            }

            data = temp.toString().split("\n\n");

            String finalTitle = title;
            String[] finalData = data;
            Consumer<EmbedCreateSpec> spec = e -> {
                e.setDescription(finalTitle);

                StringBuilder x = new StringBuilder();
                StringBuilder y = new StringBuilder();

                for (String finalDatum : finalData) {
                    String[] z = finalDatum.split(" :: ");
                    x.append(z[0]).append("\n");
                    y.append(z[1]).append("\n");
                }

                e.addField("Command", x.toString(), true);
                e.addField("\u200b", "\u200b", true);
                e.addField("Description", y.toString(), true);

                e.setFooter("[] denotes required parameter, () denotes optional parameter\nIf optional param is left blank, the sender's context will be used", "");
            };
            helpCategories.add(title);
            helpEmbeds.add(spec);
        }
        scan.close();

        /* ---- Subcommand help commands ---- */
        s = getResource("DataFiles/subHelp.txt");
        scan = new Scanner(s);

        in = scan.nextLine();
        while (scan.hasNextLine()){
            String title = "";

            String[] data;
            StringBuilder temp = new StringBuilder();

            if(in.startsWith("[[")){
                title = in.substring(2, in.length() - 2);
                in = scan.nextLine();
                while (!in.startsWith("[[")){
                    temp.append(in);
                    temp.append("\n\n");
                    if(scan.hasNextLine()){
                        in = scan.nextLine();
                    }
                    else{
                        break;
                    }
                }
            }

            data = temp.toString().split("\n\n");

            String finalTitle = title;
            String[] finalData = data;
            Consumer<EmbedCreateSpec> spec = e -> {
                e.setDescription(finalTitle + " subcommands");

                StringBuilder x = new StringBuilder();
                StringBuilder y = new StringBuilder();

                for (String finalDatum : finalData) {
                    String[] z = finalDatum.split(" :: ");
                    //x.append(finalTitle.toLowerCase()).append(" ").append(z[0]).append("\n");
                    x.append(z[0]).append("\n");
                    y.append(z[1]).append("\n");
                }

                e.addField("Command", x.toString(), true);
                e.addField("\u200b", "\u200b", true);
                e.addField("Description", y.toString(), true);

                e.setFooter("Use these commands prefixed with the " + finalTitle.toLowerCase() + " command\nSyntax should be: " + finalTitle.toLowerCase() +
                        " [subcommand]", null);
            };
            subcommandHelpEmbeds.put(title.toLowerCase(), spec);
        }
        scan.close();

        /* ---- Pokemon type data effectiveness ---- */
        pokemonTypeEffectiveness = new JSONObject(getResource("DataFiles/pkmonTypeEffectiveness.json"));
    }

    static String getResource(String file){
        StringBuilder out = new StringBuilder();

        InputStream i = Data.class.getResourceAsStream(file);
        Scanner scan = new Scanner(i);

        while (scan.hasNextLine()){
            out.append(scan.nextLine());
            out.append("\n");
        }

        return out.toString();
    }
}
