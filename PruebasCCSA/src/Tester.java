
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import unacloudws.UnaCloudOperations;
import unacloudws.requests.VirtualImageRequest;
import unacloudws.responses.VirtualMachineExecutionWS;
import unacloudws.responses.VirtualMachineStatusEnum;

public class Tester {
	
	private static final String USER = "admin";
	private static final String REMOTE_ID = "PT2PRB591GOJU5J9CTUP8EHK06IH37Y1";
	private static final int CLUSTER = 4;
	private static final int TIME = 180;
	private static final int IMAGE = 2;
	private static final String DEPLOY_NAME = "ccgridtest";
	private static DBConnector connection;
	
	
//	public static void main(String[] args){
//		init(args[0],null);		
//	}	
	
	public Tester() {
		connection = new DBConnector();
		try {
			
		} catch (Exception e) {
			e.printStackTrace();
			// TODO: handle exception
		}
		
	}

	public void init(String deploy, String allocator) {
		try{			
			//Modificamos allocator escogido para efectuar el siguiente despliegue
			connection.modifyAllocator(allocator);
			int number=Integer.parseInt(deploy);
			System.out.println("init cluster: "+deploy);
			ClusterDescription c=startCluster(number);
			//new File(""+c.id).mkdirs();			
			String line  = c.id+"\t"+allocator+"\t"+deploy+"\t"+new Date();			
			PrintWriter pw=new PrintWriter(new FileOutputStream(c.id+"/times.txt"),true);
			if(c!=null&&c.vms!=null&&!c.vms.isEmpty()){
				Thread[] ts=new Thread[number];
				for(int e=0;e<ts.length;e++)ts[e]=new HiloVm(c.vms.get(e).getId(),c.id,c.vms.get(e).getVirtualMachineExecutionIP());
				pw.println("Arrancando en "+new Date());				
				createStateAntes(c,allocator);
				for(int e=0;e<ts.length;e++)ts[e].start();
				for(int e=0;e<ts.length;e++)ts[e].join();
				createStateDespues(c, allocator);
				pw.println("Terminando en "+new Date());
			}
			pw.close();
			PrintWriter pwRe=new PrintWriter(new FileOutputStream("results.txt",true),true);
			line+="\t"+new Date();
			pwRe.println(line);
			pwRe.close();
			if(c!=null)apagarCluster(c.id);
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	
	private static void createStateDespues(ClusterDescription c, String allocator) throws SQLException, FileNotFoundException {
		PrintWriter pw=new PrintWriter(new FileOutputStream(c.id+"/pmStateDespues.txt"),true);		
		connection.connection();
		pw.println(allocator);
		for(int e=0;e<c.vms.size();e++){			
			pw.println(e+"\t"+c.vms.get(e).getId()+"\t"+c.vms.get(e).getVirtualMachineName()+"\t"+connection.getDataPM(c.vms.get(e).getId()));	
		}	
		pw.close();
		connection.close();
	}
	private static void createStateAntes(ClusterDescription c, String allocator) throws SQLException, FileNotFoundException {
		PrintWriter pw=new PrintWriter(new FileOutputStream(c.id+"/pmStateAntes.txt"),true);	
		connection.connection();
		pw.println(allocator);
		for(int e=0;e<c.vms.size();e++){
			pw.println(e+"\t"+c.vms.get(e).getId()+"\t"+c.vms.get(e).getVirtualMachineName()+"\t"+connection.getDataPM(c.vms.get(e).getId()));			
		}
		pw.close();
		connection.close();
	}
	public static ClusterDescription startCluster(int size)throws Exception{
		UnaCloudOperations uop=new UnaCloudOperations(USER,REMOTE_ID);
		String deploymentId=uop.startHeterogeneousVirtualCluster(CLUSTER,TIME,createRandomCluster(size));
		ClusterDescription desc=new ClusterDescription(-1,null);
		
		if(deploymentId!=null&&deploymentId.contains("\"id\"")){
			int ini=deploymentId.indexOf("\"id\""),fin=deploymentId.indexOf(",",ini);
			ini=deploymentId.indexOf(":",ini);
			System.out.println("El id es "+deploymentId.substring(ini,fin));			
			desc.id=Long.parseLong(deploymentId.substring(ini+1,fin));
			new File(""+desc.id).mkdirs();
			for(int intentos=0;intentos<20;intentos++){
				int encendidas=0,fallidas=0;
				Thread.sleep(60000);
				List<VirtualMachineExecutionWS> vms=uop.getDeploymentInfo((int)desc.id);
				for(VirtualMachineExecutionWS vm:vms){
					if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.DEPLOYED)encendidas++;
					if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.FAILED)fallidas++;
				}
				System.out.println(" Encendidas "+encendidas+"/"+vms.size());
				if(encendidas+fallidas==vms.size()||intentos==19){
					desc.vms=new ArrayList<VirtualMachineExecutionWS>();
					PrintWriter pw=new PrintWriter(desc.id+"/ips.txt");
					for(VirtualMachineExecutionWS vm:vms){
						if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.DEPLOYED){
							pw.println(vm.getId()+"\t"+vm.getVirtualMachineExecutionIP()+" DEPLOYED");							
						}else pw.println(vm.getVirtualMachineExecutionIP()+" FAILED");
					    desc.vms.add(vm);
					}
					pw.close();
					break;
				}				
			}
		}
		verificarServicioSSH(desc);
		System.out.println("El cluster con id "+desc.id+" ha iniciado exitosamente");
		return desc;
	}
	private static void verificarServicioSSH(ClusterDescription desc)throws Exception{
		for(int e=0;e<desc.vms.size();e++){//Esto está verificando que tengan el ssh encendido. A veces se demora arancando
			VirtualMachineExecutionWS vm = desc.vms.get(e);
			if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.DEPLOYED){
				try{
					Socket s=new Socket(vm.getVirtualMachineExecutionIP(),22);
					s.close();
					System.out.println("SSH habilitado a "+e+" "+vm.getVirtualMachineExecutionIP());
				}catch (Exception ex) {
					ex.printStackTrace();
					Thread.sleep(10000);
					e--;
				}
			}
		}
	}
	public static void apagarCluster(long id)throws Exception{
		UnaCloudOperations uop=new UnaCloudOperations(USER,REMOTE_ID);
		uop.stopDeployment((int)id);
		Thread.sleep(60000);
	}
	public static VirtualImageRequest[] createRandomCluster(int n){
		VirtualImageRequest[] ret=new VirtualImageRequest[n];
		String[] hardwareProfiles=new String[]{"small","medium","large"};
		for(int e=0;e<n;e++){
			ret[e]=new VirtualImageRequest(1,hardwareProfiles[r.nextInt(3)],IMAGE,DEPLOY_NAME);
		}
		return ret;
	}
	static class ClusterDescription{
		long id;
		List<VirtualMachineExecutionWS> vms;
		public ClusterDescription(long id, List<VirtualMachineExecutionWS> vms) {
			this.id = id;
			this.vms = vms;
		}
	}
	static class HiloVm extends Thread{
		String ip;
		int id;
		long deploymentId;
		public HiloVm(int id,long deploymentId,String ip) {
			this.ip = ip;
			this.id=id;
			this.deploymentId=deploymentId;
		}
		@Override
		public void run() {
			try {				
				//System.out.println("sshpass -p DebianP445 ssh -o StrictHostKeyChecking=no "+ip+" 'mdrun -v -s MD.tpr'");
				Process p=runtime.exec(new String[]{"sshpass","-p","DebianP445","ssh","-o","StrictHostKeyChecking=no",ip,"mdrun -v -s MD.tpr"});
  	            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
	            String line = null;
	            PrintWriter pw=new PrintWriter(new FileOutputStream(deploymentId+"/"+id+".out"),true);
	            pw.append("Empieza ejecución:"+new Date()+" \n");
	            while ( (line = stdError.readLine()) != null)
	                pw.append(line+"\n");
				System.out.println("El proceso "+id+" termino con codigo "+p.waitFor());
				pw.append("Termina ejecución:"+new Date()+" \n");
				pw.close();
				p.destroy();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	static Random r=new Random();
	static Runtime runtime=Runtime.getRuntime();

	public boolean certifiedDeploy() {
		try {
			int n = connection.machinesInError();
			if(n!=0){
				System.out.println("Error en maquinas "+n);
				return false;
			}
			else return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}		
	}
	
}
