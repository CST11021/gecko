import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static void main(String[] args) throws Exception {
        // try {
        //     ServerSocket serverSocket = new ServerSocket(8088);
        //     Socket socket = serverSocket.accept();
        //
        //     // 输入开始
        //     InputStream inputStream = socket.getInputStream();
        //     ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        //     int byteLength = objectInputStream.readInt();
        //     byte[] byteArray = new byte[byteLength];
        //     objectInputStream.readFully(byteArray);
        //     String newString = new String(byteArray);
        //     System.out.println(newString);
        //     // 输入结束
        //
        //     // 输出开始
        //     OutputStream outputStream = socket.getOutputStream();
        //     String strA = "客户端你好A\n";
        //     String strB = "客户端你好B\n";
        //     String strC = "客戸端你好C\n";
        //     int allStrByteLength = (strA + strB + strC).getBytes().length;
        //     ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        //     objectOutputStream.writeInt(allStrByteLength);
        //     objectOutputStream.flush();
        //     objectOutputStream.write(strA.getBytes());
        //     objectOutputStream.write(strB.getBytes());
        //     objectOutputStream.write(strC.getBytes());
        //     objectOutputStream.flush();
        //     // 输出结束
        //
        //     // 输入开始
        //     byteLength = objectInputStream.readInt();
        //     byteArray = new byte[byteLength];
        //     objectInputStream.readFully(byteArray);
        //     newString = new String(byteArray);
        //     System.out.println(newString);
        //     // 输入结束
        //
        //     // 输出开始.
        //     strA = "客户端你好D\n";
        //     strB = "客户端你好E\n";
        //     strC = "客戸端休好F\n";
        //     allStrByteLength = (strA + strB + strC).getBytes().length;
        //     objectOutputStream.writeInt(allStrByteLength);
        //     objectOutputStream.flush();
        //     objectOutputStream.write(strA.getBytes());
        //     objectOutputStream.write(strB.getBytes());
        //     objectOutputStream.write(strC.getBytes());
        //     objectOutputStream.flush();
        //     // 输出结束
        //
        //     inputStream.close();
        //     socket.close();
        //     serverSocket.close();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


        ServerSocket serverSocket = new ServerSocket(8080);
        Socket socket = serverSocket.accept();

        // 输入开始
        InputStream inputStream = socket.getInputStream();
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        int byteLength = objectInputStream.readInt();
        byte[] byteArray = new byte[byteLength];
        objectInputStream.readFully(byteArray);
        System.out.println(new String(byteArray));
        // 输入结束

        // 输出开始
        OutputStream outputStream = socket.getOutputStream();
        String strA = "客户端你好A\n";
        int allStrByteLength = (strA).getBytes().length;
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeInt(allStrByteLength);
        objectOutputStream.flush();
        objectOutputStream.write(strA.getBytes());
        objectOutputStream.flush();
        // 输出结束

        inputStream.close();
        socket.close();
        serverSocket.close();

    }
}


