package dk.purplegreen.musiclibrary.tools;

import javax.validation.constraints.NotNull;

public class Song {

	@NotNull
	private String artist;
	@NotNull
	private String album;
	@NotNull
	private Integer year;
	@NotNull
	private String title;
	@NotNull
	private Integer track;
	@NotNull
	private Integer disc;

	public Song(String artist, String album, Integer year, String title, Integer track, Integer disc) {
		this.artist = artist;
		this.album = album;
		this.year = year;
		this.title = title;
		this.track = track;
		this.disc = disc;
	}

	@Override
	public String toString() {
		return String.join(", ", artist, album, year == null ? null : year.toString(), title,
				track == null ? null : track.toString(), disc == null ? null : disc.toString());
	}

}
