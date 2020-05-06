import discord4j.core.object.entity.MessageChannel;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
            URL url = new URL("https://api.judge0.com/submissions/?base64_encoded=true");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");

            String payload = "{\"source_code\":\"" + Base64.getMimeEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8)) + "\",\"language_id\":" + langID +
                    ",\"base64_encoded\": true}";

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

                url = new URL("https://api.judge0.com/submissions/" + token + "?base64_encoded=true");
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
                        out += new String(Base64.getMimeDecoder().decode(obj.getString("stderr"))) + "```";
                        BotUtils.sendMessage(c, out);

                        return;
                    }
                    else if(new JSONObject(temp.toString()).getJSONObject("status").getInt("id") == 6){
                        JSONObject obj = new JSONObject(temp.toString());
                        String out = "Error:\n```";
                        out += new String(Base64.getMimeDecoder().decode(obj.getString("compile_output"))) + "```";
                        BotUtils.sendMessage(c, out);

                        return;
                    }
                    else if(new JSONObject(temp.toString()).getJSONObject("status").getInt("id") == 5){
                        JSONObject obj = new JSONObject(temp.toString());
                        String out = "Error:\n```";
                        out += new String(Base64.getMimeDecoder().decode(obj.getString("message"))) + "```";
                        BotUtils.sendMessage(c, out);

                        return;
                    }

                    dataReq = (HttpURLConnection) url.openConnection();
                    dataReq.setRequestMethod("GET");
                }

                String out = "```No data present```";

                JSONObject obj = new JSONObject(temp.toString());
                if(obj.get("stderr").getClass().equals(String.class)){
                    out = "Error:\n```";
                    out += new String(Base64.getMimeDecoder().decode(obj.getString("stderr"))) + "```";
                }
                else if (obj.get("stdout").getClass().equals(String.class)){
                    out = "Standard Output:\n```";
                    out += new String(Base64.getMimeDecoder().decode(obj.getString("stdout"))) + "```";
                }

                System.out.println(obj.getJSONObject("status").toString());

                BotUtils.sendMessage(c, out);
            }
            else{
                System.out.println(con.getResponseCode());
            }


        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
