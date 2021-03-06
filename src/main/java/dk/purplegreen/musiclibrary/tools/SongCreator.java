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

		log.debug("Creating song: {}", song);

		Integer result = null;

		try (Connection con = musicLibraryDS.getConnection()) {

			Integer artistId = artistCache.get(song.getArtist());
			if (artistId == null) {
				try (PreparedStatement stmtSelectArtist = con.prepareStatement(SELECT_ARTIST_SQL)) {
					stmtSelectArtist.setString(1, song.getArtist());
					try (ResultSet rsSelectArtist = stmtSelectArtist.executeQuery()) {
						if (rsSelectArtist.next()) {
							artistId = rsSelectArtist.getInt("id");
							artistCache.put(song.getArtist(), artistId);
							log.debug("Retrieved artist {} from database", song.getArtist());
						}
					}
				}
			} else {
				log.debug("Retrieved artist {} from cache", song.getArtist());
			}

			if (artistId == null) {
				try (PreparedStatement stmtInsertArtist = con.prepareStatement(INSERT_ARTIST_SQL,
						Statement.RETURN_GENERATED_KEYS)) {
					stmtInsertArtist.setString(1, song.getArtist());

					stmtInsertArtist.executeUpdate();
					try (ResultSet rsInsertArtist = stmtInsertArtist.getGeneratedKeys()) {
						if (rsInsertArtist.next()) {
							artistId = rsInsertArtist.getInt(1);
							artistCache.put(song.getArtist(), artistId);
							log.info("Inserted artist {} into database with id: {}", song.getArtist(), artistId);
						}
					}
				}
			}

			String albumKey = artistId + "@" + song.getAlbum();
			Integer albumId = albumCache.get(albumKey);
			if (albumId == null) {
				try (PreparedStatement stmtSelectAlbum = con.prepareStatement(SELECT_ALBUM_SQL)) {
					stmtSelectAlbum.setInt(1, artistId);
					stmtSelectAlbum.setString(2, song.getAlbum());

					try (ResultSet rsSelectAlbum = stmtSelectAlbum.executeQuery()) {
						if (rsSelectAlbum.next()) {
							albumId = rsSelectAlbum.getInt("id");
							albumCache.put(albumKey, albumId);
						}
					}
				}
			} else {
				log.debug("Retrieved album {} from cache", song.getAlbum());
			}

			if (albumId == null) {
				try (PreparedStatement stmtInsertAlbum = con.prepareStatement(INSERT_ALBUM_SQL,
						Statement.RETURN_GENERATED_KEYS)) {
					stmtInsertAlbum.setInt(1, artistId);
					stmtInsertAlbum.setString(2, song.getAlbum());
					stmtInsertAlbum.setInt(3, song.getYear());

					stmtInsertAlbum.executeUpdate();
					try (ResultSet rsInsertAlbum = stmtInsertAlbum.getGeneratedKeys()) {
						if (rsInsertAlbum.next()) {
							albumId = rsInsertAlbum.getInt(1);
							albumCache.put(albumKey, albumId);
							log.info("Inserted album {} into database with id: {}", song.getAlbum(), albumId);
						}
					}
				}
			}

			// Create song
			try (PreparedStatement stmtInsertSong = con.prepareStatement(INSERT_SONG_SQL,
					Statement.RETURN_GENERATED_KEYS)) {
				stmtInsertSong.setInt(1, albumId);
				stmtInsertSong.setString(2, song.getTitle());
				stmtInsertSong.setInt(3, song.getTrack());
				stmtInsertSong.setInt(4, song.getDisc());
				stmtInsertSong.executeUpdate();
				try (ResultSet rsInsertSong = stmtInsertSong.getGeneratedKeys()) {
					if (rsInsertSong.next()) {
						result = rsInsertSong.getInt(1);
						log.info("Inserted song {} into database with id: {}", song.getTitle(), result);
					}
				}
			}

			return result;

		} catch (SQLException e) {
			log.error("Database error: ", e);
			throw new IllegalStateException("Database error while creating song", e);
		}
	}
}
