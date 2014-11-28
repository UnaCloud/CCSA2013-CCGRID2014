
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Random;

import unacloudws.UnaCloudOperations;
import unacloudws.requests.VirtualImageRequest;
import unacloudws.responses.VirtualMachineExecutionWS;
import unacloudws.responses.VirtualMachineStatusEnum;

public class ReplicaCesar {
	public static void main(String[] args){
		try{
			int number=5;
			ClusterDescription c=startCluster(number);
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	public static ClusterDescription startCluster(int size)throws Exception{
		UnaCloudOperations uop=new UnaCloudOperations("admin","XT2PRB591GOJU5J9CTUP8EHK06IH37Y1");
		String deploymentId=uop.startHeterogeneousVirtualCluster(4,180,createRandomCluster(size));
		ClusterDescription desc=new ClusterDescription(-1,null);
		if(deploymentId!=null&&deploymentId.contains("\"id\"")){
			int ini=deploymentId.indexOf("\"id\"");
			int fin=deploymentId.indexOf(",",ini);
			ini=deploymentId.indexOf(":",ini);
			System.out.println("El id es "+deploymentId.substring(ini,fin));
			desc.id=Long.parseLong(deploymentId.substring(ini+1,fin));
			for(int intentos=0;intentos<20;intentos++){
				int encendidas=0;
				Thread.sleep(60000);
				List<VirtualMachineExecutionWS> vms=uop.getDeploymentInfo((int)desc.id);
				for(VirtualMachineExecutionWS vm:vms){
					if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.DEPLOYED)encendidas++;
					if(vm.getVirtualMachineExecutionStatus()==VirtualMachineStatusEnum.FAILED){
						System.out.println("El cluster fallo, inicie de nuevo.");
						return desc;
					}
				}
				if(encendidas==vms.size()){
					desc.vms=vms;
					PrintWriter pw=new PrintWriter(desc.id+".txt");
					for(VirtualMachineExecutionWS vm:vms)pw.println(vm.getVirtualMachineExecutionIP());
					pw.close();
					break;
				}
				System.out.println(" Encendidas "+encendidas+"/"+vms.size());
			}
		}
		for(int e=0;e<desc.vms.size();e++){
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
		System.out.println("El cluster con id "+desc.id+" ha iniciado exitosamente");
		return desc;
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
	static Random r=new Random();
}
