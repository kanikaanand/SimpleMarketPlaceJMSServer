import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {
	Connection con = null;
	Statement s = null;
	ResultSet rs = null;

	public DBConnection()
	{
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			con = DriverManager.getConnection("jdbc:mysql://localhost/smpjms","root", "root");

			if (!con.isClosed()) {
				System.out
				.println("Successfully Connected to Mysql server using TCP/IP");

			}

			s = con.createStatement();

		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	public boolean subscribeUser(int userId)
	{
		String query = "update `user` set isUserSubscribed = 1 where user_id ="+userId+";";
		try {
			s.executeUpdate(query);
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;


	}
	public boolean unsubscribeUser(int userId)
	{
		String query = "update `user` set isUserSubscribed = 0 where user_id ="+userId+";";
		try {
			s.executeUpdate(query);
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public String[] getAds(int userId)
	{
		String []arr = new String[100];
		int i = 0;
		int rsCount = 0;
		String query ="select * from item where user_id !="+userId;
		try{
			rs = s.executeQuery(query);
			// count if rs.count ==0 then return no ads found and publish nothing on the topic
			while(rs.next())
			{
				rsCount = rsCount + 1;
			}
			rs.beforeFirst();
			if(rsCount == 0)
			{
				arr[0] = "no ads found";
				return arr;
			}
			rs.beforeFirst();
			while(rs.next())
			{
				arr[i] = rs.getInt(1)+","+rs.getString(2)+","+rs.getString(3)+","+rs.getString(4)+","+rs.getString(5);
				//System.out.println("In getAds: Iteration"+i);
				i++;
			}
		}
		catch(SQLException e)
		{
			e.printStackTrace();
		}

		return arr;
	}
	public String createUser(String firstName, String lastName, String username, String password)
	{
		String query = "insert into user(user_email, user_password, user_firstname, user_lastname,last_logged, isUserSubscribed) "
				+ "values ('"+username+"','"+password+"','" +firstName+"','"+lastName+"', now(),1);";
		try {
			s.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return(authenticateUser(username,password));

	}

	public void insertAd(int userId, String itemName, String itemDescription, String sellerInfo, String itemPrice) {
		String query = "insert into item(user_id , item_name,item_description, seller_information, item_price) values ("+userId+",'"+itemName+"','" +itemDescription+"','"+sellerInfo+"',"+itemPrice+");";
		try {
			s.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public boolean isUserSubscribed(int userId)
	{
		String query = "select * from user where user_id ="+userId+";";
		try {
			rs = s.executeQuery(query);
			if(rs.next())
			{
				int isUserSubscribed = Integer.parseInt(rs.getString("isUserSubscribed"));
				if (isUserSubscribed ==1)
					return true;

			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public String authenticateUser(String username, String password)
	{
		String query ="select * from user where user_email='"+username+"';";
		String userString = "";
		int rsCount = 0;
		try {
			rs = s.executeQuery(query);
			while(rs.next())
			{
				rsCount = rsCount + 1;
			}
			rs.beforeFirst();
			System.out.println("rs count="+rsCount);
			if(rsCount !=1){
				System.out.println("Incorrect username");
				userString = "Incorrect username"; 
				//return userValidateResult;
			}
			while(rs.next())
			{
				//System.out.println(r.getString("user_password"));
				String p = rs.getString("user_password");
				if(p.equals(password))
				{
					System.out.println("correct password");

					userString = rs.getString("user_id")+";"+ rs.getString("user_firstname") +";" +
							rs.getString("user_lastname") + ";" + rs.getString("user_email")
							+";"+rs.getString("user_password")+";"+rs.getString("last_logged");
					s = con.createStatement();
					s.executeUpdate("UPDATE  `USER` set last_logged = now() where user_email ='"+username+"'");

					//return userValidateResult;
				}
				else
				{
					System.out.println("incorrect password");
					userString = "Incorrect Password";
					//return userValidateResult;

				}
			}

		}
		catch(SQLException e)
		{
			System.out.println("Error in the sql queries");
			e.printStackTrace();

		}
		catch(Exception e)
		{
			System.out.println("Error in the code in DBConnection for user");
			e.printStackTrace();

		}
		return userString;

	}

}
