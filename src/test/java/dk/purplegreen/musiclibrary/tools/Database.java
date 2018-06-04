package dk.purplegreen.musiclibrary.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.rules.ExternalResource;

public class Database extends ExternalResource {

	static {
		System.setProperty("derby.stream.error.file", "logs/derby.log");
	}

	private static final String DB_URL = "jdbc:derby:memory:musiclibrarytestdb";

	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection(DB_URL + ";user=musiclibrary;password=musiclibrary");
	}

	@Override
	protected void before() throws Throwable {

		try (Connection con = DriverManager.getConnection(DB_URL + ";create=true")) {

			// Create schema
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(Database.class.getResourceAsStream("/musiclibrary-derby.sql")))) {

				StringBuilder cmd = new StringBuilder();

				for (String line; (line = reader.readLine()) != null;) {

					line = line.trim();

					if (line.isEmpty())
						continue;

					if (line.startsWith("--"))
						continue;

					if (line.endsWith(";")) {
						cmd.append(line.substring(0, line.length() - 1));

						try (Statement stmt = con.createStatement()) {
							stmt.execute(cmd.toString());
						}
						cmd = new StringBuilder();
					} else
						cmd.append(line);
				}
			}

			// Create test data
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(Database.class.getResourceAsStream("/testdata.sql")))) {

				for (String line; (line = reader.readLine()) != null;) {

					String cmd = line.trim();

					if (cmd.isEmpty())
						continue;

					if (cmd.startsWith("--"))
						continue;

					try (Statement stmt = con.createStatement()) {
						stmt.execute(cmd);
					}
				}
			}
		}
	}

	@Override
	protected void after() {
		try {
			DriverManager.getConnection(DB_URL + ";drop=true");
		} catch (SQLException e) {
			if (!("08006".equals(e.getSQLState()))) {
				e.printStackTrace();
			}
		}
	}
}
