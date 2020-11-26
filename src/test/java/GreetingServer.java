import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class GreetingServer {

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8080);

        while (true) {
            Socket server = serverSocket.accept();
            DataInputStream in = new DataInputStream(server.getInputStream());
            DataOutputStream out = new DataOutputStream(server.getOutputStream());

            // 读取消息
            System.out.println("收到消息：" + in.readUTF());

            // 回写响应
            System.out.println("发送消息：");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String result = reader.readLine();
            if (result != null && result != "") {
                out.writeUTF(result);
            }

            server.close();
        }

    }
}