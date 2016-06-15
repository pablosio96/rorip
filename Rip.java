import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.Inet4Address;
import java.util.Scanner;
/**
 * 
 * @author Pablo Sio Fernandez 35580001J
 * Grupo: redord24
 *
 */
public class Rip{
	
	private static ArrayList<Vecino> vecinos = new ArrayList<Vecino>();
	private static ArrayList<Tabla> lista= new ArrayList<Tabla>();
	private static String password=null;
	private static int tamano_pass=0;

	/**
	 * function principal
	 * @param args
	 * @throws UnknownHostException
	 * @throws SocketException
	 */


	public static void main(String args[]) throws UnknownHostException,SocketException{
	InetAddress ip=null;
	int puerto=0;


	 Scanner sc = new Scanner(System.in);
	 System.out.print("Introduzca la password: "); 
	 password=sc.nextLine();
		if(args.length >0){
			try{
			String[] separado=args[0].split(":"); 
			 ip= InetAddress.getByName(separado[0]);
			 puerto= Integer.parseInt(separado[1]);
			}
			catch(Exception e){
				puerto=5512;
				ip=InetAddress.getByName(args[0]);
			}
		}
		else{
			puerto=5512;
			Enumeration<InetAddress> eth0_IP= null;
		try{
			eth0_IP=NetworkInterface.getByName("en0").getInetAddresses();
		}catch(SocketException e){
			System.out.println("Error al obtener IP's de eth0");
		}				
		while(eth0_IP.hasMoreElements()){
			InetAddress ip_aux = eth0_IP.nextElement();
			if(ip_aux instanceof Inet4Address){
				ip=ip_aux;				
			}
		}	
		}
		LeerFichero(ip);
		

		
		DatagramSocket server = new DatagramSocket(puerto,ip); // Este es el puerto en el que va a escuchar el server.
		long time1,time2;
		while(true){
			time1=System.currentTimeMillis(); //Esta es la hora en este instante
			try{
				server.setSoTimeout(10000);//Nos pone el timeout a 10s
				byte[] buffer;
				while(true){ //bucle de recepcion de paquetes
					buffer=new byte[504];
					DatagramPacket paquete= new DatagramPacket(buffer,buffer.length);
					server.receive(paquete);
					if(paquete != null){
						procesarpaquete(paquete);
						time2=10000+(int)Math.floor(Math.random()*1666.667);
							server.setSoTimeout((int)time2);		
					}
						
				}			
			}
			catch(SocketTimeoutException e){  //Si el time vence, entonces enviamos la tabla pq pasaron los 10s o llego un paquete
				actualizarlista();
				enviarlista();
			}
			catch(IOException e){
				e.printStackTrace();
				server.close();
			}
		}


		
	}



	/**
	 * metodo para leer el fichero
	 * @param ip
	 * @throws UnknownHostException
	 */

	public static void LeerFichero(InetAddress ip) throws UnknownHostException{
		String ip_aux= ip.getHostAddress();
		File archivo= new File("ripconf-" +ip_aux +".topo");
		BufferedReader buf;
		String line, red, ip_vecino="";
		int len,port;
		
		try {
			lista.add(new Tabla(0,ip,getMascara(32),ip)); // entrada en la tabla correspondiente al propio nodo
			if(archivo.exists()){
				buf = new BufferedReader(new FileReader(archivo));
				line = buf.readLine();
				
				do{
					if(line.contains("/")){ //subredes
						red = line.substring(0, line.indexOf("/")).trim();
						len = Integer.parseInt(line.substring(line.indexOf("/")+1));
						lista.add(new Tabla(InetAddress.getByName(red),getMascara(len))); //se anade a la tabla las entradas conrrespondientes a las subredes del fichero de configuracion
					}else{
					if(line.contains(":")){ //vecinos
						ip_vecino= line.substring(0, line.indexOf(":")).trim();
						port=Integer.parseInt(line.substring(line.indexOf(":")+1));
						vecinos.add(new Vecino(InetAddress.getByName(ip_vecino),port)); //incorporamos a la lista vecinos un nuevo vecino
						
					}
						

					else{
						ip_vecino=line;
						
						port=5512; //si no hay puerto se pone por defecto el 5512 
						vecinos.add(new Vecino(InetAddress.getByName(ip_vecino),port)); //y lo anadimos a la lista vecinos
					}
				}
					line = buf.readLine();
				}while(line != null);
				buf.close();
				
			}else{
				System.out.println("Error al abrir el archivo"); //notificamos por pantalla si hubo algun error en la lectura del fichero
				System.exit(-1);
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * metodo para enviar la tabla a los vecinos
	 */
	public static void enviarlista(){
		imprimirlista(); //sacamos por pantalla la tabla
		
		DatagramPacket paquete;
		byte[] buffer= null;
		byte cero=0, dos=2;
		byte unos = (byte)255;
		try{
			byte[] passbyte = password.getBytes("UTF-8"); 
			tamano_pass=passbyte.length;
			int cont =0;

			DatagramSocket client = new DatagramSocket();
			
			for(Vecino vec:vecinos){
				InetAddress ip_vecino= vec.getIP();
					buffer=concat(  ByteBuffer.allocate(1).put(dos).array(), //comando
									ByteBuffer.allocate(1).put(dos).array(), //version
									ByteBuffer.allocate(2).put(cero).array(),//unused
									ByteBuffer.allocate(1).put(unos).array(),//0xFFFF
									ByteBuffer.allocate(1).put(unos).array(),
									ByteBuffer.allocate(1).put(cero).array(),//Authentication Type
									ByteBuffer.allocate(1).put(dos).array(),//Authentication Type
									ByteBuffer.allocate(16).put(cero).array());//Authentication
							if(tamano_pass >0){
								for(int i=8; i<8+tamano_pass;i++){
									buffer[i]=passbyte[cont];
									cont++;
								}
							}
					
				for(Tabla tabla_vecino:lista){
					if(!(tabla_vecino.getIP().equals(ip_vecino) || tabla_vecino.getNextHop().equals(ip_vecino))){
								buffer = concat(buffer,
									ByteBuffer.allocate(1).put(cero).array(),//AddressFamilyIdentifier
									ByteBuffer.allocate(1).put(dos).array(), //AddressFamilyIdentifier
									ByteBuffer.allocate(2).put(cero).array(),//RouteTag
									ByteBuffer.allocate(4).put(tabla_vecino.getIP().getAddress()).array(), //IP Address
									ByteBuffer.allocate(4).put(tabla_vecino.getMascara().getAddress()).array(), // subnet mask
									ByteBuffer.allocate(4).put(InetAddress.getByName("0.0.0.0").getAddress()).array(), // next hop
									ByteBuffer.allocate(4).putInt(tabla_vecino.getCoste()).array());//metric			
					}
					
					
						
				}

				if(buffer != null){
					paquete= new DatagramPacket(buffer,buffer.length,ip_vecino,vec.getPort());
					byte[] bytes = paquete.getData();
					
					System.out.println("Enviando paquete al vecino: " +ip_vecino.getHostAddress() +" en el puerto: "+vec.getPort());
					//nos indica a que host le esta enviando la tabla y en que puerto lo esta recibiendo
									

					client.send(paquete);
				}
			}
			client.close();
		}
		catch(SocketException e){
			e.printStackTrace();
		}
		catch(IOException e){
			e.printStackTrace();
		}
		catch(Exception e){
		e.printStackTrace();
		}
	}



	/**
	 * metodo para procesar el paquete
	 * @param paquete
	 * @throws UnknownHostException
	 */
	public static synchronized void procesarpaquete(DatagramPacket paquete) throws UnknownHostException{
		ArrayList<Tabla> lista_aux= new ArrayList<Tabla>();
		InetAddress ip_client = paquete.getAddress();
		InetAddress ip,mascara;
		byte[] bytes = paquete.getData();
		byte[] buffer;
		byte[] pass_recibida= new byte[tamano_pass];
		int i, coste;
		Tabla t;
		String pass="";
		if(tamano_pass !=0){
			for(int j=0; j<tamano_pass;j++){
				pass_recibida[j]=bytes[j+8];
			}
			for(int k = 0; k < tamano_pass; k++)
	    	{
	        	pass += (char)pass_recibida[k];

	    	}
	    	if(!pass.equals(password))
	    		return;
		}else{
			if(bytes[8]!= 0)
				return;
		}
			for(i=28;i<bytes.length;i+=20 ){ //Los 28 primeros son cabecera + autentificacion
				if(bytes[i-4] != 2 && bytes[i-3] != 2 )
				break;
			buffer= new byte[] { bytes[i], bytes[i+1], bytes[i+2], bytes[i+3]};
			ip=InetAddress.getByAddress(buffer);
			buffer= new byte[] { bytes[i+4], bytes[i+5], bytes[i+6], bytes[i+7]};
			mascara=InetAddress.getByAddress(buffer);
			buffer= new byte[] {bytes[i+15]};
			 coste= buffer[0]& 0xff;
				if(coste<15){
					if(ip.equals(ip_client))
						t=new Tabla(coste+1,ip,mascara,InetAddress.getByName("0"));
					else
						t= new Tabla(coste+1,ip,mascara,ip_client);
					actualizarlista(t); //llamamos actualizar lista
				}	

			}
	}



	/**
	 * metodo para anadir una nueva entrada a la tabla o actualizarla
	 * @param t_nueva
	 */
	public static synchronized void actualizarlista(Tabla t_nueva){
		boolean nueva=true;
		ArrayList<Tabla> lista_aux= new ArrayList<Tabla>(lista);
		int i=0;
		for(Tabla t_aux:lista_aux){
			if(t_aux.getIP().equals(t_nueva.getIP()) && t_aux.getMascara().equals(t_nueva.getMascara())){

				if(t_aux.getCoste() != t_nueva.getCoste()){
					if(t_nueva.getCoste() < t_aux.getCoste()){//si el coste es menor sustituimos la entrada de la tabla que teniamos por la actual
						lista.remove(i);
						lista.add(t_nueva);
					enviarlista();
					}
					else if(t_aux.getNextHop().equals(t_nueva.getNextHop())){ //si el coste nuevo es igual o mayor al que teniamos cambiamos el coste
						lista.get(i).setCoste(t_nueva.getCoste());
						enviarlista();
					}
				}
				lista.get(i).setTime(System.currentTimeMillis());//actualizamos temporizador del elemento de la tabla
				nueva=false;
			}
			i++;
		}
		//no coincide con ninguna entrada que haya hasta el momento en la tabla
			if(nueva)
				lista.add(t_nueva); //anadimos la entrada a la lista
		

	}


	/**
	 * metodo para comprobar los tiempos de cada nodo de la tabla
	 */
	public static synchronized void actualizarlista(){
		lista.get(0).setTime(System.currentTimeMillis()); //Como somos nosotros, nos actualizamos porque nunca debemos ser borrados
		ArrayList<Tabla> lista_aux= new ArrayList<Tabla>(lista);
		for(Tabla t:lista_aux){


				if(!t.getSub() && t.comprobareliminar()){ // si sobrepasa los 100s se elimina de la tabla
					lista.remove(t);
					enviarlista();
			
				}	

		}
		lista_aux = new ArrayList<Tabla>(lista);
		int i =0;
		for(Tabla t:lista_aux){
			if(!t.getSub() && t.comprobarcambio()){ // si sobrepasa los 60s se modifica su coste a 16
				if(t.getCoste()!=16){
				  lista.get(i).setCoste(16);
					enviarlista();
				}	
			}
			i++;


		}
	}


	/**
	 * metodo para imprimir la tabla de encaminamiento del router
	 */
	public static synchronized void imprimirlista(){
		System.out.println("\n ***** TABLA DE ENCAMINAMIENTO *****");
		int i=0;
		for(Tabla t:lista){
			System.out.println("Entrada" +(i++)+ " ->"+ t);
		}
	}


	/**
	 * concatenacion de arrays de bytes
	 * @param arrays
	 * @return
	 */

	public static byte[] concat(byte[] ... arrays){ //puntos suspensivos para permitir parametros ilimitados
		int length=0;
		for(int i=0; i<arrays.length; i++){
			length +=arrays[i].length;
		}
		byte[] result = new byte[length];
		int index=0;
		for(int i =0; i< arrays.length; i++){
			System.arraycopy(arrays[i],0,result,index,arrays[i].length);
			index+=arrays[i].length;
		}
		return result;
	}


	/**
	 * metodo para la obtencion de una mascara a partir del prefijo de subred
	 * @param len
	 * @return
	 * @throws UnknownHostException
	 */

	public static InetAddress getMascara(int len) throws UnknownHostException{ 
			
			    int aux = 0xffffffff << (32 - len);
			    byte[] bytes = new byte[]{ (byte)(aux >>> 24), (byte)(aux >> 16 & 0xff), (byte)(aux >> 8 & 0xff), (byte)(aux & 0xff) };
			    return InetAddress.getByAddress(bytes);
		}


	}