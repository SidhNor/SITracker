package com.andrada.sitracker.task;

import android.content.Context;
import android.os.AsyncTask;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiSQLiteHelper;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class AddAuthorTask extends AsyncTask<String, Integer, String> {
	
	public interface IAuthorTaskCallback {
		public void deliverResults(String message);
        public void operationStart();
        public void onProgressUpdate(int percent);
	}

	private SiSQLiteHelper helper;
    private IAuthorTaskCallback mReceiver;
    private Context context;
	
	
	public AddAuthorTask(Context context, IAuthorTaskCallback receiver) {
        this.context = context;
		helper = new SiSQLiteHelper(context);
		this.mReceiver = receiver;
	}
	
	@Override
	protected String doInBackground(String... args) {
        String message = "";
		for (String url : args) {
			try {
                if (!url.endsWith(Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH)) {
                    url = (url.endsWith("/")) ? url + Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH : url + Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH;
                }

                if (!url.startsWith(Constants.HTTP_PROTOCOL) && !url.startsWith(Constants.HTTPS_PROTOCOL)) {
                    url = Constants.HTTP_PROTOCOL + url;
                }

                if (helper.getAuthorDao().queryBuilder().where().eq("url", url).query().size() != 0){
                    throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_ALREADY_EXISTS);
                }

                publishProgress(10);
				HttpRequest request = HttpRequest.get(new URL(url));
                if (request.code() == 404) {
                    throw new MalformedURLException();
                }
                publishProgress(70);
                String body = SamlibPageParser.sanitizeHTML(request.body());
                publishProgress(80);
				Author author = new Author();
				author.setName(SamlibPageParser.getAuthor(body));
                author.setUpdateDate(SamlibPageParser.getAuthorUpdateDate(body));
				author.setUrl(url);
                publishProgress(85);
				helper.getAuthorDao().create(author);
				int i = helper.getAuthorDao().extractId(author);
				List<Publication> items = SamlibPageParser.getPublications(body, url, i);
				for (Publication publication : items) {
					helper.getPublicationDao().create(publication);
				}
                publishProgress(99);
			} catch (HttpRequestException e) {
                message = context.getResources().getString(R.string.cannot_add_author_network);
			} catch (MalformedURLException e) {
                message = context.getResources().getString(R.string.cannot_add_author_malformed);
			} catch (SQLException e) {
                message = context.getResources().getString(R.string.cannot_add_author_internal);
			} catch (AddAuthorException e) {
                switch (e.getError()) {
                    case AUTHOR_ALREADY_EXISTS:
                        message = context.getResources().getString(R.string.cannot_add_author_already_exits);
                        break;
                    case AUTHOR_DATE_NOT_FOUND:
                        message = context.getResources().getString(R.string.cannot_add_author_no_update_date);
                        break;
                    case AUTHOR_NAME_NOT_FOUND:
                        message = context.getResources().getString(R.string.cannot_add_author_no_name);
                        break;
                    default:
                        message = context.getResources().getString(R.string.cannot_add_author_unknown);
                        break;
                }

            }

		}
		return message;
	}

    @Override
    protected void onPreExecute() {
        if (mReceiver != null) {
            mReceiver.operationStart();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        if (mReceiver != null) {
            mReceiver.onProgressUpdate(values[0]);
        }
    }

	@Override
	protected void onPostExecute(String result) {
        if (mReceiver != null) {
            mReceiver.deliverResults(result);
        }
	}
}
