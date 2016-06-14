import java.net.InetAddress;
import java.net.UnknownHostException;


public class Tabla{
	private int coste=1;
	private InetAddress direccionIP;
	private InetAddress mascara;
	private InetAddress nexthop;
	private long time;
	private boolean sub;



 	Tabla(InetAddress direccionIP, InetAddress mascara) throws UnknownHostException{
 		this.coste=1;
 		this.direccionIP=direccionIP;
 		this.mascara=mascara;
 		this.nexthop=InetAddress.getByName("0.0.0.0"); //Con el getByName("0") nos devuelve la direccion IP del host propio
 		this.time= System.currentTimeMillis(); //Nos devuelve un long que representa el tiempo actual en ms.
 		this.sub=true;
 	}
 	Tabla(int coste, InetAddress direccionIP, InetAddress mascara, InetAddress nexthop){
 		this.coste=coste;
 		this.direccionIP=direccionIP;
 		this.mascara=mascara;
 		this.nexthop=nexthop;
 		this.time=System.currentTimeMillis();
 		this.sub=false;
 	}
 	public InetAddress getIP(){
 		return direccionIP;
 	}
 	public InetAddress getNextHop(){
 		return nexthop;
 	}
 	public InetAddress getMascara(){
 		return mascara;
 	}
 	public int getCoste(){
 		return coste;
 	}
 	public boolean getSub(){
 		return sub;
 	}
 	public void setTime(long time){
 		this.time=time;
 	}
 	public void setCoste(int coste){
 		this.coste=coste;
 	}
 	public boolean comprobareliminar(){
 		return(System.currentTimeMillis() - this.time) >100000;
 	}
 	public boolean comprobarcambio(){
 		return(System.currentTimeMillis() - this.time)>60000;
 	}
 	public String toString(){
 		return "[Direccion Ip= " +direccionIP.getHostAddress() +",Mascara= " +mascara.getHostAddress() + ", Next Hop= " +nexthop.getHostAddress() +",Coste= "+coste +"]";
 	}

 }