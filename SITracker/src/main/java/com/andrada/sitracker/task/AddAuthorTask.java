package com.andrada.sitracker.task;

import android.content.Context;
import android.os.AsyncTask;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiSQLiteHelper;
import com.andrada.sitracker.util.StringParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class AddAuthorTask extends AsyncTask<String, Void, Void> {
	
	public interface ITaskCallback{
		public void deliverResults();
	}

	SiSQLiteHelper helper;
	ITaskCallback receiver;
	
	
	public AddAuthorTask(Context context, ITaskCallback receiver) {
		helper = new SiSQLiteHelper(context);
		this.receiver = receiver;
	}
	
	@Override
	protected Void doInBackground(String... args) {
		for (String url : args) {

			try {
                String normalizedLink = url;
                if (!url.endsWith("indexdate.shtml"))
                    normalizedLink = (url.endsWith("/")) ? url + "indexdate.shtml" : url + "/indexdate.shtml";

				HttpRequest request = HttpRequest.get(new URL(normalizedLink));
				String body = request.body();

				Author author = new Author();

				author.setName(StringParser.getAuthor(body));
				author.setUrl(url);
				helper.getAuthorDao().create(author);
				int i = helper.getAuthorDao().extractId(author);
				List<Publication> items = StringParser.getPublications(body, url, i);
				for (Publication publication : items) {
					helper.getPublicationDao().create(publication);
				}
			} catch (HttpRequestException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
        if (receiver != null) {
            receiver.deliverResults();
        }
	}
}