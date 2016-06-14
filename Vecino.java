import java.net.InetAddress;
import java.net.UnknownHostException;




public class Vecino{
	private int puerto;
	private InetAddress ip;



	Vecino(InetAddress ip, int puerto){
		this.ip=ip;
		this.puerto=puerto;
	}
	public int getPort(){
		return puerto;
	}
	public InetAddress getIP(){
 		return ip;
 	}
}