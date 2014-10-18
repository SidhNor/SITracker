/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.components.FileFolderView;
import com.andrada.sitracker.ui.components.FileFolderView_;
import com.andrada.sitracker.util.LogUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import static com.andrada.sitracker.util.LogUtils.LOGD;

/**
 * Activities that contain this fragment must implement the
 * {@link DirectoryChooserFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DirectoryChooserFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DirectoryChooserFragment extends DialogFragment {

    public static final String KEY_CURRENT_DIRECTORY = "CURRENT_DIRECTORY";

    private static final String ARG_IS_DIRECTORY_CHOOSER = "DIRECTORY_CHOOSER_SETTING";
    private static final String ARG_NEW_DIRECTORY_NAME = "NEW_DIRECTORY_NAME";
    private static final String ARG_INITIAL_DIRECTORY = "INITIAL_DIRECTORY";
    private static final String TAG = LogUtils.makeLogTag(DirectoryChooserFragment.class);
    private String mNewDirectoryName;
    private String mInitialDirectory;
    private Boolean mIsDirectoryChooser = true;

    @NotNull
    private OnFragmentInteractionListener mListener;

    private Button mBtnConfirm;
    private ImageButton mBtnNavUp;
    private ImageButton mBtnCreateFolder;
    private TextView mTxtvSelectedFolderLabel;
    private TextView mTxtvSelectedFolder;

    private FolderArrayAdapter mListDirectoriesAdapter;
    private ArrayList<FileDescriptor> mFilenames;
    /**
     * The directory that is currently being shown.
     */
    @Nullable
    private File mSelectedDir;
    @Nullable
    private File mSelectedFile;
    private File[] mFilesInDir;
    @Nullable
    private FileObserver mFileObserver;


    public DirectoryChooserFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param newDirectoryName Name of the directory to create.
     * @param initialDirectory Optional argument to define the path of the directory
     *                         that will be shown first.
     *                         If it is not sent or if path denotes a non readable/writable directory
     *                         or it is not a directory, it defaults to
     *                         {@link android.os.Environment#getExternalStorageDirectory()}
     * @return A new instance of fragment DirectoryChooserFragment.
     */
    @NotNull
    public static DirectoryChooserFragment newInstance(
            final String newDirectoryName,
            final String initialDirectory,
            final Boolean isDirectoryChooser) {
        DirectoryChooserFragment fragment = new DirectoryChooserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NEW_DIRECTORY_NAME, newDirectoryName);
        args.putString(ARG_INITIAL_DIRECTORY, initialDirectory);
        args.putBoolean(ARG_IS_DIRECTORY_CHOOSER, isDirectoryChooser);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedDir != null) {
            outState.putString(KEY_CURRENT_DIRECTORY, mSelectedDir.getAbsolutePath());
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new IllegalArgumentException(
                    "You must create DirectoryChooserFragment via newInstance().");
        } else {
            mNewDirectoryName = getArguments().getString(ARG_NEW_DIRECTORY_NAME);
            mInitialDirectory = getArguments().getString(ARG_INITIAL_DIRECTORY);
            mIsDirectoryChooser = getArguments().getBoolean(ARG_IS_DIRECTORY_CHOOSER, true);
        }

        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }

        if (this.getShowsDialog()) {
            setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        } else {
            setHasOptionsMenu(true);
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        assert getActivity() != null;
        final View view = inflater.inflate(R.layout.directory_chooser, container, false);

        mBtnConfirm = (Button) view.findViewById(R.id.btnConfirm);
        Button mBtnCancel = (Button) view.findViewById(R.id.btnCancel);
        mBtnNavUp = (ImageButton) view.findViewById(R.id.btnNavUp);
        mBtnCreateFolder = (ImageButton) view.findViewById(R.id.btnCreateFolder);
        mTxtvSelectedFolderLabel = (TextView) view.findViewById(R.id.txtvSelectedFolderLabel);
        mTxtvSelectedFolder = (TextView) view.findViewById(R.id.txtvSelectedFolder);
        ListView mListDirectories = (ListView) view.findViewById(R.id.directoryList);

        if (!mIsDirectoryChooser) {
            mBtnConfirm.setVisibility(View.GONE);
            View horDivider = view.findViewById(R.id.horizontalDivider);
            if (horDivider != null) {
                horDivider.setVisibility(View.INVISIBLE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) horDivider.getLayoutParams();
                params.addRule(RelativeLayout.CENTER_HORIZONTAL, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                horDivider.setLayoutParams(params);
            }
        } else {
            mBtnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isValidFile(mSelectedDir) && mIsDirectoryChooser) {
                        returnSelectedFolder();
                    }
                }
            });
        }

        mBtnCancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mListener.onCancelChooser();
            }
        });

        mListDirectories.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapter, View view,
                                    int position, long id) {
                debug("Selected index: %d", position);
                if (mFilesInDir != null && position >= 0
                        && position < mFilesInDir.length) {
                    changeDirectory(mFilesInDir[position]);
                }
            }
        });

        mBtnNavUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                File parent;
                if (mSelectedDir != null
                        && (parent = mSelectedDir.getParentFile()) != null) {
                    changeDirectory(parent);
                }
            }
        });

        mBtnCreateFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewFolderDialog();
            }
        });

        if (!getShowsDialog()) {
            mBtnCreateFolder.setVisibility(View.GONE);
        }

        adjustResourceLightness();

        mFilenames = new ArrayList<FileDescriptor>();
        mListDirectoriesAdapter = new FolderArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, mFilenames);
        mListDirectories.setAdapter(mListDirectoriesAdapter);

        final File initialDir;
        if (mInitialDirectory != null && isValidFile(new File(mInitialDirectory))) {
            initialDir = new File(mInitialDirectory);
        } else {
            initialDir = Environment.getExternalStorageDirectory();
        }

        changeDirectory(initialDir);

        return view;
    }

    private void adjustResourceLightness() {
        // change up button to light version if using dark theme
        int color = 0xFFFFFF;
        final Resources.Theme theme = getActivity().getTheme();

        if (theme != null) {
            TypedArray backgroundAttributes = theme.obtainStyledAttributes(
                    new int[]{android.R.attr.colorBackground});

            if (backgroundAttributes != null) {
                color = backgroundAttributes.getColor(0, 0xFFFFFF);
                backgroundAttributes.recycle();
            }
        }

        // convert to greyscale and check if < 128
        if (color != 0xFFFFFF && 0.21 * Color.red(color) +
                0.72 * Color.green(color) +
                0.07 * Color.blue(color) < 128) {
            mBtnNavUp.setImageResource(R.drawable.navigation_up_light);
            mBtnCreateFolder.setImageResource(R.drawable.ic_action_create);
        }
    }

    @Override
    public void onAttach(@NotNull Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mFileObserver != null) {
            mFileObserver.stopWatching();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mFileObserver != null) {
            mFileObserver.startWatching();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, @NotNull MenuInflater inflater) {
        inflater.inflate(R.menu.directory_chooser, menu);

        final MenuItem menuItem = menu.findItem(R.id.new_folder_item);

        if (menuItem == null) {
            return;
        }

        menuItem.setVisible(mInitialDirectory != null && isValidFile(new File(mInitialDirectory)) && mNewDirectoryName != null && mIsDirectoryChooser);
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        final int itemId = item.getItemId();

        if (itemId == R.id.new_folder_item) {
            openNewFolderDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Shows a confirmation dialog that asks the user if he wants to create a
     * new folder.
     */
    private void openNewFolderDialog() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.fp_create_folder_label)
                .setMessage(
                        String.format(getString(R.string.fp_create_folder_msg),
                                mNewDirectoryName))
                .setNegativeButton(R.string.fp_cancel_label,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(@NotNull DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                            }
                        })
                .setPositiveButton(R.string.fp_confirm_label,
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(@NotNull DialogInterface dialog,
                                                int which) {
                                dialog.dismiss();
                                int msg = createFolder();
                                Crouton.makeText(getActivity(), msg, Style.INFO).show();
                            }
                        }).create().show();
    }

    /**
     * Change the directory that is currently being displayed.
     *
     * @param dir The file the activity should switch to. This File must be
     *            non-null and a directory, otherwise the displayed directory
     *            will not be changed
     */
    private void changeDirectory(@Nullable File dir) {
        if (dir == null) {
            debug("Could not change folder: dir was null");
        } else if (!dir.isDirectory() && !mIsDirectoryChooser) {
            debug("Could not change folder: dir is no directory, selecting file");
            //Selecting file
            mSelectedFile = dir;
            mTxtvSelectedFolder.setText(dir.getAbsolutePath());
            if (isAdded()) {
                mTxtvSelectedFolderLabel.setText(getResources().getString(R.string.fp_selected_file_label));
            }
            if (isValidFile(mSelectedFile)) {
                returnSelectedFile();
            }
        } else {
            File[] contents = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(@NotNull File file) {
                    return !file.isHidden();
                }
            });
            if (contents != null) {
                int numDirectories = 0;
                if (mIsDirectoryChooser) {
                    for (File f : contents) {
                        if (f.isDirectory()) {
                            numDirectories++;
                        }
                    }
                } else {
                    numDirectories = contents.length;
                }

                mFilesInDir = new File[numDirectories];
                mFilenames.clear();
                for (int i = 0, counter = 0; i < numDirectories; counter++) {
                    if ((mIsDirectoryChooser && contents[counter].isDirectory()) || !mIsDirectoryChooser) {
                        mFilesInDir[i] = contents[counter];
                        mFilenames.add(new FileDescriptor(contents[counter].getName(), contents[counter].isDirectory()));
                        i++;
                    }
                }
                Arrays.sort(mFilesInDir, new Comparator<File>() {
                    @Override
                    public int compare(@NotNull File aThis, @Nullable File aThat) {
                        final int BEFORE = -1;
                        final int EQUAL = 0;
                        final int AFTER = 1;
                        if (aThis == aThat) {
                            return EQUAL;
                        }
                        if (aThat == null) {
                            return BEFORE;
                        }
                        //Compare by type first
                        if (aThis.isDirectory() && !aThat.isDirectory()) {
                            return BEFORE;
                        }
                        if (!aThis.isDirectory() && aThat.isDirectory()) {
                            return AFTER;
                        }
                        //Compare by filename
                        int comparison = aThis.getName().compareTo(aThat.getName());
                        if (comparison != EQUAL) {
                            return comparison;
                        }

                        return EQUAL;
                    }
                });
                Collections.sort(mFilenames);
                mSelectedDir = dir;
                mSelectedFile = null;
                if (isAdded()) {
                    mTxtvSelectedFolderLabel.setText(getResources().getString(R.string.fp_selected_folder_label));
                }
                mTxtvSelectedFolder.setText(dir.getAbsolutePath());
                mListDirectoriesAdapter.notifyDataSetChanged();
                mFileObserver = createFileObserver(dir.getAbsolutePath());
                mFileObserver.startWatching();
                debug("Changed directory to %s", dir.getAbsolutePath());
            } else {
                debug("Could not change folder: contents of dir were null");
            }
        }
        refreshButtonState();
    }

    private void debug(@NotNull String message, Object... args) {
        LOGD(TAG, String.format(message, args));
    }

    /**
     * Changes the state of the buttons depending on the currently selected file
     * or folder.
     */
    private void refreshButtonState() {
        final Activity activity = getActivity();
        if (activity != null && mSelectedDir != null) {
            boolean valid = mIsDirectoryChooser ? isValidFile(mSelectedDir) : isValidFile(mSelectedFile);
            mBtnConfirm.setEnabled(valid);
            getActivity().invalidateOptionsMenu();
        }
    }

    /**
     * Refresh the contents of the directory that is currently shown.
     */
    private void refreshDirectory() {
        if (mSelectedDir != null) {
            changeDirectory(mSelectedDir);
        }
    }

    /**
     * Sets up a FileObserver to watch the current directory.
     */
    @NotNull
    private FileObserver createFileObserver(String path) {
        return new FileObserver(path, FileObserver.CREATE | FileObserver.DELETE
                | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {

            @Override
            public void onEvent(int event, String path) {
                debug("FileObserver received event %d", event);
                final Activity activity = getActivity();

                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshDirectory();
                        }
                    });
                }
            }
        };
    }

    /**
     * Returns the selected folder as a result to the activity the fragment's attached to. The
     * selected folder can also be null.
     */
    private void returnSelectedFolder() {
        if (mSelectedDir != null && mIsDirectoryChooser) {
            debug("Returning %s as result", mSelectedDir.getAbsolutePath());
            mListener.onSelectDirectory(mSelectedDir.getAbsolutePath());
        } else {
            mListener.onCancelChooser();
        }
    }

    private void returnSelectedFile() {
        if (mSelectedFile != null && !mIsDirectoryChooser) {
            mListener.onSelectDirectory(mSelectedFile.getAbsolutePath());
        } else {
            mListener.onCancelChooser();
        }
    }

    /**
     * Creates a new folder in the current directory with the name
     * CREATE_DIRECTORY_NAME.
     */
    private int createFolder() {
        if (mNewDirectoryName != null && mSelectedDir != null
                && mSelectedDir.canWrite()) {
            File newDir = new File(mSelectedDir, mNewDirectoryName);
            if (!newDir.exists()) {
                boolean result = newDir.mkdir();
                if (result) {
                    return R.string.fp_create_folder_success;
                } else {
                    return R.string.fp_create_folder_error;
                }
            } else {
                return R.string.fp_create_folder_error_already_exists;
            }
        } else if (mSelectedDir != null && !mSelectedDir.canWrite()) {
            return R.string.fp_create_folder_error_no_write_access;
        } else {
            return R.string.fp_create_folder_error;
        }
    }

    /**
     * Returns true if the selected file or directory would be valid selection.
     */
    private boolean isValidFile(@Nullable File file) {
        if (mIsDirectoryChooser) {
            return (file != null && file.isDirectory() && file.canRead() && file
                    .canWrite());
        } else {
            return (file != null && !file.isDirectory() && file.canRead());
        }

    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * Triggered when the user successfully selected their destination directory.
         */
        public void onSelectDirectory(String path);

        /**
         * Advices the activity to remove the current fragment.
         */
        public void onCancelChooser();
    }

    private class FileDescriptor implements Comparable<FileDescriptor> {
        private String fileName;
        private Boolean isDirectory;

        public FileDescriptor(String filename, Boolean isDirectory) {
            fileName = filename;
            this.isDirectory = isDirectory;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String mFileName) {
            this.fileName = mFileName;
        }

        public Boolean getIsDirectory() {
            return isDirectory;
        }

        public void setIsDirectory(Boolean mIsDirectory) {
            this.isDirectory = mIsDirectory;
        }

        @Override
        public int compareTo(@Nullable FileDescriptor that) {
            final int BEFORE = -1;
            final int EQUAL = 0;
            final int AFTER = 1;
            if (this == that) {
                return EQUAL;
            }
            if (that == null) {
                return BEFORE;
            }
            //Compare by type first
            if (this.isDirectory && !that.isDirectory) {
                return BEFORE;
            }
            if (!this.isDirectory && that.isDirectory) {
                return AFTER;
            }
            //Compare by filename
            int comparison = this.fileName.compareTo(that.fileName);
            if (comparison != EQUAL) {
                return comparison;
            }

            return EQUAL;
        }
    }

    private class FolderArrayAdapter extends ArrayAdapter<FileDescriptor> {

        public FolderArrayAdapter(@NotNull Context context, int resource, List<FileDescriptor> objects) {
            super(context, resource, objects);
        }

        @Nullable
        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            FileFolderView folderItemView;
            if (convertView == null) {
                folderItemView = FileFolderView_.build(getContext());
            } else {
                folderItemView = (FileFolderView) convertView;
            }
            if (position < getCount()) {
                FileDescriptor descriptor = getItem(position);
                folderItemView.bind(descriptor.getFileName(), descriptor.getIsDirectory());
            }
            return folderItemView;
        }

    }

}
