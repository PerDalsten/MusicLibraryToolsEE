package dk.purplegreen.musiclibrary.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;
import javax.validation.Valid;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
// @CacheDefaults

public class SongCreator {

	private static final Logger log = LogManager.getLogger(SongCreator.class);

	private static final String SELECT_ARTIST_SQL = "SELECT id FROM artist WHERE artist_name= ?";
	private static final String SELECT_ALBUM_SQL = "SELECT id FROM album WHERE artist_id= ? AND album_title = ?";
	private static final String INSERT_ARTIST_SQL = "INSERT INTO artist (artist_name) VALUES (?)";
	private static final String INSERT_ALBUM_SQL = "INSERT INTO album (artist_id, album_title, album_year) VALUES (?,?,?)";
	private static final String INSERT_SONG_SQL = "INSERT INTO song (album_id, song_title, track, disc) VALUES (?,?,?,?)";

	@Resource(lookup = "java:comp/env/jdbc/MusicLibraryDS")
	private DataSource musicLibraryDS;

	private Cache<String, Integer> artistCache;
	private Cache<String, Integer> albumCache;

	@PostConstruct
	private void setupCache() {

		CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();

		MutableConfiguration<String, Integer> configuration = new MutableConfiguration<>();
		configuration.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.FIVE_MINUTES));

		artistCache = cacheManager.createCache("artist-cache", configuration);
		albumCache = cacheManager.createCache("album-cache", configuration);
	}

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	@Lock(LockType.WRITE)
	public Integer createSong(@Valid Song song) {

		log.info("Creating song: " + song);

		Integer result = null;

		try (Connection con = musicLibraryDS.getConnection()) {

			Integer artistId = artistCache.get(song.getArtist());
			if (artistId == null) {
				try (PreparedStatement stmtSelectArtist = con.prepareStatement(SELECT_ARTIST_SQL)) {
					stmtSelectArtist.setString(1, song.getArtist());
					ResultSet rsSelectArtist = stmtSelectArtist.executeQuery();
					if (rsSelectArtist.next()) {
						artistId = rsSelectArtist.getInt("id");
						log.info("Got artist id: " + artistId + " from database");
					} else {
						// Create
						// artistId =
					}
					artistCache.put(song.getArtist(), artistId);
				}
			} else
				log.info("Got artist id: " + artistId + " from cache");

			String albumKey = artistId + "@" + song.getAlbum();
			Integer albumId = albumCache.get(albumKey);
			if (albumId == null) {
				try (PreparedStatement stmtSelectAlbum = con.prepareStatement(SELECT_ALBUM_SQL)) {
					stmtSelectAlbum.setInt(1, artistId);
					stmtSelectAlbum.setString(2, song.getAlbum());
					ResultSet rsSelectAlbum = stmtSelectAlbum.executeQuery();
					if (rsSelectAlbum.next()) {
						albumId = rsSelectAlbum.getInt("id");
						log.info("Got album id: " + albumId + " from database");
					} else {
						// Create
						// albumId =
					}
					log.info("Add to albumCache: " + albumKey + ":" + albumId);

					albumCache.put(albumKey, albumId);
				}
			} else
				log.info("Got album id: " + albumId + " from cache");

			// Create song
			try (PreparedStatement stmtInsertSong = con.prepareStatement(INSERT_SONG_SQL,
					Statement.RETURN_GENERATED_KEYS)) {
				stmtInsertSong.setInt(1, albumId);
				stmtInsertSong.setString(2, song.getTitle());
				stmtInsertSong.setInt(3, song.getTrack());
				stmtInsertSong.setInt(4, song.getDisc());
				stmtInsertSong.executeQuery();
				ResultSet rsInsertSong = stmtInsertSong.getGeneratedKeys();
				if (rsInsertSong.next())
					result = rsInsertSong.getInt(1);
			}

			return result;

		} catch (SQLException e) {
			log.error("Database error: ", e);
			//XXX
			return -1;
		}
	}
}
