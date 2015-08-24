package com.opendoorlogistics.codefromweb.jxmapviewer2;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

/**
 * Fork of jxmapviewer2's LocalResponseCache which removes tiles which are
 * older than a set number of days so we can get the local cache updated
 * regularly without the overhead of checking if a tile is updated (which
 * can be slow on a slow internet connection).
 * 
 * We also validate the tiles; checking they are a non-corrupt image.
 * This response cache can only therefore be used for tiles (but it will
 * only be used for downloading data from the base URL, so if its called
 * for another website downloading data that isn't a tile, 
 * the tile validation code won't be called).
 * 
 * Original author joshy
 */
public class ExpiringOSMLocalResponseCache extends ResponseCache {
	public static final long EXPIRY_HOUR_COUNT = 24*7;
	
	private final File cacheDir;

	private boolean checkForUpdates;

	private HashSet<String> acceptedBaseURLs = new HashSet<String>();
	
	/**
	 * Private constructor to prevent instantiation.
	 * 
	 * @param baseURL
	 *            the URI that should be cached or <code>null</code> (for all URLs)
	 * @param cacheDir
	 *            the cache directory
	 * @param checkForUpdates
	 *            true if the URL is queried for newer versions of a file first
	 */
	public ExpiringOSMLocalResponseCache( File cacheDir, boolean checkForUpdates) {
		this.cacheDir = cacheDir;
		this.checkForUpdates = checkForUpdates;

		if (!cacheDir.exists()) {
			cacheDir.mkdirs();
		}
	}


	public void addAcceptedBasedURL(String s){
		acceptedBaseURLs.add(s.trim().toLowerCase());
	}
	
	protected boolean cacheURI(URI remoteUri){
		if (acceptedBaseURLs.size()>0) {
			String remote = remoteUri.toString();
			remote = remote.trim().toLowerCase();
			for(String accepted : acceptedBaseURLs){
				if(remote.startsWith(accepted)){
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the local File corresponding to the given remote URI.
	 * 
	 * @param remoteUri
	 *            the remote URI
	 * @return the corresponding local file
	 */
	private File getLocalFile(URI remoteUri) {
		if(!cacheURI(remoteUri)){
			return null;
		}

		StringBuilder sb = new StringBuilder();

		String host = remoteUri.getHost();
		String query = remoteUri.getQuery();
		String path = remoteUri.getPath();
		String fragment = remoteUri.getFragment();

		if (host != null) {
			sb.append(host);
		}
		if (path != null) {
			sb.append(path);
		}
		if (query != null) {
			sb.append('?');
			sb.append(query);
		}
		if (fragment != null) {
			sb.append('#');
			sb.append(fragment);
		}

		String name;

		final int maxLen = 250;

		if (sb.length() < maxLen) {
			name = sb.toString();
		} else {
			name = sb.substring(0, maxLen);
		}

		name = name.replace('?', '$');
		name = name.replace('*', '$');
		name = name.replace(':', '$');
		name = name.replace('<', '$');
		name = name.replace('>', '$');
		name = name.replace('"', '$');

		File f = new File(cacheDir, name);

		return f;
	}

	/**
	 * @param remoteUri
	 *            the remote URI
	 * @param localFile
	 *            the corresponding local file
	 * @return true if the resource at the given remote URI is newer than the resource cached locally.
	 */
	private static boolean isUpdateAvailable(URI remoteUri, File localFile) {
		URLConnection conn;
		try {
			conn = remoteUri.toURL().openConnection();
		} catch (MalformedURLException ex) {
			return false;
		} catch (IOException ex) {
			return false;
		}
		if (!(conn instanceof HttpURLConnection)) {
			// don't bother with non-http connections
			return false;
		}

		long localLastMod = localFile.lastModified();
		long remoteLastMod = 0L;
		HttpURLConnection httpconn = (HttpURLConnection) conn;
		// disable caching so we don't get in feedback loop with ResponseCache
		httpconn.setUseCaches(false);
		try {
			httpconn.connect();
			remoteLastMod = httpconn.getLastModified();
		} catch (IOException ex) {
			// log.error("An exception occurred", ex);();
			return false;
		} finally {
			httpconn.disconnect();
		}

		return (remoteLastMod > localLastMod);
	}

	@Override
	public CacheResponse get(URI uri, String rqstMethod, Map<String, List<String>> rqstHeaders) throws IOException {
		File localFile = getLocalFile(uri);

		if (localFile == null) {
			// we don't want to cache this URL
			return null;
		}

		if (!localFile.exists()) {
			// the file isn't already in our cache, return null
			return null;
		}

		boolean valid = true;
		BasicFileAttributes attributes = Files.readAttributes(localFile.toPath(), BasicFileAttributes.class);
		if(attributes !=null){
			FileTime time = attributes.lastModifiedTime();
			if(time==null){
				time = attributes.creationTime();
			}
			if(time!=null){
				// This comparison may or may not be a bit off depending on 
				// timezones, but the max error should be ~1 day and we
				// set the expiry longer than this anyway...
				// Also we should ensure the calculation is done in long, not int...
				long age = new Date().getTime()- time.toMillis();
				long limit = EXPIRY_HOUR_COUNT * 60L * 60L * 1000L;
				valid = age < limit;
			}
		}
		
		if(valid){
			try {
				byte [] bytes = IOUtils.toByteArray(localFile.toURI());
				if(ImageIO.read(new ByteArrayInputStream(bytes))==null){
					valid = false;
				}	
			} catch (Throwable e) {
				valid = false;
			}
		}
		
		if(!valid){
			try {
				localFile.delete();
			} catch (Throwable e) {
				// TODO: handle exception
			}
			return null;	
		}
		
		if (checkForUpdates) {
			if (isUpdateAvailable(uri, localFile)) {
				// there is an update available, so don't return cached version
				return null;
			}
		}

		return new LocalCacheResponse(localFile, rqstHeaders);
	}

	@Override
	public CacheRequest put(URI uri, URLConnection conn) throws IOException {
		// only cache http(s) GET requests
		if (!(conn instanceof HttpURLConnection) || !(((HttpURLConnection) conn).getRequestMethod().equals("GET"))) {
			return null;
		}

		File localFile = getLocalFile(uri);

		if (localFile == null) {
			// we don't want to cache this URL
			return null;
		}

		new File(localFile.getParent()).mkdirs();
		return new LocalCacheRequest(localFile);
	}

	private class LocalCacheResponse extends CacheResponse {
		private FileInputStream fis;
		private final Map<String, List<String>> headers;

		private LocalCacheResponse(File localFile, Map<String, List<String>> rqstHeaders) {
			try {
				this.fis = new FileInputStream(localFile);
			} catch (FileNotFoundException ex) {
			}
			this.headers = rqstHeaders;
		}

		@Override
		public Map<String, List<String>> getHeaders() throws IOException {
			return headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return fis;
		}
	}

	private class LocalCacheRequest extends CacheRequest {
		private final File localFile;
		private FileOutputStream fos;

		private LocalCacheRequest(File localFile) {
			this.localFile = localFile;
			try {
				this.fos = new FileOutputStream(localFile);
			} catch (FileNotFoundException ex) {
			}
		}

		@Override
		public OutputStream getBody() throws IOException {
			return fos;
		}

		@Override
		public void abort() {
			// abandon the cache attempt by closing the stream and deleting
			// the local file
			try {
				fos.close();
				localFile.delete();
			} catch (IOException e) {
			}
		}
	}
}
