import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Base64;

import com.mongodb.*;


import org.bson.Document;

import java.security.SecureRandom;
import java.util.Arrays;

import java.util.*;

public class threadRun implements Runnable{
    public Socket socket;

    static ArrayList<String> commentArray = new ArrayList<String>();
    static HashMap<String, String> fileStore = new HashMap<>();
    static ArrayList<byte[]> chatHis = new ArrayList<>();
    static ArrayList<Socket> threads = new ArrayList<>();
    static ArrayList<String> userRegister = new ArrayList<>();
    static ArrayList<String> userLogin = new ArrayList<>();
    static String csrfToken = null;
    static String cookies = null;




    threadRun(Socket socket){
        this.socket = socket;
    }

    @Override
    public void run() {
        InputStream inputStream = null;
        try {
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ArrayList<String> store = new ArrayList<>();
        byte[] header = new byte[1024];
        try {
            inputStream.read(header);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String hd = new String(header, StandardCharsets.UTF_8);
        int contentSize = 0;
        String boundary = null;


        if (hd.contains("GET")) {
            String[] storage = hd.split("\r\n");
            store.add(storage[0]);
            for(String i: storage) {
                if (i.contains("Cookie")) store.add(i);
            }
        } else {
            String[] storage = hd.split("\r\n");
            store.add(storage[0]);
            for (String i : storage) {
                if (i.contains("Content-Length")) contentSize = getLength(i);
                if (i.contains("Content-Type")) boundary = getBoundary(i);
            }
            if (hd.contains("/image-upload")) {
                try {
                    if (!getPostBody(inputStream, contentSize, boundary)) {
                        String res = "HTTP/1.1 403 Forbidden\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 24\r\n\r\n" +
                                "The requested was reject";
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (hd.contains("/comment")) {
                try {
                    if (!parseBody(hd, boundary, contentSize, inputStream, false,false)) {
                        String res = "HTTP/1.1 403 Forbidden\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 24\r\n\r\n" +
                                "The requested was reject";
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    }
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            if (hd.contains("/register")) {
                try {
                    if(!parseBody(hd, boundary, contentSize, inputStream, true, false)) {
                        String res = "HTTP/1.1 403 Forbidden\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 242\r\n\r\n" +
                                "Sorry Register Fail\nyou need these requirement\n1. A minimum length of 8\n2. At least 1 lowercase character\n3. At least 1 uppercase character\n4. At least 1 number\n5. At least 1 special character\n6. Cannot include your userName in your password\n";
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    }
                }
                catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            }
            if(hd.contains("login")) {
                try{
                    if(!parseBody(hd, boundary, contentSize, inputStream, false, true)) {
                        String res = "HTTP/1.1 403 Forbidden\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: 35\r\n\r\n" +
                                "The account is wrong or not existed";
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    }
                }
                catch (IOException | NoSuchAlgorithmException e){
                    e.printStackTrace();
                }
            }
        }


        if (store.size() > 0) {
            String[] request = store.get(0).split(" ");
            String[] path = request[1].split("/");

            HashMap<String, String[]> queryMap = null;
            String html = null;
            if (store.get(0).contains("?")) {
                queryMap = parseQuery(path[1]);
                html = generateHTML(queryMap);
            }
            if (store.get(0).contains("GET")) {
                if (path.length == 0) {
                    String cookiesHeader = store.get(1);
                    String homeHtml;

                    cookies = UUID.randomUUID().toString();


                    MongoClient mongoClient = new MongoClient("mongo");
                    DB db = mongoClient.getDB("cse312");
                    DBCollection cookieCollection = db.getCollection("Cookies");
                    DBCollection loginCollection = db.getCollection("Login");
                    DBObject doc = new BasicDBObject("cookies","id="+cookies);
                    cookieCollection.insert(doc);

                    boolean find = false;

                    // check Login db with cookies db to welcome back




                    DBCursor cursor = cookieCollection.find();
                    System.out.println("this is cookies header: " + cookiesHeader);
                    while(cursor.hasNext()){
                        DBObject object = cursor.next();
                        String c = object.get("cookies").toString();
                        System.out.println(c);
                        if(cookiesHeader.contains(c)) {
                            find = true;
                        }
                        DBCursor loginCursor = loginCollection.find();
                        while(loginCursor.hasNext()) {
                            DBObject longinObject = loginCursor.next();
                            String uc = longinObject.get("cookies").toString();
                            System.out.println("uc is: " + uc);
                            System.out.println("c is: " + c);
                            if(c.contains(uc) && !cookiesHeader.contains(uc)) {
                                userLogin.add("welcome back: " + longinObject.get("username").toString());
                            }
                        }

                    }
                    System.out.println(userLogin);

                    if(find) homeHtml = generateHomePage(true);
                    else homeHtml = generateHomePage(false);


                    String res = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type:text/html\r\n" +
                            "Set-Cookie: id ="+cookies+"\r\n" +
                            "Content-Length: " + homeHtml.length() + "\r\n\r\n" +
                            homeHtml;
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (store.get(0).equals("GET /websocket HTTP/1.1")) {
                    String[] storage = hd.split("\r\n");
                    String connection = null;
                    String key = null;
                    String upgrade = null;
                    for (String i : storage) {
                        if (i.contains("Connection:")) connection = i;
                        if (i.contains("Upgrade:")) upgrade = i;
                        if (i.contains("Sec-WebSocket-Key")) {
                            String[] keySplit = i.split(": ");
                            key = keySplit[1];
                        }
                    }
                    threads.add(this.socket);
                    String res = null;
                    try {
                        res = "HTTP/1.1 101 Switching Protocols\r\n" +
                                connection + "\r\n" +
                                "Sec-WebSocket-Accept: " + java.util.Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8"))) + "\r\n"+
                                upgrade + "\r\n\r\n";
                    } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                        for(byte[] i: chatHis) {
                            socket.getOutputStream().write(i);
                        }

                        takeOutDb(socket);

                        while(true) {
                            webSocket(inputStream, socket.getOutputStream());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (store.get(0).equals("GET /hello HTTP/1.1")) {
                    String res = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type:text/plain\r\n" +
                            "Content-Length: 11\r\n\r\n" +
                            "Hello world";
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (store.get(0).equals("GET /hi HTTP/1.1")) {
                    String res = "HTTP/1.1 301 Moved Permanently\r\n" +
                            "Location: /hello";
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (path.length == 2 && path[1].contains("css")) {

                    try {
                        sendRespond(socket, "Content-Type:text/css", "src/main/java/style.css");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (path.length == 2 && path[1].equals("function.js")) {
                    try {
                        sendRespond(socket, "Content-Type:text/javascript", "src/main/java/function.js");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (path.length == 2 && path[1].equals("utf.txt")) {
                    try {
                        sendTextRespond(socket, "Content-Type:text/plain", "utf.txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (path.length == 3 && (path[1].equals("image") || path[1].equals("test"))) {
                    if (!path[2].contains(".jpg")) {
                        try {
                            sendPicRespond(socket, path[1] + "/" + path[2] + ".jpg");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            sendPicRespond(socket, path[1] + "/" + path[2]);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (queryMap != null && queryMap.size() > 0) {
                    try {
                        socket.getOutputStream().write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.getOutputStream().write("Content-Type:text/html\r\n".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.getOutputStream().write(("Content-Length: " + html.length()).getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.getOutputStream().write("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.getOutputStream().write(html.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }


                } else {
                    String res = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: 36\r\n\r\n" +
                            "The requested content does not exist";
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (store.get(0).contains("POST")) {
                if (store.get(0).equals("POST /comment HTTP/1.1")) {
                    String res = "HTTP/1.1 301 Moved Permanently\r\n" +
                            "Location: /";
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    String res = "HTTP/1.1 301 Moved Permanently\r\n" +
                            "Location: /";
                    try {
                        socket.getOutputStream().write(res.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static void sendRespond(Socket client, String contentType, String filePath) throws IOException {
        String sniff = "X-Content-Type-Options: nosniff";
        PrintWriter printWriter = new PrintWriter(client.getOutputStream());
        printWriter.println("HTTP/1.1 200 OK");
        printWriter.println(contentType + "; " + "charset=UTF-8");
        printWriter.println(sniff);

        File file= new File(filePath);
        BufferedReader fileBR = new BufferedReader(new FileReader(file));

        printWriter.println("Content-Length: " + (file.length()+1));
        printWriter.println("\r\n");
        String line = fileBR.readLine();
        while(line != null) {
            printWriter.println(line);
            line = fileBR.readLine();
        }

        fileBR.close();
        printWriter.close();

    }

    private static void sendTextRespond(Socket client, String contentType, String filePath) throws IOException {
        String sniff = "X-Content-Type-Options: nosniff";
        PrintWriter printWriter = new PrintWriter(client.getOutputStream());
        printWriter.println("HTTP/1.1 200 OK");
        printWriter.println(contentType + "; " + "charset=UTF-8");
        printWriter.println(sniff);

        File file= new File(filePath);
        BufferedReader fileBR = new BufferedReader(new FileReader(file));

        printWriter.println("Content-Length: " + (file.length()));
        printWriter.println();
        String line = fileBR.readLine();
        while(line != null) {
            printWriter.print(line);
            line = fileBR.readLine();
        }

        fileBR.close();
        printWriter.close();

    }


    private static void sendPicRespond(Socket client, String pictureName) throws IOException{
        String sniff = "X-Content-Type-Options: nosniff";
        client.getOutputStream().write("HTTP/1.1 200 OK\r\n".getBytes(StandardCharsets.UTF_8));
        client.getOutputStream().write("Content-Type:image/jpg\r\n".getBytes(StandardCharsets.UTF_8));
        File file = new File(pictureName);
        byte[] fileContent = Files.readAllBytes(file.toPath());
        client.getOutputStream().write(("Content-Length: " + (fileContent.length) + "\r\n").getBytes(StandardCharsets.UTF_8));
        client.getOutputStream().write(sniff.getBytes(StandardCharsets.UTF_8));
        client.getOutputStream().write("\r\n\r\n".getBytes(StandardCharsets.UTF_8));
        client.getOutputStream().write(fileContent);
    }


    private static String generateHTML(HashMap<String, String[]> map) {
        String imageTag = "";
        for(String i: map.get("pic")) {
            imageTag+="<img src=\"/image/" + i + "\">";
            imageTag+="\n";
        }
        String res = "<!DOCTYPE html>\n"+
                "<html lang=\"en\">\n" +
                "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title></title>\n" +
                "<h1> Welcome " + map.get("name")[0] + "</h1>\n"+
                "</head>\n" +
                "<body>\n" +
                "\n"+
                imageTag +
                "</body>\n" +
                "</html>";


        return res;


    }



    private static HashMap<String, String[]> parseQuery(String query) {
        HashMap<String,String[]> map = new HashMap<>();
        String[] pair = query.split("&");
        String picQuery;
        String nameQuery;
        if(pair[0].contains("name")) {
            picQuery = pair[1];
            nameQuery = pair[0];
        }
        else {
            picQuery = pair[0];
            nameQuery = pair[1];
        }

        String[] picValue = picQuery.split("=");
        String[] nameValue = nameQuery.split("=");

        map.put("name", new String[]{nameValue[1]});
        String[] pic = picValue[1].split("\\+");
        map.put("pic", pic);
        return map;
    }


    // HW 3
    private static String generateHomePage(boolean secondTime){
        String body = "";
        String register = "";
        String login = "";
        if(commentArray.size() > 0)
            for(int i = 0; i < commentArray.size();i++) {
                body+="<p>" + commentArray.get(i) + "</p>\n";
            }

        if(userRegister.size() > 0) register+="<p>" + userRegister.get(userRegister.size()-1) + "</p>\n";

        if(userLogin.size() > 0) login+="<h2>" + userLogin.get(userLogin.size()-1) + "</h2>\n";

        csrfToken = UUID.randomUUID().toString();
        String png = "";

        if(fileStore.size()!=0){
            for(Map.Entry<String, String> entry: fileStore.entrySet()){
                png+="<img src=/test/"+ entry.getKey()+">\n";
                png+="<p>" + entry.getValue() + "</p>\n";
            }
        }

        String html = null;

        if(!secondTime) html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title> This is Yuan's Page from CSE312</title>\n" +
                "    <h1> You come here for first time</h1>\n" +
                "    <button onclick=\"useless()\" type=\"button\">Don't Click Me! I am Useless! Waste your time</button>\n" +
                "    <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n" +
                "\n" +
                "</head>\n" +
                "<body onload= hello();>\n" +
                "<p id = \"useless\"></p>\n" +
                "\n" +
                "<img src=\"/image/flamingo\">\n" +
                "\n" +
                "\n" +
                "<form action=\"/register\" id=\"register-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "    <input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"text-form-userName\">Register UserName: </label>\n" +
                "    <input id=\"text-form-userName\" type=\"text\" name=\"username\"><br/>\n" +
                "    <label for=\"form-password\">Register Password: </label>\n" +
                "    <input id=\"form-password\" type=\"text\" name=\"password\">\n" +
                "    <input type=\"submit\" value=\"Register\">\n" +
                "</form>\n"+
                register+
                "\n" +
                "<form action=\"/login\" id=\"login-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"text-form-loginUser\">Login UserName: </label>\n" +
                "    <input id=\"text-form-loginUser\" type=\"text\" name=\"username\"><br/>\n" +
                "    <label for=\"form-loginPassword\">Login Password: </label>\n" +
                "    <input id=\"form-loginPassword\" type=\"text\" name=\"password\">\n" +
                "    <input type=\"submit\" value=\"Login\">\n" +
                "</form>\n" +
                login+
                "\n" +
                "<form action=\"/comment\" id=\"comment-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"text-form-name\">Name: </label>\n" +
                "    <input id=\"text-form-name\" type=\"text\" name=\"name\"><br/>\n" +
                "    <label for=\"form-comment\">Comment: </label>\n" +
                "    <input id=\"form-comment\" type=\"text\" name=\"comment\">\n" +
                "    <input type=\"submit\" value=\"Submit\">\n" +
                "</form>\n" +
                body+
                "\n" +
                "<p>below will be file upload</p>\n" +
                "\n" +
                "<form action=\"/image-upload\" id=\"image-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"form-file\">Image: </label>\n" +
                "    <input id=\"form-file\" type=\"file\" name=\"upload\">\n" +
                "    <br/>\n" +
                "    <label for=\"image-form-name\">Caption: </label>\n" +
                "    <input id=\"image-form-name\" type=\"text\" name=\"name\">\n" +
                "    <input type=\"submit\" value=\"Submit\">\n" +
                "</form>\n" +
                "\n" +
                png+
                "\n" +
                "\n" +
                "\n" +

                "<label for=\"chat-name\">Name: </label>\n"+
                "<input id=\"chat-name\" type=\"text\" name=\"name\">\n"+
                "<br/>\n"+
                "<label for=\"chat-comment\">Comment: </label>\n"+
                "<input id=\"chat-comment\" type=\"text\" name=\"comment\">\n"+
                "<button onclick=\"sendMessage()\">Chat</button>\n"+

                "<div id=\"chat\"></div> \n"+

                "<script src=\"function.js\"></script>\n" +
                "\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        else html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title> This is Yuan's Page from CSE312</title>\n" +
                "    <h1> You come here for second time</h1>\n" +
                "    <button onclick=\"useless()\" type=\"button\">Don't Click Me! I am Useless! Waste your time</button>\n" +
                "    <link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\">\n" +
                "\n" +
                "</head>\n" +
                "<body onload= hello();>\n" +
                "<p id = \"useless\"></p>\n" +
                "\n" +
                "<img src=\"/image/flamingo\">\n" +
                "\n" +
                "\n" +
                "<form action=\"/register\" id=\"register-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"text-form-userName\">Register UserName: </label>\n" +
                "    <input id=\"text-form-userName\" type=\"text\" name=\"username\"><br/>\n" +
                "    <label for=\"form-password\">Register Password: </label>\n" +
                "    <input id=\"form-password\" type=\"text\" name=\"password\">\n" +
                "    <input type=\"submit\" value=\"Register\">\n" +
                "</form>\n"+
                register+
                "\n" +
                "<form action=\"/login\" id=\"login-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"text-form-loginUser\">Login UserName: </label>\n" +
                "    <input id=\"text-form-loginUser\" type=\"text\" name=\"username\"><br/>\n" +
                "    <label for=\"form-loginPassword\">Login Password: </label>\n" +
                "    <input id=\"form-loginPassword\" type=\"text\" name=\"password\">\n" +
                "    <input type=\"submit\" value=\"Login\">\n" +
                "</form>\n" +
                login+
                "\n" +
                "<form action=\"/comment\" id=\"comment-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"text-form-name\">Name: </label>\n" +
                "    <input id=\"text-form-name\" type=\"text\" name=\"name\"><br/>\n" +
                "    <label for=\"form-comment\">Comment: </label>\n" +
                "    <input id=\"form-comment\" type=\"text\" name=\"comment\">\n" +
                "    <input type=\"submit\" value=\"Submit\">\n" +
                "</form>\n" +
                body+
                "\n" +
                "<p>below will be file upload</p>\n" +
                "\n" +
                "<form action=\"/image-upload\" id=\"image-form\" method=\"post\" enctype=\"multipart/form-data\">\n" +
                "<input value="+csrfToken+" name=\"xsrf_token\" hidden>\n"+
                "    <label for=\"form-file\">Image: </label>\n" +
                "    <input id=\"form-file\" type=\"file\" name=\"upload\">\n" +
                "    <br/>\n" +
                "    <label for=\"image-form-name\">Caption: </label>\n" +
                "    <input id=\"image-form-name\" type=\"text\" name=\"name\">\n" +
                "    <input type=\"submit\" value=\"Submit\">\n" +
                "</form>\n" +
                "\n" +
                png+
                "\n" +
                "\n" +
                "\n" +

                "<label for=\"chat-name\">Name: </label>\n"+
                "<input id=\"chat-name\" type=\"text\" name=\"name\">\n"+
                "<br/>\n"+
                "<label for=\"chat-comment\">Comment: </label>\n"+
                "<input id=\"chat-comment\" type=\"text\" name=\"comment\">\n"+
                "<button onclick=\"sendMessage()\">Chat</button>\n"+

                "<div id=\"chat\"></div> \n"+

                "<script src=\"function.js\"></script>\n" +
                "\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        return html;
    }

    private static int getLength(String str){
        String[] contentLength = str.split(" ");
        return Integer.parseInt(contentLength[1]);
    }

    private static String getBoundary(String str){
        String[] split2Bounday = str.split("=");
        return split2Bounday[1];
    }

    private static boolean getPostBody(InputStream Is, int n, String boundary) throws IOException {
        ByteArrayOutputStream baot = new ByteArrayOutputStream();
        while(baot.size() < n) {
            byte[] b = new byte[1024];
            int total = Is.read(b,0,1024);
            for(byte i : b){
                baot.write(i);
            }
        }

        byte[] a = baot.toByteArray();


        List<byte[]> arr = tokens(a, ("--"+boundary).getBytes());

        List<byte[]> name = tokens(arr.get(2), "\r\n\r\n".getBytes());

        List<byte[]> csrf = tokens(arr.get(0),"\r\n\r\n".getBytes());


        String curCsrf = new String(Arrays.copyOfRange(csrf.get(1),0,csrf.get(1).length-2),StandardCharsets.UTF_8);


        List<byte[]> filearr = tokens(arr.get(1), "\r\n\r\n".getBytes());

        if(!curCsrf.equals(csrfToken)) {
            return false;
        }

        if(!checkFile(new String(filearr.get(0)))){
            return false;
        }


        String escape = escapeHtml(new String(name.get(1), StandardCharsets.UTF_8).substring(0,new String(name.get(1), StandardCharsets.UTF_8).length()-2));


        File file = new File("test/"+fileStore.size()+".jpg");
        fileStore.put(String.valueOf(fileStore.size()),escape);
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(Arrays.copyOfRange(filearr.get(1),0,filearr.get(1).length));
        fos.close();
        return true;
    }


    private static boolean parseBody(String hd,String boundary, int n, InputStream Is, boolean register, boolean login) throws IOException, NoSuchAlgorithmException {

        ByteArrayOutputStream baot = new ByteArrayOutputStream();
        while(baot.size() < n) {
            byte[] b = new byte[1024];
            int total = Is.read(b,0,1024);
            for(byte i : b){
                baot.write(i);
            }
        }

        hd = hd + new String(baot.toByteArray(),StandardCharsets.UTF_8);

        String[] splitHeader = hd.split("--"+boundary);


        String[] csrf = splitHeader[1].split("\r\n\r\n");
        String[] to = csrf[1].split("\r\n");

        if(!to[0].equals(csrfToken)){
            return false;
        }
        String[] nameBody = splitHeader[2].split("\r\n\r\n");

        String[] name = nameBody[1].split("\r\n");

        String[] commentBody = splitHeader[3].split("\r\n\r\n");
        String[] comment = commentBody[1].split("\r\n");


        if(register) {
            String userName = name[0];
            String password = comment[0];

            if(!passwordCheck(password, userName)) {
                return false;
            }

            byte[] salt = createSalt();
            byte[] saltedPwd = hashPwd(salt, password);
            String saltedPwdDB = Base64.encodeBase64String(saltedPwd);
            String saltDB = Base64.encodeBase64String(salt);

//            User user = new User(userName, password, saltDB, saltedPwdDB, cookies);

            MongoClient mongoClient = new MongoClient("mongo");
            DB db = mongoClient.getDB("cse312");
            DBCollection hwCollection = db.getCollection("Register");

            DBObject doc = new BasicDBObject("username", userName).append("salt",saltDB)
                    .append("saltedPwd",saltedPwdDB);
            try {
                hwCollection.insert(doc);
                System.out.println("Successful insert User");

                userRegister.add("Register success");
            }
            catch (Exception e){
                System.out.println("error on: "+ e);
                System.out.println("unsuccessful add User");
            }

//            System.out.println("Thank you " + escapeHtml(userName) + " for your comment of " + escapeHtml(password));
            return true;
        }

        if(login) {
            String userName = name[0];
            String password = comment[0];

            MongoClient mongoClient = new MongoClient("mongo");
            DB db = mongoClient.getDB("cse312");
            DBCollection collection = db.getCollection("Register");

            DBCursor cursor = collection.find();
            while(cursor.hasNext()) {
                DBObject obj = cursor.next();
                String dbUser = obj.get("username").toString();
                String dbSalt = obj.get("salt").toString();
                String dbSaltedPwd = obj.get("saltedPwd").toString();
                byte[] saltByte = Base64.decodeBase64(dbSalt);
                byte[] saltedPwdByte = Base64.decodeBase64(dbSaltedPwd);
                byte[] userInputPwd = hashPwd(saltByte, password);

                if(dbUser.equals(userName) && Arrays.equals(saltedPwdByte, userInputPwd)) {
                    userLogin.add("You logged in as " + userName);
                    DBCollection LoginCollection = db.getCollection("Login");
                    DBObject LoginInfo = new BasicDBObject("username", userName).append("cookies",cookies);
                    LoginCollection.insert(LoginInfo);
                    return true;
                }

            }
            userLogin.add("login failed");
            return true;
        }


        commentArray.add("Thank you " + escapeHtml(name[0]) + " for your comment of " + escapeHtml(comment[0]));
        return true;
    }



    private static List<byte[]> tokens(byte[] array, byte[] delimiter) {
        List<byte[]> byteArrays = new LinkedList<byte[]>();
        if (delimiter.length == 0)
        {
            return byteArrays;
        }
        int begin = 0;

        outer: for (int i = 0; i < array.length - delimiter.length + 1; i++)
        {
            for (int j = 0; j < delimiter.length; j++)
            {
                if (array[i + j] != delimiter[j])
                {
                    continue outer;
                }
            }


            if (begin != i)
                byteArrays.add(Arrays.copyOfRange(array, begin, i));
            begin = i + delimiter.length;
        }


        if (begin != array.length)
            byteArrays.add(Arrays.copyOfRange(array, begin, array.length));

        return byteArrays;
    }


    private static String escapeHtml(String str){
        StringBuilder res = new StringBuilder();

        for(int i = 0; i < str.length(); i++) {
            char cur = str.charAt(i);
            if(cur == '<'){
                res.append("&lt");
            }
            else if(cur == '>'){
                res.append("&gt");
            }
            else if(cur == '&'){
                res.append("&amp");
            }
            else {
                res.append(cur);
            }
        }

        return res.toString();
    }

    private static Boolean checkFile(String str){
        String[] fileName = str.split("\r\n");
        if(!fileName[1].substring(fileName[1].length()-4,fileName[1].length()-1).equals("jpg")){
            return false;
        }
        return true;
    }


    /** HW 3 */
    private static void webSocket(InputStream inputStream, OutputStream outputStream) throws IOException {
        byte[] frame = new byte[1024];
        inputStream.read(frame);
        List<String> bitString = new ArrayList<>();

        for(int i = 0; i < frame.length;i++) {
//            if(frame[i] == 0) break;
            bitString.add(toBit(frame[i]));
        }
        // op code
        int opCode = bitString.get(0).charAt(7) - '0';
        int mask = bitString.get(1).charAt(0) - '0';



        int payloadLength = Integer.parseInt(bitString.get(1).substring(1,8),2);

        //.substring(2,8)
        if(payloadLength < 126) {
            String maskkey = bitString.get(2) + bitString.get(3) + bitString.get(4) + bitString.get(5);
            StringBuilder sb = new StringBuilder();
            String payload = "";
            for (int i = 6; i < payloadLength + 6; i++) {
                sb.append(bitString.get(i));
                if (sb.length() == maskkey.length()) {
                    for (int j = 0; j < sb.length(); j++) {
                        if (sb.charAt(j) == maskkey.charAt(j)) {
                            payload += "0";
                        } else payload += "1";
                    }
                    sb = new StringBuilder();
                }
            }
            if (sb.length() != 0) {
                for (int j = 0; j < sb.length(); j++) {
                    if (sb.charAt(j) == maskkey.charAt(j)) {
                        payload += "0";
                    } else payload += "1";
                }
            }



            if (payload.length() != 0) {
                byte[] text = new BigInteger(payload, 2).toByteArray();
                String str = new String(text, StandardCharsets.UTF_8);
                String completeStr = escapeHtml(str);
                byte[] bval = completeStr.getBytes();

                ByteArrayOutputStream returnByte = new ByteArrayOutputStream();
                returnByte.write(129); // fin
                returnByte.write((bval.length));
                for (byte i : bval) {
                    returnByte.write(i);
                }
                

                addDB(returnByte.toByteArray());

                for (Socket s : threads) {
                    s.getOutputStream().write(returnByte.toByteArray());
                }
            }
        }

        else {
            int length = Integer.parseInt(bitString.get(2) + bitString.get(3), 2);

            String maskkey = bitString.get(4) + bitString.get(5) + bitString.get(6) + bitString.get(7);
            StringBuilder sb = new StringBuilder();
            String payload = "";
            for (int i = 8; i < length + 8; i++) {
                sb.append(bitString.get(i));
                if (sb.length() == maskkey.length()) {
                    for (int j = 0; j < sb.length(); j++) {
                        if (sb.charAt(j) == maskkey.charAt(j)) {
                            payload += "0";
                        } else payload += "1";
                    }
                    sb = new StringBuilder();
                }
            }
            if (sb.length() != 0) {
                for (int j = 0; j < sb.length(); j++) {
                    if (sb.charAt(j) == maskkey.charAt(j)) {
                        payload += "0";
                    } else payload += "1";
                }
            }




            if (payload.length() != 0) {
                byte[] text = new BigInteger(payload, 2).toByteArray();
                String str = new String(text, StandardCharsets.UTF_8);
                String completeStr = escapeHtml(str);
                byte[] bval = completeStr.getBytes();

                ByteArrayOutputStream returnByte = new ByteArrayOutputStream();
                returnByte.write(129); //
                returnByte.write(126);
                returnByte.write(bval.length >> 8);
                returnByte.write(bval.length&255);


                for (byte i : bval) {
                    returnByte.write(i);
                }
                addDB(returnByte.toByteArray());

                for (Socket s : threads) {
                    s.getOutputStream().write(returnByte.toByteArray());
                }
            }
        }


    }

    private static String toBit(byte b) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < 8; i++) {
            sb.append((int)(b >> (8-(i+1)) & 0x0001));
        }
        return sb.toString();
    }


    private static void addDB(byte[] frame){



        String str =Base64.encodeBase64String(frame);

        MongoClient mongoClient = new MongoClient("mongo");
        DB db = mongoClient.getDB("cse312");
        DBCollection collection = db.getCollection("ws");
        DBObject doc = new BasicDBObject("his", str);
        try{
            collection.insert(doc);

            System.out.println("successful");
        }catch (Exception e){
            System.out.println("the error is : "+ e);
            System.out.println("unsuccessful insert");
        }
    }
    private static void takeOutDb(Socket socket) throws IOException {
        MongoClient mongoClient = new MongoClient("mongo");
        DB db = mongoClient.getDB("cse312");
        DBCollection collection = db.getCollection("ws");


        DBCursor cursor = collection.find();
        while(cursor.hasNext()){
            DBObject obj = cursor.next();
            String cur = obj.get("his").toString();



            byte[] frame = Base64.decodeBase64(cur);

            socket.getOutputStream().write(frame);

        }
    }

    private static byte[] hashPwd(byte[] salt, String password) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        digest.update(salt);
        byte[] hash = digest.digest(password.getBytes());
        return hash;
    }

    private static byte[] createSalt() {
        byte[] bytes = new byte[10];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    private static boolean passwordCheck(String password, String userName) {
        boolean length = false;
        boolean lowerCase = false;
        boolean upperCase = false;
        boolean number = false;
        boolean special = false;
        boolean usernameInPassword = false;
        if(password.length() >= 8) length = true;
        for(int i =0; i< password.length(); i++) {
            if(Character.isLowerCase(password.charAt(i))) lowerCase = true;
            if(Character.isUpperCase(password.charAt(i))) upperCase = true;
            if(Character.isDigit(password.charAt(i))) number = true;
        }
        if(!password.contains(userName)) usernameInPassword = true;
        if(password.contains("~")||password.contains("!")|| password.contains("@")||password.contains("#")||
                password.contains("$")||password.contains("%")||password.contains("^")||password.contains("&")||
                password.contains("*")||password.contains("(")||password.contains(")")||password.contains("_")||password.contains(".")) special = true;

        if(length && lowerCase && upperCase && number && special && usernameInPassword) return true;
        return false;
    }








}
