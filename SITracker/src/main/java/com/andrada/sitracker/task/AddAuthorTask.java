package com.andrada.sitracker.task;

import android.content.Context;
import android.os.AsyncTask;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiSQLiteHelper;
import com.andrada.sitracker.util.StringParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class AddAuthorTask extends AsyncTask<String, Void, Void> {
	
	public interface ITaskCallback{
		public void deliverResults();
	}

	SiSQLiteHelper helper;
	ITaskCallback reciever;
	
	
	public AddAuthorTask(Context context, ITaskCallback reciever) {
		helper = new SiSQLiteHelper(context);
		this.reciever = reciever;
	}
	
	@Override
	protected Void doInBackground(String... args) {
		for (String url : args) {

			try {
				HttpRequest request = HttpRequest.get(new URL(url));
				String body = request.body();

				Author author = new Author();
				
				author.setName(StringParser.getAuthor(body));
				author.setUrl(url);
				helper.getAuthorDao().create(author);
				int i = helper.getAuthorDao().extractId(author).intValue();
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
		reciever.deliverResults();
	}
}