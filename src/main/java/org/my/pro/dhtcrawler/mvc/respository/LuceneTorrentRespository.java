package org.my.pro.dhtcrawler.mvc.respository;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.my.pro.dhtcrawler.TorrentRespository;
import org.my.pro.dhtcrawler.btdownload.BtFileInfo;
import org.my.pro.dhtcrawler.btdownload.BtInfo;
import org.my.pro.dhtcrawler.mvc.pojo.PageBean;
import org.my.pro.dhtcrawler.util.BtUtils;
import org.my.pro.dhtcrawler.util.GsonUtils;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.google.gson.reflect.TypeToken;

public class LuceneTorrentRespository implements TorrentRespository {

	private Log log = LogFactory.getLog(LuceneTorrentRespository.class);

	private Directory directory;
	private Analyzer analyzer;
	private IndexWriterConfig config;
	private IndexWriter writer;
	private DirectoryReader reader;

	private static volatile LuceneTorrentRespository instance;

	// 定时任务
	private ScheduledExecutorService scheduledExecutor;

	private ConcurrentHashMap<String, Object> hashSet;
	private static final Object EMPTY = new Object();

	private LuceneTorrentRespository() {

		try {
			directory = FSDirectory.open(Paths.get("data"));
			analyzer = new IKAnalyzer();
			config = new IndexWriterConfig(analyzer);
			writer = new IndexWriter(directory, config);
			//
			//
			reader = DirectoryReader.open(writer);
			//
			// 一分钟提交一次文档到磁盘，并重新设置DirectoryReader
			scheduledExecutor = Executors.newScheduledThreadPool(1);
			scheduledExecutor.scheduleAtFixedRate(new Runnable() {

				@Override
				public void run() {
					// 提交
					try {
						writer.commit();
						DirectoryReader newReader = DirectoryReader.openIfChanged(reader);
						if (null != newReader) {
							reader.close();
							reader = newReader;
						}
					} catch (Exception e) {
						e.printStackTrace();
						log.error(e.getMessage());
					}
				}
			}, 1000 * 60, 1000 * 60, TimeUnit.MILLISECONDS);
			// 加载所有已获得哈希
			//
			hashSet = new ConcurrentHashMap<String, Object>();
			new Thread(new Runnable() {

				@Override
				public void run() {
					putAllHashToMap(hashSet);
					log.info("加载哈希缓存完成.数量" + hashSet.size());
				}
			}).start();
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}

	}

	public static LuceneTorrentRespository getInstance() {
		if (null == instance) {
			synchronized (LuceneTorrentRespository.class) {
				if (null == instance) {
					instance = new LuceneTorrentRespository();
				}
			}
		}

		return instance;
	}

	@Override
	public boolean exist(String hash) {
		return hashSet.containsKey(hash);
	}

	@Override
	public long saveBtInfo(BtInfo btInfo) {
		long rs = 0;
		try {
			Document doc = new Document();
			doc.add(new StringField("hash", btInfo.getHash(), Field.Store.YES));
			doc.add(new TextField("name", btInfo.getName(), Field.Store.YES));
			doc.add(new LongField("length", btInfo.getLength(), Field.Store.YES));
			doc.add(new TextField("time", btInfo.getTime(), Field.Store.YES));
			doc.add(new TextField("files", GsonUtils.toJsonString(btInfo.getFiles()), Field.Store.YES));
			//
			rs = writer.updateDocument(new Term("hash", btInfo.getHash()), doc);
			hashSet.put(btInfo.getHash(), EMPTY);
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return rs;
	}

	public long count() {
		return reader.numDocs();
	}

	@Override
	public BtInfo findByHash(String hash) {

		try {
			Query query = new TermQuery(new Term("hash", hash));
			IndexSearcher indexSearcher = new IndexSearcher(reader);

			TopDocs hits = indexSearcher.search(query, 10);
			StoredFields storedFields = indexSearcher.storedFields();
			for (ScoreDoc hit : hits.scoreDocs) {
				Document doc = storedFields.document(hit.doc);

				return read(doc);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void putAllHashToMap(ConcurrentHashMap<String, Object> map) {
		MatchAllDocsQuery allDocsQuery = new MatchAllDocsQuery();
		IndexSearcher indexSearcher = new IndexSearcher(reader);

		try {
			int count = indexSearcher.count(allDocsQuery);
			TopDocs hits = indexSearcher.search(allDocsQuery, count);
			StoredFields storedFields = indexSearcher.storedFields();

			for (int i = 0; i < hits.scoreDocs.length; i++) {
				Document doc = storedFields.document(hits.scoreDocs[i].doc);
				String hash = doc.get("hash");
				if (null != hash) {
					map.put(hash, EMPTY);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public PageBean search(String ss, int page, int pageSize) {
		PageBean bean = new PageBean();
		List<BtInfo> rs = new ArrayList<BtInfo>();
		//
		try {
			//
			IndexSearcher indexSearcher = new IndexSearcher(reader);

			Query query1 = new TermQuery(new Term("name", ss));
			Query query2 = new TermQuery(new Term("files", ss));
			Query query3 = new TermQuery(new Term("hash", ss));
			;
			BooleanQuery booleanQuery = new BooleanQuery.Builder().add(query1, BooleanClause.Occur.SHOULD)
					.add(query2, BooleanClause.Occur.SHOULD).add(query3, BooleanClause.Occur.SHOULD).build();
			int count = indexSearcher.count(booleanQuery);
			int queryCount = Math.min(count, 1000);
			TopDocs hits = indexSearcher.search(booleanQuery, queryCount);
			StoredFields storedFields = indexSearcher.storedFields();
			//
			int start = (page - 1) * pageSize;
			int end = Math.min(start + pageSize, hits.scoreDocs.length);

			for (int i = start; i < end; i++) {
				Document doc = storedFields.document(hits.scoreDocs[i].doc);
				rs.add(read(doc));
			}
			//
			bean.setBtInfos(rs);
			bean.setPage(page);
			bean.setPageSize(pageSize);
			bean.setCount(queryCount);

			//
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		//
		return bean;
	}

	@Override
	public long del(String hash) {

		Query query = new TermQuery(new Term("hash", hash));

		try {
			return writer.deleteDocuments(query);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;

	}

	private BtInfo read(Document doc) {

		String hash = doc.get("hash");
		String name = doc.get("name");

		long length = Long.parseLong(doc.get("length"));
		String time = doc.get("time");
		String files = doc.get("files");
		//
		// Type
		Type type = new TypeToken<List<BtFileInfo>>() {
		}.getType();

		List<BtFileInfo> btfileinfos = GsonUtils.GSON.fromJson(files, type);
		//
		BtInfo btInfo = new BtInfo();
		btInfo.setHash(hash);
		btInfo.setName(name);
		btInfo.setLength(length);
		btInfo.setTime(time);
		btInfo.setFiles(btfileinfos);
		//
		btInfo.setFileNum(btfileinfos.size());
		btInfo.setSize(BtUtils.lengthStr(length));
		return btInfo;
	}

}
