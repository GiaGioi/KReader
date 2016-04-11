/*
 * Copyright (C) 2009-2015 FBReader.ORG Limited <contact@fbreader.org>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package com.koolearn.android.kooreader.bookmark;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.koolearn.android.kooreader.api.KooReaderIntents;
import com.koolearn.android.kooreader.libraryService.BookCollectionShadow;
import com.koolearn.android.util.ViewUtil;
import com.koolearn.klibrary.core.options.ZLStringOption;
import com.koolearn.klibrary.core.resources.ZLResource;
import com.koolearn.klibrary.ui.android.R;
import com.koolearn.kooreader.book.Book;
import com.koolearn.kooreader.book.BookEvent;
import com.koolearn.kooreader.book.Bookmark;
import com.koolearn.kooreader.book.BookmarkUtil;
import com.koolearn.kooreader.book.HighlightingStyle;
import com.koolearn.kooreader.book.IBookCollection;

import java.util.ArrayList;
import java.util.List;

import yuku.ambilwarna.widget.AmbilWarnaPrefWidgetView;

public class EditBookmarkActivity extends Activity implements IBookCollection.Listener<Book> {
    private final ZLResource myResource = ZLResource.resource("editBookmark");
    private final BookCollectionShadow myCollection = new BookCollectionShadow();
    private Bookmark myBookmark;
    private StyleListAdapter myStylesAdapter;

    private void addTab(TabHost host, String id, int content) {
        final TabHost.TabSpec spec = host.newTabSpec(id);
        if (id.equals("text")) {
            spec.setIndicator("笔记");
        } else if (id.equals("style")) {
            spec.setIndicator("颜色");
        } else if (id.equals("delete")) {
            spec.setIndicator("删除");
        }
        spec.setContent(content);
        host.addTab(spec);
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.edit_bookmark);

        myBookmark = KooReaderIntents.getBookmarkExtra(getIntent());
        if (myBookmark == null) {
            finish();
            return;
        }

        final DisplayMetrics dm = getResources().getDisplayMetrics();
        final int width = Math.min(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 500, dm),
                dm.widthPixels * 9 / 10
        );
        final int height = Math.min(
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 350, dm),
                dm.heightPixels * 9 / 10
        );

        final TabHost tabHost = (TabHost) findViewById(R.id.edit_bookmark_tabhost);
        String bgValue = getIntent().getStringExtra("bgColor");

        if (bgValue != null) {
            switch (bgValue) {
                case "wallpapers/bg_green.png":
                    tabHost.setBackgroundResource(R.drawable.bg_green);
                    break;
                case "wallpapers/bg_grey.png":
                    tabHost.setBackgroundResource(R.drawable.bg_grey);
                    break;
                case "wallpapers/bg_night.png":
                    tabHost.setBackgroundResource(R.drawable.bg_white);
                    break;
                case "wallpapers/bg_vine_grey.png":
                    tabHost.setBackgroundResource(R.drawable.bg_vine_grey);
                    break;
                case "wallpapers/bg_vine_white.png":
                    tabHost.setBackgroundResource(R.drawable.bg_vine_white);
                    break;
                case "wallpapers/bg_white.png":
                    tabHost.setBackgroundResource(R.drawable.bg_white);
                    break;
            }
        }

        tabHost.setLayoutParams(new FrameLayout.LayoutParams(
                new ViewGroup.LayoutParams(width, height)
        ));
        tabHost.setup();


        addTab(tabHost, "text", R.id.edit_bookmark_content_text);
        addTab(tabHost, "style", R.id.edit_bookmark_content_style);
        addTab(tabHost, "delete", R.id.edit_bookmark_content_delete);

        final ZLStringOption currentTabOption =
                new ZLStringOption("LookNFeel", "EditBookmark", "text");
        tabHost.setCurrentTabByTag(currentTabOption.getValue());
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tag) {
                if (!"delete".equals(tag)) {
                    currentTabOption.setValue(tag);
                }
            }
        });

        final EditText editor = (EditText) findViewById(R.id.edit_bookmark_text);
        editor.setText(myBookmark.getText());
        final int len = editor.getText().length();
        editor.setSelection(len, len);

        final Button saveTextButton = (Button) findViewById(R.id.edit_bookmark_save_text_button);
        saveTextButton.setEnabled(false);
//		saveTextButton.setText(myResource.getResource("saveText").getValue());
        saveTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
                    public void run() {
                        myBookmark.setText(editor.getText().toString());
                        myCollection.saveBookmark(myBookmark);
                        saveTextButton.setEnabled(false);
                    }
                });
            }
        });
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence sequence, int start, int before, int count) {
                final String originalText = myBookmark.getText();
                saveTextButton.setEnabled(!originalText.equals(editor.getText().toString()));
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final Button deleteButton = (Button) findViewById(R.id.edit_bookmark_delete_button);
        deleteButton.setText(myResource.getResource("deleteBookmark").getValue());
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
                    public void run() {
                        myCollection.deleteBookmark(myBookmark);
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        myCollection.bindToService(this, new Runnable() {
            public void run() {
                final List<HighlightingStyle> styles = myCollection.highlightingStyles();
                if (styles.isEmpty()) {
                    finish();
                    return;
                }
                myStylesAdapter = new StyleListAdapter(styles);
                final ListView stylesList =
                        (ListView) findViewById(R.id.edit_bookmark_content_style);
                stylesList.setAdapter(myStylesAdapter);
                stylesList.setOnItemClickListener(myStylesAdapter);
                myCollection.addListener(EditBookmarkActivity.this);
            }
        });
    }

    @Override
    protected void onDestroy() {
        myCollection.unbind();
        super.onDestroy();
    }

    // method from IBookCollection.Listener
    public void onBookEvent(BookEvent event, Book book) {
        if (event == BookEvent.BookmarkStyleChanged) {
            myStylesAdapter.setStyleList(myCollection.highlightingStyles());
        }
    }

    // method from IBookCollection.Listener
    public void onBuildEvent(IBookCollection.Status status) {
    }

    private class StyleListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private final List<HighlightingStyle> myStyles;

        StyleListAdapter(List<HighlightingStyle> styles) {
            myStyles = new ArrayList<HighlightingStyle>(styles);
        }

        public synchronized void setStyleList(List<HighlightingStyle> styles) {
            myStyles.clear();
            myStyles.addAll(styles);
            notifyDataSetChanged();
        }

        public final synchronized int getCount() {
            return myStyles.size();
        }

        public final synchronized HighlightingStyle getItem(int position) {
            return myStyles.get(position);
        }

        public final long getItemId(int position) {
            return position;
        }

        public final synchronized View getView(int position, View convertView, final ViewGroup parent) {
            final View view = convertView != null
                    ? convertView
                    : LayoutInflater.from(parent.getContext()).inflate(R.layout.style_item, parent, false);
            final HighlightingStyle style = getItem(position);

            final CheckBox checkBox = (CheckBox) ViewUtil.findView(view, R.id.style_item_checkbox);
            final AmbilWarnaPrefWidgetView colorView =
                    (AmbilWarnaPrefWidgetView) ViewUtil.findView(view, R.id.style_item_color);
            final TextView titleView = ViewUtil.findTextView(view, R.id.style_item_title);
            final Button button = (Button) ViewUtil.findView(view, R.id.style_item_edit_button);

            checkBox.setChecked(style.Id == myBookmark.getStyleId());

            colorView.setVisibility(View.VISIBLE);
            BookmarksUtil.setupColorView(colorView, style);

            titleView.setText(BookmarkUtil.getStyleName(style));

            button.setVisibility(View.VISIBLE);
            button.setText(myResource.getResource("editStyle").getValue());
            button.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(
                            new Intent(EditBookmarkActivity.this, EditStyleActivity.class)
                                    .putExtra(EditStyleActivity.STYLE_ID_KEY, style.Id)
                    );
                }
            });

            return view;
        }

        public final synchronized void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final HighlightingStyle style = getItem(position);
            int mSelectColor = style.getBackgroundColor().intValue();
            Intent data = new Intent();
            data.putExtra("selectColor", mSelectColor);
            setResult(7, data);
            myCollection.bindToService(EditBookmarkActivity.this, new Runnable() {
                public void run() {
                    myBookmark.setStyleId(style.Id);
                    myCollection.setDefaultHighlightingStyleId(style.Id);
                    myCollection.saveBookmark(myBookmark);
                }
            });
            notifyDataSetChanged();
        }
    }
}
