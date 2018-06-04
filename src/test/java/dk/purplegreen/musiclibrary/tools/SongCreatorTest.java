package dk.purplegreen.musiclibrary.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.cache.Cache;
import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SongCreatorTest {

	@Rule
	public Database database = new Database();

	@Mock
	private DataSource ds;

	@Mock
	private Cache<String, Integer> artistCache;

	@Mock
	private Cache<String, Integer> albumCache;

	@InjectMocks
	private SongCreator songCreator;

	@Test
	public void testCreateSong() throws Exception {
		when(ds.getConnection()).thenReturn(database.getConnection());

		Integer id = songCreator.createSong(new Song("Deep Purple", "Machine Head", 1972, "Highway Star", 1, 1));

		try (Connection con = database.getConnection()) {

			try (PreparedStatement stmt = con.prepareStatement("SELECT album_id, song_title FROM song WHERE id = " + id);
					ResultSet rs = stmt.executeQuery()) {

				assertTrue("Song not created", rs.next());
				assertEquals("Wrong song title", "Highway Star", rs.getString("song_title"));

				verify(artistCache, times(1)).put(eq("Deep Purple"), anyInt());
				verify(albumCache, times(1)).put(endsWith("Machine Head"), anyInt());
			}
		}
	}

	@Test
	public void testCreateSongExistingAlbum() throws Exception {
		when(ds.getConnection()).thenReturn(database.getConnection());

		Integer id = songCreator.createSong(new Song("The Beatles", "Abbey Road", 1969, "Here Comes the Sun", 7, 1));

		try (Connection con = database.getConnection()) {

			try (PreparedStatement stmt = con.prepareStatement("SELECT album_id, song_title FROM song WHERE id = " + id);
					ResultSet rs = stmt.executeQuery()) {

				assertTrue("Song not created", rs.next());
				assertEquals("Wrong album id", 3, rs.getInt("album_id"));

				verify(artistCache, times(1)).put(eq("The Beatles"), eq(1));
				verify(albumCache, times(1)).put(endsWith("Abbey Road"), eq(3));
			}
		}
	}
}
