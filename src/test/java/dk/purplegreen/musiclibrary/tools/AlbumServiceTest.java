package dk.purplegreen.musiclibrary.tools;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import dk.purplegreen.musiclibrary.tools.ws.GetAlbums;
import dk.purplegreen.musiclibrary.tools.ws.GetAlbumsResponse;

@RunWith(MockitoJUnitRunner.class)
public class AlbumServiceTest {

	@Rule
	public Database database = new Database();

	@InjectMocks
	private AlbumServiceImpl albumService;

	@Mock
	private DataSource ds;

	@Test
	public void testAlbumService() throws Exception {

		when(ds.getConnection()).thenReturn(database.getConnection());

		GetAlbumsResponse result = albumService.getAlbums(new GetAlbums());

		assertEquals("Wrong number of albums", 3, result.getAlbums().getAlbum().size());
	}

}
