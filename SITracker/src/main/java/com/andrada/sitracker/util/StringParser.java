package com.andrada.sitracker.util;


import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.Publication;

import java.util.ArrayList;
import java.util.List;

public class StringParser {
	public static String getAuthor(String body) {
		int start;
		int end;
		start = body.indexOf(Constants.AUTHOR_START_BLOCK);
		body = body.substring(start + Constants.AUTHOR_START_BLOCK.length(),
				body.length());
		end = body.indexOf(Constants.AUTHOR_END_BLOCK);
		String author = body.substring(0, end);
		return author;
	}

	public static List<Publication> getPublications(String body, String baseUrl, long authorId) {
		ArrayList<Publication> publicationList = new ArrayList<Publication>();
		int start;
		int end;
		start = body.indexOf(Constants.START_PUBLICATIONS_BLOCK);
		body = body.substring(start, body.length());
		end = body.indexOf(Constants.END_PUBLICATIONS_BLOCK);
		String publicationsBody = body.substring(0, end);
		String[] pubArray = publicationsBody
				.split(Constants.CATEGORIES_DIVIDER);
		for (int i = 1; i < pubArray.length; i++) {
			publicationList.addAll(processCategory(pubArray[i],baseUrl, authorId));
		}
		return publicationList;
	}

	private static List<Publication> processCategory(String input, String baseUrl, long authorId) {
		ArrayList<Publication> publicationList = new ArrayList<Publication>();
		String categoryName = input.substring(
				input.indexOf(Constants.CATEGORIES_NAME_START) + Constants.CATEGORIES_NAME_START.length(),
				input.indexOf(Constants.CATEGORIES_NAME_END));
		String[] categoryItems = input.split(Constants.ITEMS_DIVIDER);
		for (int i = 1; i < categoryItems.length; i++) {
			String rawItem = categoryItems[i];
			String itemURL = rawItem.substring(rawItem.indexOf(Constants.ITEM_URL_START)+Constants.ITEM_URL_START.length(), rawItem.indexOf(Constants.ITEM_URL_END));
			String itemTitle = rawItem.substring(rawItem.indexOf(Constants.ITEM_TITLE_START)+Constants.ITEM_TITLE_START.length(), rawItem.indexOf(Constants.ITEM_TITLE_END));;
			String itemDescription ="";
			if (rawItem.indexOf(Constants.ITEM_DESCRIPTION_END)!=-1){
				itemDescription = rawItem.substring(rawItem.indexOf(Constants.ITEM_DESCRIPTION_START)+Constants.ITEM_DESCRIPTION_START.length(), rawItem.indexOf(Constants.ITEM_DESCRIPTION_END));
			}
			Publication item = new Publication();
			if(!baseUrl.endsWith("/")){
				baseUrl+="/";
			}
			item.setUrl(baseUrl+itemURL);
			item.setAuthorID(authorId);
			item.setName(itemTitle);
			item.setCategory(categoryName);
			item.setDescription(itemDescription);
			publicationList.add(item);
		}

		return publicationList;
	}
}
