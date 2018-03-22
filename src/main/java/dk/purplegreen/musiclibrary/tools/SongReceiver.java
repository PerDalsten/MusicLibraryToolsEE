package dk.purplegreen.musiclibrary.tools;

import java.io.StringReader;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "MUSICLIBRARY.SONG") })
public class SongReceiver implements MessageListener {

	private static final Logger log = LogManager.getLogger(SongReceiver.class);

	@EJB
	private SongCreator songCreator;

	@Resource(lookup = "java:comp/env/jms/ActiveMQConnectionFactory")
	private ConnectionFactory cf;

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage) {
				String songJSON = ((TextMessage) message).getText();

				log.info("Received song: " + songJSON);

				Song song = getSong(songJSON);

				Integer id = songCreator.createSong(song);								
				
				if (message.getJMSReplyTo() != null) {

					String corrId = message.getJMSCorrelationID();
					try (Connection con = cf.createConnection()) {
						Session session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
						MessageProducer producer = session.createProducer(message.getJMSReplyTo());

						con.start();

						String result = Json.createObjectBuilder().add("id", id).build().toString();

						TextMessage reply = session.createTextMessage(result);
						reply.setJMSCorrelationID(corrId);
						producer.send(reply);
					}
				}
			}
		} catch (Exception e) {
			log.error("Exception caught: ", e);
		}
	}

	private Song getSong(String song) {
		JsonReader jsonReader = Json.createReader(new StringReader(song));

		JsonObject jsonObject = jsonReader.readObject();

		jsonReader.close();

		return new Song(jsonObject.getString("artist", null), jsonObject.getString("album", null),
				jsonObject.isNull("year") ? null : jsonObject.getInt("year"), jsonObject.getString("title", null),
				jsonObject.isNull("track") ? null : jsonObject.getInt("track"), jsonObject.getInt("disc", 1));
	}

}
