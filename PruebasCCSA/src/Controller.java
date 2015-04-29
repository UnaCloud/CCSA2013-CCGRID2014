import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Random;
import java.util.TreeMap;


public class Controller {
	
	private static final int[] deploys = {10,20,30,40,50,60,70,80,90};
	private static final String [] allocators = {"ROUND_ROBIN","SORTING","BEST_FIT"};		
	static Random r=new Random();
	static File f = new File("list.txt");
	static File fr = new File("results.txt");
	static Tester test = new Tester();	
	
	
	
	public Controller() throws FileNotFoundException{
		//Creamos el archivo en caso que no exista, este archivo llevara el conteo de despliegues por allocation
		if(!f.exists()){
			PrintWriter pw=new PrintWriter(new FileOutputStream(f),true);
			pw.append("BEST_FIT=10:0,20:0,30:0,40:0,50:0,60:0,70:0,80:0,90:0\n");
			pw.append("ROUND_ROBIN=10:0,20:0,30:0,40:0,50:0,60:0,70:0,80:0,90:0\n");
			pw.append("SORTING=10:0,20:0,30:0,40:0,50:0,60:0,70:0,80:0,90:0\n");
			pw.close();
		}
		//Guardamos todos los comentarios de consola en archivo
		try {
        	PrintStream ps=new PrintStream(new FileOutputStream("log.txt",true),true){
        		@Override
        		public void println(String x) {
        			super.println(new Date()+" "+x);
        		}
        		@Override
        		public void println(Object x) {
        			super.println(new Date()+" "+x);
        		}
        	};
			System.setOut(ps);
			System.setErr(ps);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try{
//			File fr = new File("results.txt");
//			if(!fr.exists())fr.createNewFile();
			//Creamos el ciclo principal
			while(true){
				//Elegimos en random la cantidad de maquinas
				int deploy = deploys[r.nextInt(deploys.length)];
				System.out.println("*************************INIT CYCLE****************************");
				//Elegimos en random el tipo de allocation
				String allocator = allocators[r.nextInt(allocators.length)];
				TreeMap<String, TreeMap<Integer, Integer>> map = loadFile();
				//Validamos que se quieran más pruebas de ese allocation.
				if(isValid(deploy,allocator,map)){
					//Validamos que no hay errores en ese despliegue.
					if(test.certifiedDeploy()){
						map.get(allocator).put(deploy, map.get(allocator).get(deploy)+1);
						test.init(deploy+"", allocator);						
						Thread.sleep(60*1000*5);
						saveFile(map);
					}else {
						//En caso de error envia mensaje y se queda esperando 5 minutos 
						Sender.send();
						System.out.println("Esperando arreglo de error "+new Date());
						Thread.sleep(60*1000*5);
					}					
				}
				System.out.println("*************************END CYCLE****************************");
			}
			
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException{
//		new Controller();
//		for (int i = 0; i <10; i++) {
//			System.out.println(deploys[r.nextInt(9)]);
//		}
	}

	private static boolean isValid(int deploy, String allocator,TreeMap<String, TreeMap<Integer, Integer>> map) throws IOException {
		if(map.get(allocator).get(deploy)>0)return true;
		return false;
	}
	
	private static TreeMap<String, TreeMap<Integer, Integer>> loadFile() throws IOException{
		TreeMap<String, TreeMap<Integer, Integer>> map = new TreeMap<String, TreeMap<Integer,Integer>>();
		BufferedReader r = new BufferedReader(new FileReader(f));
		String line = null;
        while((line = r.readLine())!=null){
        	String alloc = line.split("=")[0];        	
        	String[] data = line.split("=")[1].split(",");
        	TreeMap<Integer, Integer> subMap = new TreeMap<Integer, Integer>();
        	for (String string : data) {
        		subMap.put(Integer.parseInt(string.split(":")[0]), Integer.parseInt(string.split(":")[1]));
			}
        	map.put(alloc, subMap);
        }
        r.close();
        return map;
	}
	
	private static void saveFile(TreeMap<String, TreeMap<Integer, Integer>> map) throws IOException{		
		BufferedWriter bf = new BufferedWriter(new FileWriter(f));
		bf.write("");
		for (String key : map.navigableKeySet()) {
			TreeMap<Integer, Integer> mp = map.get(key);
			String chain = "";
			for (Integer sKey : mp.navigableKeySet()) {
				chain+=","+sKey+":"+mp.get(sKey);
			}
			chain = chain.replaceFirst(",", "");
			bf.append(key+"="+chain+"\n");
		}
		bf.close();
	}

}
