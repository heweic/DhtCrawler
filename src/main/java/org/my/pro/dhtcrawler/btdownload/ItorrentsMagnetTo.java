package org.my.pro.dhtcrawler.btdownload;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.routing.HttpRoute;
import org.my.pro.dhtcrawler.Saver;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;

/**
 * 
 * itorrents.org/torrent/489B90DD4F05A767C2D8E76D343A82F94BA7ECA4.torrent
 * http://torcache.net/torrent/18981BC9759950B4715AD46ADCAF514E6A773CFE.torrent
 */
public class ItorrentsMagnetTo extends HttpMagnetToTorrent {

	public ItorrentsMagnetTo(Saver magnetSaver) {
		super(magnetSaver);
	}

	private static String FORMAT = "https://itorrents.org/torrent/%s.torrent";

	private static HttpRoute route = new HttpRoute(new HttpHost("itorrents.org", 80));

	@Override
	public HttpGet httpRequest(String magnet) {
		HttpGet get = new HttpGet(String.format(FORMAT, magnet.toUpperCase()));

		get.setHeader("Referer", "http://itorrents.org");
		get.setHeader("User-Agent",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.81 Safari/537.36");

		return get;
	}

	@Override
	public BtInfo handlerEntity(HttpEntity entity, String hash) throws Exception {

		try {

			BEncodedValue value = BDecoder.decode(entity.getContent());
			return new BtInfo(value, hash);

		} catch (Exception e) {
			throw e;
		}
	}

	@Override
	public HttpRoute httpRoute() {
		return route;
	}

}
