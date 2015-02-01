
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import unacloudws.UnaCloudOperations;
import unacloudws.requests.VirtualImageRequest;
import unacloudws.responses.VirtualMachineExecutionWS;
import unacloudws.responses.VirtualMachineStatusEnum;

public class Tester {
	public static void main(String[] args){
		try{
			int number=Integer.parseInt(args[0]);
			ClusterDescription c=startCluster(number);
			new File(""+c.id).mkdirs();
			PrintWriter pw=new PrintWriter(new FileOutputStream("dep"+c.id+".txt"),true);
			if(c!=null&&c.vms!=null&&!c.vms.isEmpty()){
				Thread[] ts=new Thread[number];
				for(int e=0;e<ts.length;e++)ts[e]=new HiloVm(e,c.id,c.vms.get(e).getVirtualMachineExecutionIP());
				pw.println("Arrancando en "+new Date());
				for(int e=0;e<ts.length;e++)ts[e].start();
				for(int e=0;e<ts.length;e++)ts[e].join();
				pw.println("Terminando en "+new Date());
			}
			pw.close();
			if(c!=null)apagarCluster(c.id);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	public static ClusterDescription startCluster(int size)throws Exception{
		UnaCloudOperations uop=new UnaCloudOperations("admin","XT2PRB591GOJU5J9CTUP8EHK06IH37Y1");
		String deploymentId=uop.startHeterogeneousVirtualCluster(4,180,createRandomCluster(size));
		ClusterDescription desc=new ClusterDescription(-1,null);
		if(deploymentId!=null&&deploymentId.contains("\"id\"")){
			int ini=deploymentId.indexOf("\"id\""),fin=deploymentId.indexOf(",",ini);
			ini=deploymentId.indexOf(":",ini);
			System.out.println("El id es "+deploymentId.substring(ini,fin));
			desc.id=Long.parseLong(deploymentId.substring(ini+1,fin));
			for(int intentos=0;intentos<20;intentos++){
				int encendidas=0,fallidas=0;
				Thread.sleep(60000);
				List<VirtualMachineExecutionWS> vms=uop.getDeploymentInfo((int)desc.id);
				for(VirtualMachineExecutionWS vm:vms){
					if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.DEPLOYED)encendidas++;
					if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.FAILED)fallidas++;
				}
				if(encendidas+fallidas==vms.size()){
					desc.vms=new ArrayList<VirtualMachineExecutionWS>();
					PrintWriter pw=new PrintWriter(desc.id+".txt");
					for(VirtualMachineExecutionWS vm:vms)if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.DEPLOYED){
						pw.println(vm.getVirtualMachineExecutionIP());
						desc.vms.add(vm);
					}
					pw.close();
					break;
				}
				System.out.println(" Encendidas "+encendidas+"/"+vms.size());
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
					Thread.sleep(10000);
					e--;
				}
			}
		}
	}
	public static void apagarCluster(long id)throws Exception{
		UnaCloudOperations uop=new UnaCloudOperations("admin","XT2PRB591GOJU5J9CTUP8EHK06IH37Y1");
		uop.stopDeployment((int)id);
		Thread.sleep(60000);
	}
	public static VirtualImageRequest[] createRandomCluster(int n){
		VirtualImageRequest[] ret=new VirtualImageRequest[n];
		String[] hardwareProfiles=new String[]{"small","medium","large"};
		for(int e=0;e<n;e++){
			ret[e]=new VirtualImageRequest(1,hardwareProfiles[r.nextInt(3)],2,"ccgridtest");
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
				Process p=runtime.exec(new String[]{"sshpass","-p","DebianP445","ssh","-o","StrictHostKeyChecking=no",ip,"mdrun -v -s MD.tpr"});
				Files.copy(Paths.get(deploymentId+"/"+id+".out"),p.getOutputStream());
				System.out.println(" El proceso "+id+" termino con codigo "+p.waitFor());
				p.destroy();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	static Random r=new Random();
	static Runtime runtime=Runtime.getRuntime();
	
}
