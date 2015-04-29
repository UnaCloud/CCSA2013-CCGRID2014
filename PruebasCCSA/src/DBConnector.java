
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

	public class DBConnector {
		private static final String db = "iaas2prod";
		private static final String host = "jdbc:mysql://157.253.236.160:3306/"+db+"?useUnicode=yes&characterEncoding=UTF-8";
		private static final String username = "readOnly";
		private static final String password = "reader123";
		private Connection con;
		
		private static void connect() {
			try {
				Class.forName("com.mysql.jdbc.Driver");
			} catch (ClassNotFoundException e) {				
				e.printStackTrace();
			}
		}
		
		public DBConnector(){
			connect();
		}
		
		public String getDataPM(long vmId) throws SQLException {			 
			PreparedStatement ps = con.prepareStatement("SELECT pm.with_user, pm.name, vm.hardware_profile_id, ip.ip FROM physical_machine pm INNER JOIN virtual_machine_execution vm ON pm.id = vm.execution_node_id INNER JOIN ip as ip ON ip.id = pm.ip_id WHERE vm.id ="+vmId+";");
			ResultSet rs = ps.executeQuery();			
			if(rs.next())return rs.getString(2)+"\t"+rs.getBoolean(1)+"\t"+(rs.getLong(3)==1?"small":rs.getLong(3)==2?"medium":rs.getLong(3)==3?"large":"xlarge")+"\t"+rs.getString(4);		
			return null;
		}
		
		public void connection() throws SQLException{
			con= DriverManager.getConnection(host, username, password);
		}
		
		public void close() throws SQLException{
			con.close();
		}

		public void modifyAllocator(String allocator) throws SQLException {
			connection();
			String query = "update server_variable as v set v.variable = '"+allocator+"' where v.name = 'VM_ALLOCATOR_NAME' and id > 0";
			System.out.println(query);
			PreparedStatement ps = con.prepareStatement(query);
			ps.executeUpdate();
			close();			
		}
		
		public int machinesInError() throws SQLException{
			int res = -1;
			connection();			
			PreparedStatement ps = con.prepareStatement("SELECT count(vm.id) FROM iaas2prod.virtual_machine_execution as vm inner join iaas2prod.physical_machine as pm on pm.id = vm.execution_node_id WHERE pm.laboratory_id in (1,2) AND vm.status <>'FINISHED' ");
			ResultSet rs = ps.executeQuery();
			if(rs.next())res = rs.getInt(1);
			close();
			return res;
		}
	}

	

