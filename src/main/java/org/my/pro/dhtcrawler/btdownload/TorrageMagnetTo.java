package org.my.pro.dhtcrawler.btdownload;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.routing.HttpRoute;
import org.my.pro.dhtcrawler.Saver;
import org.my.pro.dhtcrawler.saver.TxtMagnetSaver;

import be.adaxisoft.bencode.BDecoder;
import be.adaxisoft.bencode.BEncodedValue;

@Deprecated
public class TorrageMagnetTo extends HttpMagnetToTorrent {

	// https://t.torrage.info/download?h=640FE84C613C17F663551D218689A64E8AEBEABE

	private static String URL_TMP = "https://t.torrage.info/download?h=%s";
	private static HttpRoute route = new HttpRoute(new HttpHost("t.torrage.info", 80));

	public TorrageMagnetTo(Saver magnetSaver) {
		super(magnetSaver);
	}

	@Override
	public HttpPost httpRequest(String magnet) {
		return new HttpPost(String.format(URL_TMP, magnet.toUpperCase()));
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

	public static void main(String[] args) {
		Saver magnetSaver = new TxtMagnetSaver();
		String hash = "64DCA5F30A4C093E58CDF17FC937DB52D3BE4228";
		System.out.println(hash.toUpperCase());

		//
		// TorrageMagnetTo magnetTo = new TorrageMagnetTo(magnetSaver);
		ItorrentsMagnetTo itorrentsMagnetTo = new ItorrentsMagnetTo(magnetSaver);
		try {
			// magnetTo.convert(hash);
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			itorrentsMagnetTo.convert(hash);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
