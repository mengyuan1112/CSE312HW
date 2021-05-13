import java.net.ServerSocket;
import java.net.Socket;
import com.mongodb.*;


public class HelloWorld {

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(8000);

        System.out.println("The date server is running...");

        DBconnect();


        while (true) {
            Socket serverClient = server.accept();
            threadRun runner = new threadRun(serverClient);
            Thread thread = new Thread(runner);
            thread.start();

        }
    }
    private static void DBconnect(){
        System.out.println("this is not make scene for entire thing");
        MongoClient mongoClient = new MongoClient("mongo");
//        MongoDatabase database = mongo.
        DB db = mongoClient.getDB("cse312");
        System.out.println("connect to DB");
        DBCollection collection = db.getCollection("ws");
        try{
            System.out.println("mongo connect is successful");
        }catch (Exception e){

            System.out.println("unsuccessful insert");
        }

    }

}



