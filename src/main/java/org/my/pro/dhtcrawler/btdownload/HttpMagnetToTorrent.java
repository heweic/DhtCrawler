package org.my.pro.dhtcrawler.btdownload;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.my.pro.dhtcrawler.Saver;

public abstract class HttpMagnetToTorrent implements MagnetToTorrent {

	private static PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
	private static CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();

	private Saver magnetSaver;

	public HttpMagnetToTorrent(Saver magnetSaver) {

		cm.setMaxTotal(200);
		cm.setDefaultMaxPerRoute(20);
		cm.setMaxPerRoute(httpRoute(), 50);

		this.magnetSaver = magnetSaver;
	}

	@Override
	public void convert(String hash) throws Exception {

		try {
			CloseableHttpResponse response = httpClient.execute(httpRequest(hash), HttpClientContext.create());
			HttpEntity entity = response.getEntity();
			try {

				BtInfo btInfo = handlerEntity(entity, hash);
				if (null != btInfo) {
					magnetSaver.saveBtInfo(btInfo);
				}
			} finally {
				try {
					EntityUtils.consume(entity);
				} catch (Exception e) {
				}
				try {
					response.close();
				} catch (Exception e) {
				}
			}

		} catch (Exception e) {
			throw e;
		}

	}

	public abstract HttpUriRequest httpRequest(String magnet);

	public abstract BtInfo handlerEntity(HttpEntity entity, String hash) throws Exception;

	public abstract HttpRoute httpRoute();
}
