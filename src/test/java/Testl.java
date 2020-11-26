import java.io.IOException;
import java.net.Socket;

public class Testl {
    public static void main(String[] args) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket("www.csdncasdfq34w21342345345634567.com", 80);
            System.out.println("socket 连接成功");
        } catch (IOException e) {
            System.out.println("socket 连接失败");
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }
}