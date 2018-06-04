package dk.purplegreen.musiclibrary.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dk.purplegreen.musiclibrary.tools.ws.AlbumService;
import dk.purplegreen.musiclibrary.tools.ws.AlbumType;
import dk.purplegreen.musiclibrary.tools.ws.Albums;
import dk.purplegreen.musiclibrary.tools.ws.GetAlbums;
import dk.purplegreen.musiclibrary.tools.ws.GetAlbumsResponse;
import dk.purplegreen.musiclibrary.tools.ws.SongType;

@WebService(endpointInterface = "dk.purplegreen.musiclibrary.tools.ws.AlbumService", targetNamespace = "http://www.purplegreen.dk/albumservice", portName = "AlbumServiceSOAP", serviceName = "AlbumService")
public class AlbumServiceImpl implements AlbumService {

	private static final Logger log = LogManager.getLogger(AlbumServiceImpl.class);

	private static final String SELECT_ALBUMS_SQL = "SELECT album.id AS id, album.album_title AS title, album.album_year AS yr, artist.artist_name AS artist "
			+ "FROM album " + "JOIN artist ON artist.id=album.artist_id";

	private static final String SELECT_SONGS_SQL = "SELECT album_id, song_title AS title, track, disc FROM song ORDER BY album_id, disc, track";

	@Resource(lookup = "java:comp/env/jdbc/MusicLibraryDS")
	private DataSource musicLibraryDS;

	@Override
	public GetAlbumsResponse getAlbums(GetAlbums in) {
		GetAlbumsResponse result = new GetAlbumsResponse();

		try (Connection con = musicLibraryDS.getConnection()) {

			Map<Integer, AlbumType> albums = new HashMap<>();

			try (PreparedStatement stmtSelectAlbums = con.prepareStatement(SELECT_ALBUMS_SQL);
					ResultSet rsAlbums = stmtSelectAlbums.executeQuery()) {

				while (rsAlbums.next()) {
					AlbumType album = new AlbumType();
					album.setSongs(new AlbumType.Songs());
					album.setArtist(rsAlbums.getString("artist"));
					album.setTitle(rsAlbums.getString("title"));
					album.setYear(rsAlbums.getInt("yr"));
					albums.put(rsAlbums.getInt("id"), album);
					log.debug("Added album: {}", album.getTitle());
				}
			}

			try (PreparedStatement stmtSelectSongs = con.prepareStatement(SELECT_SONGS_SQL);
					ResultSet rsSongs = stmtSelectSongs.executeQuery()) {

				while (rsSongs.next()) {
					SongType song = new SongType();
					song.setTitle(rsSongs.getString("title"));
					song.setTrack(rsSongs.getInt("track"));
					song.setDisc(rsSongs.getInt("disc"));
					albums.get(rsSongs.getInt("album_id")).getSongs().getSong().add(song);
					log.debug("Added song: {}", song.getTitle());
				}
			}

			LinkedList<AlbumType> albumList = new LinkedList<>(albums.values());

			albumList.sort(Comparator.comparing((AlbumType album) -> album.getArtist().toLowerCase())
					.thenComparing(AlbumType::getYear));

			result.setAlbums(new Albums());

			result.getAlbums().getAlbum().addAll(albumList);

		} catch (SQLException e) {
			log.error("Database error: ", e);
			throw new IllegalStateException("Database error while retrieving albums", e);
		}

		return result;
	}
}
