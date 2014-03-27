/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.app;

import android.support.v17.leanback.R;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemSelectedListener;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.SearchOrbView;
import android.util.Log;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.drawable.Drawable;

import static android.support.v7.widget.RecyclerView.NO_POSITION;

/**
 * Wrapper fragment for leanback browse screens. Composed of a
 * RowsFragment and a HeadersFragment.
 *
 */
public class BrowseFragment extends Fragment {
    private static final String TAG = "BrowseFragment";
    private static boolean DEBUG = false;

    /** The fastlane navigation panel is enabled and shown by default. */
    public static final int HEADERS_ENABLED = 1;

    /** The fastlane navigation panel is enabled and hidden by default. */
    public static final int HEADERS_HIDDEN = 2;

    /** The fastlane navigation panel is disabled and will never be shown. */
    public static final int HEADERS_DISABLED = 3;

    private final RowsFragment mRowsFragment = new RowsFragment();
    private final HeadersFragment mHeadersFragment = new HeadersFragment();

    private Params mParams;
    private BrowseFrameLayout mBrowseFrame;
    private ImageView mBadgeView;
    private TextView mTitleView;
    private ViewGroup mBrowseTitle;
    private SearchOrbView mSearchOrbView;
    private boolean mShowingTitle = true;
    private boolean mShowingHeaders = true;
    private boolean mCanShowHeaders = true;
    private int mContainerListMarginLeft;
    private int mContainerListWidth;
    private int mContainerListAlignTop;
    private TransitionHelper mTransitionHelper;
    private OnItemSelectedListener mExternalOnItemSelectedListener;
    private OnClickListener mExternalOnSearchClickedListener;
    private int mSelectedPosition = -1;

    private static final String ARG_TITLE = BrowseFragment.class.getCanonicalName() + ".title";
    private static final String ARG_BADGE_URI = BrowseFragment.class.getCanonicalName() + ".badge";
    private static final String ARG_HEADERS_STATE =
        BrowseFragment.class.getCanonicalName() + ".headersState";

    /**
     * @param args Bundle to use for the arguments, if null a new Bundle will be created.
     */
    public static Bundle createArgs(Bundle args, String title, String badgeUri) {
        return createArgs(args, title, badgeUri, HEADERS_ENABLED);
    }

    public static Bundle createArgs(Bundle args, String title, String badgeUri, int headersState) {
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_TITLE, title);
        args.putString(ARG_BADGE_URI, badgeUri);
        args.putInt(ARG_HEADERS_STATE, headersState);
        return args;
    }

    public static class Params {
        private String mTitle;
        private Drawable mBadgeDrawable;
        private int mHeadersState;

        /**
         * Sets the badge image.
         */
        public void setBadgeImage(Drawable drawable) {
            mBadgeDrawable = drawable;
        }

        /**
         * Returns the badge image.
         */
        public Drawable getBadgeImage() {
            return mBadgeDrawable;
        }

        /**
         * Sets a title for the browse fragment.
         */
        public void setTitle(String title) {
            mTitle = title;
        }

        /**
         * Returns the title for the browse fragment.
         */
        public String getTitle() {
            return mTitle;
        }

        /**
         * Sets the state for the headers column in the browse fragment.
         */
        public void setHeadersState(int headersState) {
            if (headersState < HEADERS_ENABLED || headersState > HEADERS_DISABLED) {
                Log.e(TAG, "Invalid headers state: " + headersState
                        + ", default to enabled and shown.");
                mHeadersState = HEADERS_ENABLED;
            } else {
                mHeadersState = headersState;
            }
        }

        /**
         * Returns the state for the headers column in the browse fragment.
         */
        public int getHeadersState() {
            return mHeadersState;
        }
    }

    /**
     * Set browse parameters.
     */
    public void setBrowseParams(Params params) {
        mParams = params;
        setBadgeDrawable(mParams.mBadgeDrawable);
        setTitle(mParams.mTitle);
        setHeadersState(mParams.mHeadersState);
    }

    /**
     * Set background parameters.
     * @deprecated Use BackgroundManager instead
     */
    @Deprecated
    public void setBackgroundParams(BackgroundParams params) {
    }

    /**
     * Returns browse parameters.
     */
    public Params getBrowseParams() {
        return mParams;
    }

    /**
     * Returns the background parameters.
     * @deprecated Use BackgroundManager instead
     */
    @Deprecated
    public BackgroundParams getBackgroundParams() {
        return new BackgroundParams();
    }

    /**
     * Sets the list of rows for the fragment.
     */
    public void setAdapter(ObjectAdapter adapter) {
        mRowsFragment.setAdapter(adapter);
        mHeadersFragment.setAdapter(adapter);
    }

    /**
     * Returns the list of rows.
     */
    public ObjectAdapter getAdapter() {
        return mRowsFragment.getAdapter();
    }

    /**
     * Sets an item selection listener.
     */
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        mExternalOnItemSelectedListener = listener;
    }

    /**
     * Sets an item clicked listener on the fragment.
     * OnItemClickedListener will override {@link View.OnClickListener} that
     * item presenter sets during {@link Presenter#onCreateViewHolder(ViewGroup)}.
     * So in general,  developer should choose one of the listeners but not both.
     */
    public void setOnItemClickedListener(OnItemClickedListener listener) {
        mRowsFragment.setOnItemClickedListener(listener);
    }

    /**
     * Returns the item Clicked listener.
     */
    public OnItemClickedListener getOnItemClickedListener() {
        return mRowsFragment.getOnItemClickedListener();
    }

    /**
     * Sets a click listener for the search "affordance".
     *
     * The presence of a listener will change the visibility of the search "affordance" in the
     * title area. When set to non null the title area will contain a call to search action.
     *
     * The listener onClick method will be invoked when the user click on the search action.
     *
     * @param listener The listener.
     */
    public void setOnSearchClickedListener(View.OnClickListener listener) {
        mExternalOnSearchClickedListener = listener;
        if (mSearchOrbView != null) {
            mSearchOrbView.setOnOrbClickedListener(listener);
        }
    }

    private final BrowseFrameLayout.OnFocusSearchListener mOnFocusSearchListener =
            new BrowseFrameLayout.OnFocusSearchListener() {
        @Override
        public View onFocusSearch(View focused, int direction) {
            // If fastlane is disabled, just return null.
            if (!mCanShowHeaders) return null;

            if (DEBUG) Log.v(TAG, "onFocusSearch focused " + focused + " + direction " + direction);
            if (!mShowingHeaders && direction == View.FOCUS_LEFT) {
                mTransitionHelper.runTransition(TransitionHelper.SCENE_WITH_HEADERS);
                mShowingHeaders = true;
                return mHeadersFragment.getVerticalGridView();

            } else if (mShowingHeaders && direction == View.FOCUS_RIGHT) {
                mTransitionHelper.runTransition(TransitionHelper.SCENE_WITHOUT_HEADERS);
                mShowingHeaders = false;
                return mRowsFragment.getVerticalGridView();

            } else if (focused == mSearchOrbView && direction == View.FOCUS_DOWN) {
                return mShowingHeaders ? mHeadersFragment.getVerticalGridView() :
                    mRowsFragment.getVerticalGridView();

            } else if (focused != mSearchOrbView && mSearchOrbView.getVisibility() == View.VISIBLE
                    && direction == View.FOCUS_UP) {
                return mSearchOrbView;

            } else {
                return null;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHeadersFragment.setOnHeaderClickListener(mHeaderClickListener);

        mContainerListMarginLeft = (int) getResources().getDimension(
                R.dimen.lb_browse_rows_margin_left);
        mContainerListWidth =  getResources().getDimensionPixelSize(R.dimen.lb_browse_rows_width);
        mContainerListAlignTop =
            getResources().getDimensionPixelSize(R.dimen.lb_browse_rows_align_top);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.lb_browse_fragment, container, false);

        mBrowseFrame = (BrowseFrameLayout) root.findViewById(R.id.browse_frame);
        mBrowseFrame.setOnFocusSearchListener(mOnFocusSearchListener);

        mBrowseTitle = (ViewGroup) root.findViewById(R.id.browse_title_group);
        mBadgeView = (ImageView) mBrowseTitle.findViewById(R.id.browse_badge);
        mTitleView = (TextView) mBrowseTitle.findViewById(R.id.browse_title);
        mSearchOrbView = (SearchOrbView) mBrowseTitle.findViewById(R.id.browse_orb);
        if (mExternalOnSearchClickedListener != null) {
            mSearchOrbView.setOnOrbClickedListener(mExternalOnSearchClickedListener);
        }

        readArguments(getArguments());
        if (mParams != null) {
            setBadgeDrawable(mParams.mBadgeDrawable);
            setTitle(mParams.mTitle);
            setHeadersState(mParams.mHeadersState);
        }

        mTransitionHelper = new TransitionHelper(getActivity());
        mTransitionHelper.addSceneRunnable(TransitionHelper.SCENE_WITH_TITLE, mBrowseFrame,
                new Runnable() {
            @Override
            public void run() {
                showTitle(true);
            }
        });
        mTransitionHelper.addSceneRunnable(TransitionHelper.SCENE_WITHOUT_TITLE, mBrowseFrame,
                new Runnable() {
            @Override
            public void run() {
                showTitle(false);
            }
        });
        mTransitionHelper.addSceneRunnable(TransitionHelper.SCENE_WITH_HEADERS, mBrowseFrame,
                new Runnable() {
            @Override
            public void run() {
                showHeaders(true);
            }
        });
        mTransitionHelper.addSceneRunnable(TransitionHelper.SCENE_WITHOUT_HEADERS, mBrowseFrame,
                new Runnable() {
            @Override
            public void run() {
                showHeaders(false);
            }
        });

        return root;
    }

    private void showTitle(boolean show) {
        mBrowseTitle.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showHeaders(boolean show) {
        if (DEBUG) Log.v(TAG, "showHeaders " + show);
        View headerList = mHeadersFragment.getView();
        View containerList = mRowsFragment.getView();
        MarginLayoutParams lp;

        headerList.setVisibility(show ? View.VISIBLE : View.GONE);
        lp = (MarginLayoutParams) containerList.getLayoutParams();
        lp.leftMargin = show ? mContainerListMarginLeft : 0;
        containerList.setLayoutParams(lp);

        mRowsFragment.setExpand(!show);
    }

    private HeaderPresenter.OnHeaderClickListener mHeaderClickListener =
        new HeaderPresenter.OnHeaderClickListener() {
            @Override
            public void onHeaderClicked() {
                if (!mCanShowHeaders || !mShowingHeaders) return;

                mTransitionHelper.runTransition(TransitionHelper.SCENE_WITHOUT_HEADERS);
                mShowingHeaders = false;
                mRowsFragment.getVerticalGridView().requestFocus();
            }
        };

    private OnItemSelectedListener mRowSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(Object item, Row row) {
            int position = mRowsFragment.getVerticalGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "row selected position " + position);
            onRowSelected(position);
            if (mExternalOnItemSelectedListener != null) {
                mExternalOnItemSelectedListener.onItemSelected(item, row);
            }
        }
    };

    private OnItemSelectedListener mHeaderSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(Object item, Row row) {
            int position = mHeadersFragment.getVerticalGridView().getSelectedPosition();
            if (DEBUG) Log.v(TAG, "header selected position " + position);
            onRowSelected(position);
        }
    };

    private void onRowSelected(int position) {
        if (position != mSelectedPosition) {
            mSetSelectionRunnable.mPosition = position;
            mBrowseFrame.getHandler().post(mSetSelectionRunnable);

            if (position == 0) {
                if (!mShowingTitle) {
                    mTransitionHelper.runTransition(TransitionHelper.SCENE_WITH_TITLE);
                    mShowingTitle = true;
                }
            } else if (mShowingTitle) {
                mTransitionHelper.runTransition(TransitionHelper.SCENE_WITHOUT_TITLE);
                mShowingTitle = false;
            }
        }
    }

    private class SetSelectionRunnable implements Runnable {
        int mPosition;
        @Override
        public void run() {
            setSelection(mPosition);
        }
    }

    private final SetSelectionRunnable mSetSelectionRunnable = new SetSelectionRunnable();

    private void setSelection(int position) {
        if (position != NO_POSITION) {
            mRowsFragment.setSelectedPosition(position);
            mHeadersFragment.setSelectedPosition(position);
        }
        mSelectedPosition = position;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (getChildFragmentManager().findFragmentById(R.id.browse_container_dock) == null) {
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.browse_headers_dock, mHeadersFragment)
                    .replace(R.id.browse_container_dock, mRowsFragment).commit();
            mRowsFragment.setOnItemSelectedListener(mRowSelectedListener);
            mHeadersFragment.setOnItemSelectedListener(mHeaderSelectedListener);
        }
    }

    private void setVerticalVerticalGridViewLayout(VerticalGridView listview) {
        // align the top edge of item to a fixed position
        listview.setItemAlignmentOffset(0);
        listview.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
        listview.setWindowAlignmentOffset(mContainerListAlignTop);
        listview.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        listview.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
    }

    /**
     * Setup dimensions that are only meaningful when the child Fragments are inside
     * BrowseFragment.
     */
    private void setupChildFragmentsLayout() {
        VerticalGridView headerList = mHeadersFragment.getVerticalGridView();
        VerticalGridView containerList = mRowsFragment.getVerticalGridView();

        // Both fragments list view has the same alignment
        setVerticalVerticalGridViewLayout(headerList);
        setVerticalVerticalGridViewLayout(containerList);

        mRowsFragment.getVerticalGridView().getLayoutParams().width = mContainerListWidth;
        mRowsFragment.getVerticalGridView().requestLayout();
    }

    @Override
    public void onStart() {
        super.onStart();
        setupChildFragmentsLayout();
        if (mCanShowHeaders && mShowingHeaders && mHeadersFragment.getView() != null) {
            mHeadersFragment.getView().requestFocus();
        } else if ((!mCanShowHeaders || !mShowingHeaders)
                && mRowsFragment.getView() != null) {
            mRowsFragment.getView().requestFocus();
        }
        showHeaders(mCanShowHeaders && mShowingHeaders);
    }

    private void readArguments(Bundle args) {
        if (args == null) {
            return;
        }
        if (args.containsKey(ARG_TITLE)) {
            setTitle(args.getString(ARG_TITLE));
        }

        if (args.containsKey(ARG_BADGE_URI)) {
            setBadgeUri(args.getString(ARG_BADGE_URI));
        }

        if (args.containsKey(ARG_HEADERS_STATE)) {
            setHeadersState(args.getInt(ARG_HEADERS_STATE));
        }
    }

    private void setBadgeUri(String badgeUri) {
        // TODO - need a drawable downloader
    }

    private void setBadgeDrawable(Drawable drawable) {
        if (mBadgeView == null) {
            return;
        }
        mBadgeView.setImageDrawable(drawable);
        if (drawable != null) {
            mBadgeView.setVisibility(View.VISIBLE);
        } else {
            mBadgeView.setVisibility(View.GONE);
        }
    }

    private void setTitle(String title) {
        if (mTitleView != null) {
            mTitleView.setText(title);
        }
    }

    private void setHeadersState(int headersState) {
        if (DEBUG) Log.v(TAG, "setHeadersState " + headersState);
        switch (headersState) {
            case HEADERS_ENABLED:
                mCanShowHeaders = true;
                mShowingHeaders = true;
                break;
            case HEADERS_HIDDEN:
                mCanShowHeaders = true;
                mShowingHeaders = false;
                break;
            case HEADERS_DISABLED:
                mCanShowHeaders = false;
                mShowingHeaders = false;
                break;
            default:
                Log.w(TAG, "Unknown headers state: " + headersState);
                break;
        }
    }
}
