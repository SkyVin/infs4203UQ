/*
 * Author : Elvin See 
 * University Of Queensland
 */
package examples;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.sql.*;

import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.FlickrException;
import com.aetrion.flickr.REST;
import com.aetrion.flickr.photos.SearchParameters;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.tags.Tag;
import com.aetrion.flickr.tags.TagsInterface;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class PhotoMining {

	// set api keys
	static String api;
	static String serv;
	String photoID;
	REST rest;
	Flickr f;
	static Connection conn = null;
	static PreparedStatement stmt;

	public PhotoMining() throws ParserConfigurationException, IOException {

		api = "9990ace3612a8467fd1fc5d3fafbffd3";
		serv = "www.flickr.com";
		rest = new REST();
		rest.setHost(serv);
		f = new Flickr(api, rest);
		f.debugStream = false;

	}

	public Connection connDB() {

		String url = "jdbc:mysql://localhost:3306/";
		String dbName = "flickrmining";
		String driver = "com.mysql.jdbc.Driver";
		String username = "root";
		String password = null;

		try {
			Class.forName(driver);
			conn = DriverManager
					.getConnection(url + dbName, username, password);
			System.out.println("Connected to the database");
			// conn.close();
			// System.out.println("Connection Close");

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return conn;

	}

	public String getPhotoByTag(String keyWord, Connection con) {

		// create a search parameters for search of photos
		SearchParameters searchParams = new SearchParameters();
		// create a string array of tag for search of photos
		String[] tag = new String[] { keyWord };
		// set the tags to the search parameters
		searchParams.setTags(tag);

		// create a photo interface
		PhotosInterface pI = f.getPhotosInterface();
		// execute the search with entered tags
		try {
			PhotoList photoList = pI.search(searchParams, 1000, 1);
			// get searchResult and fetch the photo object
			if (photoList != null) {

				for (int i = 0; i < photoList.size(); i++) {

					Photo photo = (Photo) photoList.get(i);
					photoID = photo.getId();
					StringBuffer str = new StringBuffer();
					str.append("PhotoID: " + photoID);
					System.out.println(str);
					Statement st = con.createStatement();
					int val = st.executeUpdate("INSERT PHOTO VALUES('"
							+ photoID + "')ON DUPLICATE KEY Update PhotoID ='"
							+ photoID + "'");

					System.out.println("Row Inserted: " + val);
				}
			}

		} catch (IOException e) {

			e.printStackTrace();
		} catch (SAXException e) {

			e.printStackTrace();
		} catch (FlickrException e) {

			e.printStackTrace();
		} catch (SQLException e) {
			System.out.println("Photo ID not inserted");
			e.printStackTrace();
		}

		try {
			con.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return photoID;
	}

	public void getPhotosTag(Connection con) throws IOException,
			FlickrException, SAXException {

		try {
			Statement st = con.createStatement();
			// Statement st1 = con.prepareStatement();
			
			ResultSet rs = st.executeQuery("Select * from Photo");
			while (rs.next()) {

				String id = rs.getString("PhotoID");
				TagsInterface tI = f.getTagsInterface();
				Photo photo = tI.getListPhoto(id);
				@SuppressWarnings("unchecked")
				Collection<Tag> tags = photo.getTags();
				if (tags.isEmpty() == false) {
					Iterator<Tag> itr = tags.iterator();
					while (itr.hasNext()) {
						Tag tag = itr.next();
						String tagName = tag.getRaw();
						int i = 1;
						/*int val = st1.executeUpdate("INSERT INTO PHOTOTAG(tag_name,count)VALUES('"
										+ tagName
										+ "',"
										+ i
										+ ")ON DUPLICATE KEY Update tag_name='"
										+ tagName + "',count=count+1");*/
						// id=LAST_INSERT_ID(id)
						stmt = con.prepareStatement("INSERT INTO PHOTOTAG(tag_name,count)VALUES(?,?)ON DUPLICATE KEY Update tag_name = ? ,count=count+1");
						stmt.setString(1,tagName);
						stmt.setInt(2,i);
						stmt.setString(3,tagName);
						stmt.executeUpdate();
						stmt.close();
						System.out.println(tagName);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			con.close();
		} catch (SQLException e) {

			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws ParserConfigurationException,
			IOException, FlickrException, SAXException {

		PhotoMining pm = new PhotoMining();
		Connection conn = pm.connDB();
		// String id = pm.getPhotoByTag("queensland", conn);
		pm.getPhotosTag(conn);
	}

}
