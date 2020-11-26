import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    public static void main(String[] args) throws Exception {
        // try {
        //     Socket socket = new Socket("localhost", 8088);
        //
        //     OutputStream outputStream = socket.getOutputStream();
        //     InputStream inputStream = socket.getInputStream();
        //
        //     // 输出开始
        //     ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        //     String strA = "服务端你好 A\n";
        //     String strB = "服务端你好 B\n";
        //     String strC = "服务端你好 C\n";
        //     int allStrByteLength = (strA + strB + strC).getBytes().length;
        //     objectOutputStream.write(allStrByteLength);
        //     objectOutputStream.flush();
        //
        //     objectOutputStream.write(strA.getBytes());
        //     objectOutputStream.write(strB.getBytes());
        //     objectOutputStream.write(strC.getBytes());
        //     objectOutputStream.flush();
        //     // 输出结束
        //
        //     // 输入开始
        //     ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        //     int byteLength = objectInputStream.readInt();
        //     byte[] byteArray = new byte[byteLength];
        //     objectInputStream.readFully(byteArray);
        //     String newString = new String(byteArray);
        //     System.out.println(newString);
        //     // 输入结束
        //
        //     // 输出开始
        //     strA = "服务端你好 D\n";
        //     strB = "服务端你好 E\n";
        //     strC = "服务端你好 F\n";
        //     allStrByteLength = (strA + strB + strC).getBytes().length;
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
        //     objectOutputStream.close();
        //     outputStream.close();
        //     socket.close();
        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


        Socket socket = new Socket("localhost", 8080);

        OutputStream outputStream = socket.getOutputStream();
        InputStream inputStream = socket.getInputStream();

        // 输出开始
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        String strA = "服务端你好 A\n";
        int allStrByteLength = (strA).getBytes().length;
        objectOutputStream.write(allStrByteLength);
        objectOutputStream.flush();
        objectOutputStream.write(strA.getBytes());
        objectOutputStream.flush();

        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        int byteLength = objectInputStream.readInt();
        byte[] byteArray = new byte[byteLength];
        objectInputStream.readFully(byteArray);
        System.out.println(new String(byteArray));

        objectOutputStream.close();
        outputStream.close();
        socket.close();

    }
}


