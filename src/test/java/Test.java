import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * @Author: wanghz
 * @Date: 2020/11/21 5:26 PM
 */
public class Test {

    public static void main(String[] args) throws SocketException {

        // NetworkInterface类：表示一个由名称和分配给此接口的IP地址列表组成的网络接口，也就是 NetworkInterface 类包含网络接口名称与IP地址列表。
        // 该类提供访问网卡设备的相关信息，如可以获取网卡名称、 IP 地址和子网掩码等。
        // 以下是返回此机器上的所有网络接口：
        Enumeration<NetworkInterface> items = NetworkInterface.getNetworkInterfaces();
        while (items.hasMoreElements()) {
            NetworkInterface item = items.nextElement();
            System.out.println("getName获得网络设备名称＝" + item.getName());
            System.out.println("getDisplayName获得网络设备显示名称＝" + item.getDisplayName());
            System.out.println("getIndex获得网络接口的索引＝"+ item.getIndex()) ;
            System.out.println("isUp是否已经开启并运行＝" + item.isUp());
            System.out.println("isLoopback是否为回调接口＝" + item.isLoopback());
            System.out.println("getMTU获得最大传输单元=" + item.getMTU());
            System.out.println("getHardwareAddress获取硬件地址" + item.getHardwareAddress());
            System.out.println("isVirtual是否为虚拟接口：" + item.isVirtual());
            System.out.println("父接口：" + item.getParent());
            System.out.println("是否支持多播" + item.supportsMulticast());
            System.out.println("" + item.isPointToPoint());
            System.out.println("获取虚拟子接口：");
            while (item.getSubInterfaces().hasMoreElements()) {
                NetworkInterface sub = item.getSubInterfaces().nextElement();
                System.out.println("网络设备名称：" + sub.getName() + "，是否是虚拟网络：" + sub.isVirtual());
            }


            Enumeration<InetAddress> enumeration = item.getInetAddresses();


            System.out.println("getInterfaceAddresses获取绑定IP地址列表：");
            for (InterfaceAddress interfaceAddress : item.getInterfaceAddresses()) {
                InetAddress inetAddress = interfaceAddress.getAddress();
                // inetAddress.ge
                System.out.println("\thostName=" + inetAddress.getHostName() + "，hostAddress=" + inetAddress.getHostAddress() + "，canonicalHostName=" + inetAddress.getCanonicalHostName());
            }

            System.out.println();
        }

    }
}


