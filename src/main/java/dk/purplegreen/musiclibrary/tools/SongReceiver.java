package dk.purplegreen.musiclibrary.tools;

import java.io.StringReader;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
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

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof TextMessage) {
				String songJSON = ((TextMessage) message).getText();

				log.info("Received song: " + songJSON);

				Song song = getSong(songJSON);

				songCreator.createSong(song);

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
