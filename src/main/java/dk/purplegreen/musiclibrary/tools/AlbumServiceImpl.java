package dk.purplegreen.musiclibrary.tools;

import javax.jws.WebService;

import dk.purplegreen.musiclibrary.tools.ws.AlbumService;
import dk.purplegreen.musiclibrary.tools.ws.AlbumType;
import dk.purplegreen.musiclibrary.tools.ws.Albums;
import dk.purplegreen.musiclibrary.tools.ws.GetAlbums;
import dk.purplegreen.musiclibrary.tools.ws.GetAlbumsResponse;
import dk.purplegreen.musiclibrary.tools.ws.SongType;

@WebService(endpointInterface = "dk.purplegreen.musiclibrary.tools.ws.AlbumService", targetNamespace = "http://www.purplegreen.dk/albumservice", portName = "AlbumServiceSOAP", serviceName = "AlbumService")
public class AlbumServiceImpl implements AlbumService {

	@Override
	public GetAlbumsResponse getAlbums(GetAlbums in) {

		try {
			
		
		
		GetAlbumsResponse result = new GetAlbumsResponse();

		Albums albums = new Albums();

		AlbumType album=new AlbumType();
		album.setSongs(new AlbumType.Songs());
		album.setArtist("Judas Priest");
		album.setTitle("Stained Class");
		album.setYear(1978);
		
		SongType song=new SongType();
		song.setTitle("Exciter");
		song.setTrack(1);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		song=new SongType();
		song.setTitle("White Heat, Red Hot");
		song.setTrack(2);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		albums.getAlbum().add(album);
		
		album=new AlbumType();
		album.setSongs(new AlbumType.Songs());
		album.setArtist("Van Halen");
		album.setTitle("1984");
		album.setYear(1984);
		
		song=new SongType();
		song.setTitle("1984");
		song.setTrack(1);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		song=new SongType();
		song.setTitle("Jump");
		song.setTrack(2);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		albums.getAlbum().add(album);
		
		album=new AlbumType();
		album.setSongs(new AlbumType.Songs());
		album.setArtist("Gasolin");
		album.setTitle("Gør det noget");
		album.setYear(1977);
		
		song=new SongType();
		song.setTitle("Det bedste til mig og mine venner");
		song.setTrack(1);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		song=new SongType();
		song.setTitle("Smukke Møller");
		song.setTrack(2);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		song=new SongType();
		song.setTitle("Længes hjem");
		song.setTrack(10);
		song.setDisc(1);
		album.getSongs().getSong().add(song);
		
		albums.getAlbum().add(album);
		
		result.setAlbums(albums);

		return result;
		
		} catch (Exception e) {
			e.printStackTrace();
		throw e;
		}
	}

}
