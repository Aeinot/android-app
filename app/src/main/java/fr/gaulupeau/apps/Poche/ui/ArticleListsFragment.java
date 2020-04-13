package fr.gaulupeau.apps.Poche.ui;

import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.ViewPager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Collections;
import java.util.EnumSet;

import fr.gaulupeau.apps.InThePoche.R;
import fr.gaulupeau.apps.Poche.App;
import fr.gaulupeau.apps.Poche.events.ArticlesChangedEvent;
import fr.gaulupeau.apps.Poche.events.FeedsChangedEvent;

public class ArticleListsFragment extends Fragment implements Sortable, Searchable {

    private static final String TAG = ArticleListsFragment.class.getSimpleName();

    private static final String PARAM_TAG = "tag";

    private static final String STATE_SORT_ORDER = "sort_order";
    private static final String STATE_SEARCH_QUERY = "search_query";

    private static final EnumSet<ArticlesChangedEvent.ChangeType> CHANGE_SET = EnumSet.of(
            ArticlesChangedEvent.ChangeType.UNSPECIFIED,
            ArticlesChangedEvent.ChangeType.ADDED,
            ArticlesChangedEvent.ChangeType.DELETED,
            ArticlesChangedEvent.ChangeType.FAVORITED,
            ArticlesChangedEvent.ChangeType.UNFAVORITED,
            ArticlesChangedEvent.ChangeType.ARCHIVED,
            ArticlesChangedEvent.ChangeType.UNARCHIVED,
            ArticlesChangedEvent.ChangeType.CREATED_DATE_CHANGED,
            ArticlesChangedEvent.ChangeType.TITLE_CHANGED,
            ArticlesChangedEvent.ChangeType.DOMAIN_CHANGED,
            ArticlesChangedEvent.ChangeType.ESTIMATED_READING_TIME_CHANGED);

    private static final EnumSet<ArticlesChangedEvent.ChangeType> CHANGE_SET_FORCE_CONTENT_UPDATE
            = EnumSet.of(ArticlesChangedEvent.ChangeType.ESTIMATED_READING_TIME_CHANGED);

    private ArticleListsPagerAdapter adapter;
    private ViewPager viewPager;

    private Sortable.SortOrder sortOrder;
    private String searchQuery;

    public static ArticleListsFragment newInstance(String tag) {
        ArticleListsFragment fragment = new ArticleListsFragment();

        Bundle args = new Bundle();
        args.putString(PARAM_TAG, tag);
        fragment.setArguments(args);

        return fragment;
    }

    private String tag;

    public ArticleListsFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            tag = getArguments().getString(PARAM_TAG);
        }

        if(savedInstanceState != null) {
            Log.v(TAG, "onCreate() restoring state");

            if(sortOrder == null) {
                sortOrder = Sortable.SortOrder.values()[savedInstanceState.getInt(STATE_SORT_ORDER)];
            }
            if(searchQuery == null) {
                searchQuery = savedInstanceState.getString(STATE_SEARCH_QUERY);
            }
        }
        if(sortOrder == null) sortOrder = SortOrder.DESC;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_article_lists, container, false);

        adapter = new ArticleListsPagerAdapter(getChildFragmentManager(), tag);
        getChildFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentCreated(@NonNull FragmentManager fm, @NonNull Fragment f, @Nullable Bundle savedInstanceState) {
                if (f instanceof ArticleListFragment)
                    setParametersToFragment((ArticleListFragment) f);
            }
        }, false);

        viewPager = (ViewPager)view.findViewById(R.id.articles_list_pager);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Log.v(TAG, "onPageSelected() position: " + position);

                setParametersToFragment(getCurrentFragment());
            }
        });

        TabLayout tabLayout = (TabLayout)view.findViewById(R.id.articles_list_tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        // CUSTOM
        String[] navLabels = new String[3];
        navLabels[0] = App.getInstance().getString(R.string.feedName_unread);
        navLabels[1] = App.getInstance().getString(R.string.feedName_favorites);
        navLabels[2] = App.getInstance().getString(R.string.feedName_archived);

        int[] navIconsSelected = new int[3];
        navIconsSelected[0] = R.drawable.ic_home_solid_black_24dp;
        navIconsSelected[1] = R.drawable.ic_star_solid_black_24dp;
        navIconsSelected[2] = R.drawable.ic_archive_black_24dp;

        int[] navIcons = new int[3];
        navIcons[0] = R.drawable.ic_home_line_gray_24dp;
        navIcons[1] = R.drawable.ic_star_border_grey_24dp;
        navIcons[2] = R.drawable.ic_archive_gray_24dp;

        // loop through all tabLayout tabs
        for (int i = 0; i < tabLayout.getTabCount(); i++) {
            // inflate the Parent LinearLayout Container for the tab
            // from the layout nav_tab.xml file that we created 'R.layout.nav_tab
            LinearLayout tab = (LinearLayout) inflater.inflate(R.layout.custom_tab_layout, null);

            // get child TextView and ImageView from this layout for the icon and label
            TextView tab_label = (TextView) tab.findViewById(R.id.nav_label);
            ImageView tab_icon = (ImageView) tab.findViewById(R.id.nav_icon);

            // set the label text by getting the actual string value by its id
            // by getting the actual resource value `getResources().getString(string_id)`
            tab_label.setText(navLabels[i]);

            // set the home to be active at first
            if(i == 0){
                tab_label.setTextColor(Color.BLACK);
                tab_icon.setImageResource(navIconsSelected[i]);
            }else{
                tab_label.setTextColor(Color.GRAY);
                tab_icon.setImageResource(navIcons[i]);
            }


            // finally publish this custom view to tabLayout tab
            tabLayout.getTabAt(i).setCustomView(tab);
        }

        tabLayout.addOnTabSelectedListener(
                new TabLayout.ViewPagerOnTabSelectedListener(viewPager) {

                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        super.onTabSelected(tab);

                        // 1. get the custom View you've added
                        View tabView = tab.getCustomView();

                        // get inflated children Views the icon and the label by their id
                        TextView tab_label = (TextView) tabView.findViewById(R.id.nav_label);
                        ImageView tab_icon = (ImageView) tabView.findViewById(R.id.nav_icon);

                        // change the label color, by getting the color resource value
                        tab_label.setTextColor(Color.BLACK);
                        // change the image Resource
                        // i defined all icons in an array ordered in order of tabs appearances
                        // call tab.getPosition() to get active tab index.
                        tab_icon.setImageResource(navIconsSelected[tab.getPosition()]);
                    }

                    // do as the above the opposite way to reset tab when state is changed
                    // as it not the active one any more
                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {
                        super.onTabUnselected(tab);
                        View tabView = tab.getCustomView();
                        TextView tab_label = (TextView) tabView.findViewById(R.id.nav_label);
                        ImageView tab_icon = (ImageView) tabView.findViewById(R.id.nav_icon);

                        // back to the black color
                        tab_label.setTextColor(Color.GRAY);
                        // and the icon resouce to the old black image
                        // also via array that holds the icon resources in order
                        // and get the one of this tab's position
                        tab_icon.setImageResource(navIcons[tab.getPosition()]);
                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {
                        super.onTabReselected(tab);
                    }
                }
        );

        // ENDCUSTOM

        viewPager.setCurrentItem(0); // CUSTOM : valeur par défaut = 1

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.v(TAG, "onSaveInstanceState()");

        if(sortOrder != null) outState.putInt(STATE_SORT_ORDER, sortOrder.ordinal());
        if(searchQuery != null) outState.putString(STATE_SEARCH_QUERY, searchQuery);
    }

    @Override
    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;

        setSortOrder(getCurrentFragment(), sortOrder);
    }

    @Override
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;

        setSearchQueryOnFragment(getCurrentFragment(), searchQuery);
    }

    public void onFeedsChangedEvent(FeedsChangedEvent event) {
        Log.d(TAG, "onFeedsChangedEvent()");

        invalidateLists(event);
    }

    private void setParametersToFragment(ArticleListFragment fragment) {
        Log.v(TAG, "setParametersToFragment() started");
        if(fragment == null) return;

        setSortOrder(fragment, sortOrder);
        setSearchQueryOnFragment(fragment, searchQuery);
    }

    private void setSortOrder(ArticleListFragment fragment,
                              Sortable.SortOrder sortOrder) {
        if(fragment != null) fragment.setSortOrder(sortOrder);
    }

    private void setSearchQueryOnFragment(ArticleListFragment fragment, String searchQuery) {
        if(fragment != null) fragment.setSearchQuery(searchQuery);
    }

    private ArticleListFragment getCurrentFragment() {
        return adapter == null || viewPager == null ? null
                : adapter.getCachedFragment(viewPager.getCurrentItem());
    }

    private ArticleListFragment getFragment(int position) {
        return adapter != null ? adapter.getCachedFragment(position) : null;
    }

    private void invalidateLists(FeedsChangedEvent event) {
        if(!Collections.disjoint(event.getInvalidateAllChanges(), CHANGE_SET)) {
            updateAllLists(!Collections.disjoint(event.getInvalidateAllChanges(),
                    CHANGE_SET_FORCE_CONTENT_UPDATE));
            return;
        }

        if(!Collections.disjoint(event.getMainFeedChanges(), CHANGE_SET)) {
            updateList(ArticleListsPagerAdapter.positionByFeedType(FeedsChangedEvent.FeedType.MAIN),
                    !Collections.disjoint(event.getMainFeedChanges(), CHANGE_SET_FORCE_CONTENT_UPDATE));
        }
        if(!Collections.disjoint(event.getFavoriteFeedChanges(), CHANGE_SET)) {
            updateList(ArticleListsPagerAdapter.positionByFeedType(FeedsChangedEvent.FeedType.FAVORITE),
                    !Collections.disjoint(event.getFavoriteFeedChanges(), CHANGE_SET_FORCE_CONTENT_UPDATE));
        }
        if(!Collections.disjoint(event.getArchiveFeedChanges(), CHANGE_SET)) {
            updateList(ArticleListsPagerAdapter.positionByFeedType(FeedsChangedEvent.FeedType.ARCHIVE),
                    !Collections.disjoint(event.getArchiveFeedChanges(), CHANGE_SET_FORCE_CONTENT_UPDATE));
        }
    }

    private void updateAllLists(boolean forceContentUpdate) {
        Log.d(TAG, "updateAllLists() started; forceContentUpdate: " + forceContentUpdate);

        for(int i = 0; i < ArticleListsPagerAdapter.PAGES.length; i++) {
            ArticleListFragment f = getFragment(i);
            if(f != null) {
                if(forceContentUpdate) f.forceContentUpdate();
                f.invalidateList();
            } else {
                Log.w(TAG, "updateAllLists() fragment is null; position: " + i);
            }
        }
    }

    private void updateList(int position, boolean forceContentUpdate) {
        Log.d(TAG, String.format("updateList() position: %d, forceContentUpdate: %s",
                position, forceContentUpdate));

        if(position != -1) {
            ArticleListFragment f = getFragment(position);
            if(f != null) {
                if(forceContentUpdate) f.forceContentUpdate();
                f.invalidateList();
            } else {
                Log.w(TAG, "updateList() fragment is null");
            }
        } else {
            updateAllLists(forceContentUpdate);
        }
    }

    public void scroll(boolean up) {

        ArticleListFragment currentFragment = getCurrentFragment();

        if( currentFragment != null && currentFragment.recyclerViewLayoutManager != null) {
            LinearLayoutManager listLayout = currentFragment.recyclerViewLayoutManager;

            int numberOfVisibleItems =
                    listLayout.findLastCompletelyVisibleItemPosition() -
                            listLayout.findFirstCompletelyVisibleItemPosition() + 1;

            int oldPositionOnTop = listLayout.findFirstCompletelyVisibleItemPosition();

            // scroll so that as many new articles are visible than possible with one overlap
            int newPositionOnTop;
            if (up) {
                newPositionOnTop = oldPositionOnTop - numberOfVisibleItems + 1;
            } else {
                newPositionOnTop = oldPositionOnTop + numberOfVisibleItems - 1;
            }

            if (newPositionOnTop >= listLayout.getItemCount()) {
                newPositionOnTop = listLayout.getItemCount() - numberOfVisibleItems - 1;
            } else if (newPositionOnTop < 0) {
                newPositionOnTop = 0;
            }

            Log.v(TAG, " scrolling to position: " + newPositionOnTop);

            listLayout.scrollToPositionWithOffset(newPositionOnTop, 0);
        }
    }

}
