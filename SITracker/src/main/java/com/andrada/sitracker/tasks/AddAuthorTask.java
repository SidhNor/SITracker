package com.andrada.sitracker.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class AddAuthorTask extends AsyncTask<String, Integer, String> {

	public interface IAuthorTaskCallback {
		public void onAuthorAddCompleted(String message);
        public void onAuthorAddStarted();
	}

    private IAuthorTaskCallback mReceiver;
    private Context context;
    private SiDBHelper helper;
	
	
	public AddAuthorTask(Context context, IAuthorTaskCallback receiver) {
        this.context = context;
		this.mReceiver = receiver;
	}
	
	@Override
	protected String doInBackground(String... args) {
        String message = "";
		for (String url : args) {
			try {
                if (url.equals("") || !url.matches(Constants.SIMPLE_URL_REGEX)) {
                    throw new MalformedURLException();
                }

                if (!url.endsWith(Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH)) {
                    url = (url.endsWith("/")) ? url + Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH : url + Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH;
                }

                if (!url.startsWith(Constants.HTTP_PROTOCOL) && !url.startsWith(Constants.HTTPS_PROTOCOL)) {
                    url = Constants.HTTP_PROTOCOL + url;
                }

                if (helper.getAuthorDao().queryBuilder().where().eq("url", url).query().size() != 0){
                    throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_ALREADY_EXISTS);
                }

				HttpRequest request = HttpRequest.get(new URL(url));
                if (request.code() == 404) {
                    throw new MalformedURLException();
                }
                String body = SamlibPageParser.sanitizeHTML(request.body());
				Author author = new Author();
				author.setName(SamlibPageParser.getAuthor(body));
                author.setUpdateDate(SamlibPageParser.getAuthorUpdateDate(body));
				author.setUrl(url);
				helper.getAuthorDao().create(author);
				List<Publication> items = SamlibPageParser.getPublications(body, author);
				for (Publication publication : items) {
					helper.getPublicationDao().create(publication);
				}
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
        helper = OpenHelperManager.getHelper(this.context, SiDBHelper.class);
        if (mReceiver != null) {
            mReceiver.onAuthorAddStarted();
        }
    }

	@Override
	protected void onPostExecute(String result) {
        OpenHelperManager.releaseHelper();
        if (mReceiver != null) {
            mReceiver.onAuthorAddCompleted(result);
        }
	}
}
