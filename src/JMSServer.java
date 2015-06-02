    import java.util.Properties;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;


public class JMSServer implements MessageListener {
	private Connection connection;
	private Session session;
	private Queue queue;
	private MessageConsumer queueConsumer;
	private MessageConsumer topicConsumer;
	private Topic adTopic;
	DBConnection dbc;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new JMSServer();

	}
	public void publishAds(Message request, int userId) throws JMSException {

		dbc = new DBConnection();
		String []adArr = dbc.getAds(userId);
		String Ads = "";
		int i=0;
		int len = adArr.length;
		if(adArr[0].equalsIgnoreCase("no ads found"))
		{
			Ads = "no ads to display";

		}

		else {
			while(adArr[i]!=null)
			{
				Ads = Ads.concat(adArr[i]);
				Ads = Ads.concat(";");
				i++;
			}
		}
		System.out.println("in JMS SERVER: Ad string is "+Ads);

		MessageProducer MP = session.createProducer(null);
		Destination reply = request.getJMSReplyTo();
		//TextMessage TM = session.createTextMessage("hi...........");
		TextMessage TM = session.createTextMessage(Ads);
		MP.send(reply, TM);

	}
	public  JMSServer(){
		try
		{
			Properties properties = new Properties();
			properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
			properties.put(Context.URL_PKG_PREFIXES, "org.jnp.interfaces");
			properties.put(Context.PROVIDER_URL, "localhost");

			InitialContext jndi = new InitialContext(properties);
			ConnectionFactory conFactory = (ConnectionFactory)jndi.lookup("XAConnectionFactory");
			connection = conFactory.createConnection();

			session = connection.createSession( false, Session.AUTO_ACKNOWLEDGE );
			dbc = new DBConnection();
			try
			{
				queue = (Queue)jndi.lookup("UserQueue");
				adTopic = (Topic)jndi.lookup("adTopic");
			}
			catch(NamingException NE1)
			{
				System.out.println("NamingException: "+NE1+ " : Continuing anyway...");
			}

			if( null == queue )
			{
				queue = session.createQueue("UserQueue");
				jndi.bind("UserQueue", queue);
			}
			if( null == adTopic )
			{
				adTopic = session.createTopic("adTopic");
				jndi.bind("adTopic", adTopic);
			}
			queueConsumer = session.createConsumer( queue );
			topicConsumer = session.createConsumer( adTopic );
			queueConsumer.setMessageListener(this);
			topicConsumer.setMessageListener(this);
			System.out.println("Server started waiting for client requests");
			connection.start();
		}
		catch(NamingException NE)
		{
			System.out.println("Naming Exception: "+NE);
		}
		catch(JMSException JMSE)
		{
			System.out.println("JMS Exception: "+JMSE);
			JMSE.printStackTrace();
		}

	}
	public void sendReply(Message request, String replyString)
	{
		try
		{
			MessageProducer MP = session.createProducer(null);
			Destination reply = request.getJMSReplyTo();
			TextMessage TM = session.createTextMessage();
			TM.setText(""+replyString);
			//System.out.println("Reply STring is -- "+replyString);
			MP.send(reply, TM);
		}
		catch(JMSException JMSE)
		{
			System.out.println("JMS Exception: "+JMSE);
		}
	}


	public void onMessage(Message message){

		TextMessage TM = (TextMessage)message;
		String messageText;
		String[] messageTokens;
		String replyString;

		try
		{ 	
			messageText = TM.getText();
			messageTokens = messageText.split(",");

			if( messageTokens[0].equalsIgnoreCase("signin"))
			{
				//signin -read DB + publish user ads other than itself
				//if(messageTokens[1].equalsIgnoreCase("kanika@gmail.com") && messageTokens[2].equalsIgnoreCase("kanika1987"))
				System.out.println("on Message : Queue - sign in");
				String authenticationResult = dbc.authenticateUser(messageTokens[1], messageTokens[2]);

				if(!authenticationResult.equalsIgnoreCase("Incorrect username") && !authenticationResult.equalsIgnoreCase("Incorrect PASSWORD"))
				{
					replyString = authenticationResult;
					sendReply(message, replyString);
				}
			}
			else if( messageTokens[0].equalsIgnoreCase("signup"))
			{
				//signup
				String replyUserString = dbc.createUser(messageTokens[1], messageTokens[2], messageTokens[3], messageTokens[4]);

				sendReply(message, replyUserString);

			}
			else if( messageTokens[0].equalsIgnoreCase("postad"))
			{
				int userId = Integer.parseInt(messageTokens[1]);
				//int price = Integer.parseInt(messageTokens[4]);
				createNewAd(userId,messageTokens[2],messageTokens[3],messageTokens[4],messageTokens[5]);
				System.out.println("item =" + messageTokens[1]);
			}
			else if( messageTokens[0].equalsIgnoreCase("getUserAds") )
			{

				System.out.println("on Message : Topic - get user ads");
				System.out.println("message token 1 is : SHOULD BE USERID"+messageTokens[1]);
				int userId = Integer.parseInt(messageTokens[1]);
				if(dbc.isUserSubscribed(userId)){
					publishAds(message, userId);
				}
				else{
					sendReply(message,"you are not subscribed to any ads");
				}
			}
			else if(messageTokens[0].equalsIgnoreCase("subscribe"))
			{
				int userId = Integer.parseInt(messageTokens[1]);
				subscribeUser(userId);
				sendReply(message, "subscribed successfully");
			}
			else if(messageTokens[0].equalsIgnoreCase("unsubscribe"))
			{
				int userId = Integer.parseInt(messageTokens[1]);
				unsubscribeUser(userId);
				sendReply(message, "unsubscribed successfully");
			}

		}
		catch(JMSException JMSE)
		{
			System.out.println("JMS Exception: "+JMSE);
		}
	}
	public String createUser(String firstName, String lastName, String username, String password)
	{
		dbc = new DBConnection();
		return(dbc.createUser(firstName, lastName, username, password));
	}
	public void createNewAd(int userId, String itemName, String itemDescription, String sellerInfo, String itemPrice) {
		// TODO Auto-generated method stub
		dbc = new DBConnection();
		dbc.insertAd(userId,itemName,itemDescription, sellerInfo,itemPrice);
	}
	public boolean subscribeUser(int userId)
	{
		dbc = new DBConnection();
		return(dbc.subscribeUser(userId));
	}
	public boolean unsubscribeUser(int userId)
	{
		dbc = new DBConnection();
		return(dbc.unsubscribeUser(userId));
	}

}
