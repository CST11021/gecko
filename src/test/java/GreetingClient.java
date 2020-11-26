import java.net.*;
import java.io.*;

public class GreetingClient {

    public static void main(String[] args) throws Exception {

        Socket client = new Socket("127.0.0.1", 8080);

        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        DataInputStream in = new DataInputStream(client.getInputStream());

        while (true) {
            // 发送消息
            System.out.print("发送消息：");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String message = reader.readLine();
            if (message != null && message != "") {
                out.writeUTF(message);
            }

            // 收到响应
            System.out.println("收到消息：" + in.readUTF());
        }

    }

}