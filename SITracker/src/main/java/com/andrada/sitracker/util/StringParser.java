package com.andrada.sitracker.util;


import android.util.Log;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.Publication;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringParser {

    public static String getAuthor(String pageContent) {
        int index = pageContent.indexOf('.', pageContent.indexOf("<title>")) + 1;
        int secondPointIndex = pageContent.indexOf(".", index);
        String authorName = pageContent.substring(index, secondPointIndex);
        if (authorName == null || authorName.trim().isEmpty()) {
            //TODO Handle author add error
        }
        return authorName;
	}

	public static List<Publication> getPublications(String body, String baseUrl, long authorId) {
		ArrayList<Publication> publicationList = new ArrayList<Publication>();
        String page = sanitizeHTML(body);

        Pattern pattern = Pattern.compile("<DL>\\s*<DT>\\s*<li>.*?<A HREF=(.*?)><b>\\s*(.*?)\\s*</b></A>.*?<b>(\\d+)k</b>.*?<small>(?:Оценка:<b>((\\d+(?:\\.\\d+)?).*?)</b>.*?)?\\s*\\\"(.*?)\\\"\\s*(.*?)?\\s*(?:<A HREF=\\\"(.*?)\\\">Комментарии:\\s*((\\d+).*?)</A>\\s*)?</small>.*?(?:<br>\\s*<dd>\\s*<font.*?>(.*?)</font>)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(page);
        while (matcher.find()) {

            Publication item = new Publication();
            if(!baseUrl.endsWith("/")){
                baseUrl+="/";
            }
            item.setAuthorID(authorId);

            //Group 1 - LinkToText
            String itemURL = matcher.group(1) == null ? "" : matcher.group(1);
            item.setUrl(baseUrl+itemURL);
            //Group 2 - NameOfText
            String itemTitle = matcher.group(2) == null ? "" : matcher.group(2);
            item.setName(itemTitle);
            //Group 3 - SizeOfText
            String sizeOfText = matcher.group(3) == null ? "0" : matcher.group(3);
            //Group 4 - DescriptionOfRating
            String descriptionOfRating = matcher.group(4) == null ? "" : matcher.group(4);
            //Group 5 - Rating
            String rating = matcher.group(5) == null ? "0" : matcher.group(5);
            //Group 6 - Section
            String categoryName = matcher.group(6) == null ? "" : matcher.group(6);
            item.setCategory(categoryName);
            //Group 7 - Genres
            String genre =  matcher.group(7) == null ? "" : matcher.group(7);
            //Group 8 - Link to Comments
            String commentsUrl = matcher.group(8) == null ? "" : matcher.group(8);
            //Group 9 - CommentsDescription
            String commentsDescription = matcher.group(9) == null ? "" : matcher.group(9);
            //Group 10 - CommentsCount
            String commentsCount = matcher.group(10) == null ? "0" : matcher.group(10);
            //Group 11 - Description
            String itemDescription = matcher.group(11) == null ? "" : matcher.group(11);
            item.setDescription(itemDescription);
            publicationList.add(item);
        }
        //Find the comment we need



		/*
		for (int i = 1; i < pubArray.length; i++) {
			publicationList.addAll(processCategory(pubArray[i],baseUrl, authorId));
		}*/
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

    private static String sanitizeHTML(String value) {
        value = value.replaceAll("&nbsp;", " ");
        value = value.replaceAll("&quot;", "\"");
        return value.replaceAll("<br />", "<br>");
    }
}
