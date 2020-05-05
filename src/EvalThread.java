import discord4j.core.object.entity.MessageChannel;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

class EvalThread extends Thread {

    private String code;
    private int langID;
    private MessageChannel c;

    EvalThread(String lang, String sourceCode,
               MessageChannel channel){
        code = sourceCode;
        c = channel;
        langID = Data.languageID.get(lang);
    };

    public void run(){
        try {
            URL url = new URL("https://api.judge0.com/submissions/");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");

            String payload = "{\"source_code\":\"" + code + "\",\"language_id\":" + langID + "}";

            OutputStream os = con.getOutputStream();
            byte[] input = payload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);



            if(con.getResponseCode() == HttpURLConnection.HTTP_CREATED){
                String token = "";

                String inputLine;
                StringBuilder response = new StringBuilder();
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                inputLine = response.toString();
                token = new JSONObject(inputLine).getString("token");

                url = new URL("https://api.judge0.com/submissions/" + token);
                HttpURLConnection dataReq = (HttpURLConnection) url.openConnection();
                dataReq.setRequestMethod("GET");

                StringBuilder temp;

                while(true){
                    String ln;
                    BufferedReader br = new BufferedReader(new InputStreamReader(dataReq.getInputStream()));
                    temp = new StringBuilder();
                    while ((ln = br.readLine()) != null) {
                        temp.append(ln);
                    }
                    if(new JSONObject(temp.toString()).getJSONObject("status").getInt("id") == 3){ break;}
                    else if(new JSONObject(temp.toString()).getJSONObject("status").getInt("id") == 11){
                        JSONObject obj = new JSONObject(temp.toString());
                        String out = "Error:\n```";
                        out += obj.getString("stderr") + "```";
                        BotUtils.sendMessage(c, out);

                        return;
                    }
                    else if(new JSONObject(temp.toString()).getJSONObject("status").getInt("id") == 6){
                        JSONObject obj = new JSONObject(temp.toString());
                        String out = "Error:\n```";
                        out += obj.getString("compile_output") + "```";
                        BotUtils.sendMessage(c, out);

                        return;
                    }
                    else if(new JSONObject(temp.toString()).getJSONObject("status").getInt("id") == 5){
                        JSONObject obj = new JSONObject(temp.toString());
                        String out = "Error:\n```";
                        out += obj.getString("message") + "```";
                        BotUtils.sendMessage(c, out);

                        return;
                    }

                    dataReq = (HttpURLConnection) url.openConnection();
                    dataReq.setRequestMethod("GET");
                }


                JSONObject obj = new JSONObject(temp.toString());
                String out = "Standard Output:\n```";
                out += obj.getString("stdout") + "```";

                System.out.println(obj.getJSONObject("status").toString());

                BotUtils.sendMessage(c, out);
            }


        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
