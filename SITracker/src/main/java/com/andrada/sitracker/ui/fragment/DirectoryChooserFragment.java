/*
 * Copyright 2016 Gleb Godonoga.
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
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.components.FileFolderView;
import com.andrada.sitracker.ui.components.FileFolderView_;
import com.andrada.sitracker.util.LogUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    private static final String ARG_INITIAL_DIRECTORY = "INITIAL_DIRECTORY";

    private static final String TAG = LogUtils.makeLogTag(DirectoryChooserFragment.class);

    private String mInitialDirectory;

    private Boolean mIsDirectoryChooser = true;

    private WeakReference<OnFragmentInteractionListener> mListener;

    private FolderArrayAdapter mListDirectoriesAdapter;

    private ArrayList<FileDescriptor> mFilenames;

    /**
     * The directory that is currently being shown.
     */
    @Nullable
    private File mSelectedDir;

    @Nullable
    private File mSelectedFile;

    private ArrayList<File> mFilesInDir;

    @Nullable
    private FileObserver mFileObserver;

    MaterialDialog mCurrentDialog;

    public DirectoryChooserFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param initialDirectory Optional argument to define the path of the directory
     *                         that will be shown first.
     *                         If it is not sent or if path denotes a non readable/writable
     *                         directory
     *                         or it is not a directory, it defaults to
     *                         {@link android.os.Environment#getExternalStorageDirectory()}
     * @return A new instance of fragment DirectoryChooserFragment.
     */
    @NotNull
    public static DirectoryChooserFragment newInstance(
            final String initialDirectory,
            final Boolean isDirectoryChooser,
            final OnFragmentInteractionListener listener) {
        DirectoryChooserFragment fragment = new DirectoryChooserFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_DIRECTORY, initialDirectory);
        args.putBoolean(ARG_IS_DIRECTORY_CHOOSER, isDirectoryChooser);
        fragment.setArguments(args);
        fragment.mListener = new WeakReference<>(listener);
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
            mInitialDirectory = getArguments().getString(ARG_INITIAL_DIRECTORY);
            mIsDirectoryChooser = getArguments().getBoolean(ARG_IS_DIRECTORY_CHOOSER, true);
        }

        if (savedInstanceState != null) {
            mInitialDirectory = savedInstanceState.getString(KEY_CURRENT_DIRECTORY);
        }

        if (this.getShowsDialog()) {
            setStyle(DialogFragment.STYLE_NO_TITLE, 0);
        }
    }

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        assert getActivity() != null;

        MaterialDialog.Builder dialogBuilder = new MaterialDialog.Builder(getActivity())
                .title("")
                .negativeText(R.string.fp_cancel_label)
                .negativeColor(Color.GRAY)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (mListener.get() != null)
                            mListener.get().onCancelChooser();
                    }
                });

        if (mIsDirectoryChooser) {
            dialogBuilder
                    .positiveText(R.string.fp_confirm_label)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (isValidFile(mSelectedDir) && mIsDirectoryChooser) {
                                returnSelectedFolder();
                            }
                        }
                    });
        }

        if (getShowsDialog()) {
            dialogBuilder
                    .neutralText(R.string.fp_new_folder)
                    .neutralColor(Color.GRAY)
                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            openNewFolderDialog();
                        }
                    });

        }

        mFilenames = new ArrayList<>();
        mListDirectoriesAdapter = new FolderArrayAdapter(getActivity(),
                android.R.layout.simple_list_item_1, mFilenames);
        dialogBuilder.customView(R.layout.directory_chooser, false);

        mCurrentDialog = dialogBuilder.build();

        ListView lv = (ListView) mCurrentDialog.findViewById(R.id.directoryList);
        lv.setAdapter(mListDirectoriesAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int which, long id) {
                if (mFilesInDir != null && which >= 0
                        && which < mFilesInDir.size()) {
                    changeDirectory(mFilesInDir.get(which));
                }
            }
        });

        final File initialDir;
        if (mInitialDirectory != null && isValidFile(new File(mInitialDirectory))) {
            initialDir = new File(mInitialDirectory);
        } else {
            initialDir = Environment.getExternalStorageDirectory();
        }
        changeDirectory(initialDir);

        ViewGroup parent = ((ViewGroup) mCurrentDialog.getView().getParent());
        if (parent != null) {
            parent.removeView(mCurrentDialog.getView());
        }

        return mCurrentDialog.getView();
    }

    public void setListener(@NotNull OnFragmentInteractionListener mListener) {
        this.mListener = new WeakReference<>(mListener);
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

    /**
     * Shows a confirmation dialog that asks the user if he wants to create a
     * new folder.
     */
    private void openNewFolderDialog() {
        new MaterialDialog.Builder(getActivity())
                .title(R.string.fp_create_folder_label)
                .inputRangeRes(2, 20, R.color.md_edittext_error)
                .input(R.string.fp_create_folder_msg, R.string.fp_default_folder_name, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                        int msg = createFolder(input);
                        if (msg != R.string.fp_create_folder_success) {
                            new MaterialDialog.Builder(getActivity()).content(msg)
                                    .positiveText(android.R.string.ok)
                                    .show();
                        }
                    }
                })
                .positiveText(R.string.fp_confirm_label)
                .negativeText(R.string.fp_cancel_label)
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                }).show();
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

            mCurrentDialog.setTitle(dir.getAbsolutePath());
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

                mFilesInDir = new ArrayList<>();
                mFilenames.clear();
                for (int i = 0, counter = 0; i < numDirectories; counter++) {
                    if ((mIsDirectoryChooser && contents[counter].isDirectory())
                            || !mIsDirectoryChooser) {
                        mFilesInDir.add(contents[counter]);
                        mFilenames.add(new FileDescriptor(contents[counter].getName(),
                                contents[counter].isDirectory()));
                        i++;
                    }
                }
                Collections.sort(mFilesInDir, new Comparator<File>() {
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

                if (mSelectedDir.getParentFile() != null) {
                    //Insert back navigation
                    FileDescriptor bDescriptor = new FileDescriptor("..", true);
                    mFilenames.add(0, bDescriptor);
                    mFilesInDir.add(0, dir.getParentFile());

                }
                mCurrentDialog.setTitle(dir.getAbsolutePath());
                mListDirectoriesAdapter.notifyDataSetChanged();
                mFileObserver = createFileObserver(dir.getAbsolutePath());
                mFileObserver.startWatching();
                debug("Changed directory to %s", dir.getAbsolutePath());
            } else {
                debug("Could not change folder: contents of dir were null");
            }
        }
    }

    private void debug(@NotNull String message, Object... args) {
        LOGD(TAG, String.format(message, args));
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
        if (mListener.get() == null)
            return;
        if (mSelectedDir != null && mIsDirectoryChooser) {
            debug("Returning %s as result", mSelectedDir.getAbsolutePath());
            mListener.get().onSelectDirectory(mSelectedDir.getAbsolutePath());
        } else {
            mListener.get().onCancelChooser();
        }
    }

    private void returnSelectedFile() {
        if (mListener.get() == null)
            return;
        if (mSelectedFile != null && !mIsDirectoryChooser) {
            mListener.get().onSelectDirectory(mSelectedFile.getAbsolutePath());
        } else {
            mListener.get().onCancelChooser();
        }
    }

    /**
     * Creates a new folder in the current directory with the name
     * CREATE_DIRECTORY_NAME.
     *
     * @param input
     */
    private int createFolder(CharSequence input) {
        if (input != null && mSelectedDir != null
                && mSelectedDir.canWrite()) {
            File newDir = new File(mSelectedDir, input.toString());
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
        void onSelectDirectory(String path);

        /**
         * Advices the activity to remove the current fragment.
         */
        void onCancelChooser();
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

        public Boolean getIsDirectory() {
            return isDirectory;
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

        public FolderArrayAdapter(@NotNull Context context, int resource,
                                  List<FileDescriptor> objects) {
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
