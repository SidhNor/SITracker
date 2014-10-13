package com.andrada.sitracker.ui.components;

import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;

import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

@EViewGroup(R.layout.dialog_about)
public class AboutDialogView extends RelativeLayout {


    @ViewById
    TextView generalText;

    @ViewById
    TextView versionText;

    public AboutDialogView(@NotNull Context context) {
        super(context);
    }

    public void bindData(String versionTxt, CharSequence aboutBody) {
        versionText.setText(versionTxt);
        generalText.setText(aboutBody);
        generalText.setMovementMethod(new LinkMovementMethod());
    }
}
