package dk.purplegreen.musiclibrary.tools;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Singleton
public class SongCreator {

	private static final Logger log = LogManager.getLogger(SongCreator.class);

	@Resource(lookup = "java:comp/env/jdbc/MusicLibraryDS")
	private DataSource musicLibraryDS;

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void createSong(@Valid Song song) {

		log.info("Creating song: " + song);

		try (Connection con = musicLibraryDS.getConnection()) {
			
			

		} catch (SQLException e) {
			log.error("Database error: ", e);
		}
	}

}
