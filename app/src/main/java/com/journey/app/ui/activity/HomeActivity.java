package com.journey.app.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.journey.app.R;
import com.journey.app.api.JourneyApi;
import com.journey.app.model.CardItem;
import com.journey.app.model.Fragment;
import com.journey.app.model.FragmentCardItem;
import com.journey.app.model.Travel;
import com.journey.app.model.TravelCardItem;
import com.journey.app.model.User;
import com.journey.app.ui.fragment.CardListFragment;
import com.journey.app.util.Api;
import com.journey.app.util.Auth;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.squareup.seismic.ShakeDetector;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private final static int REQUEST_AUTH = 0;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.container) ViewPager viewPager;
    @BindView(R.id.tabs) TabLayout tabs;
    @BindView(R.id.fab) FloatingActionButton fab;

    private SectionsPagerAdapter sectionsPagerAdapter;

    private Drawer drawer;
    private AccountHeader accountHeader;
    private ProfileDrawerItem profile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ButterKnife.bind(this);

        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkAuthStatus()) {
            loadLoggedInUser();
            loadHomeTimeline();
            loadExplore();

            initShake();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_AUTH && resultCode == RESULT_OK) {
            loadLoggedInUser();
        } else {
            finish();
        }
    }

    private void initView() {
        initToolbar();
        initDrawer();
        initTabs();
        initFab();
    }

    private void initToolbar() {
        setSupportActionBar(toolbar);
    }

    private void initDrawer() {
        profile = new ProfileDrawerItem()
                .withIdentifier(1L)
                .withName("User")
                .withIcon(R.drawable.avatar3)
                .withEmail("12345678912");
        accountHeader = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.color.orange_500)
                .addProfiles(profile)
                .withOnAccountHeaderSelectionViewClickListener(new AccountHeader.OnAccountHeaderSelectionViewClickListener() {
                    @Override public boolean onClick(View view, IProfile profile) {
                        return false;
                    }
                })
                .withSelectionListEnabled(false)
                .build();

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withAccountHeader(accountHeader)
                .addDrawerItems(new PrimaryDrawerItem().withName("首页").withIdentifier(0L),
                        new PrimaryDrawerItem().withName("我的足迹").withIdentifier(1L),
                        new PrimaryDrawerItem().withName("我的收藏"),
                        new PrimaryDrawerItem().withName("设置"),
                        new PrimaryDrawerItem().withName("反馈"))
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem.getIdentifier() == 1L) {
                            Intent intent = new Intent(HomeActivity.this, MyFragmentsActivity.class);
                            intent.putExtra("USER_ID", 3);
                            startActivity(intent);
                            return true;
                        }
                        return false;
                    }
                })
                .build();
    }

    private void initTabs() {
        sectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), this);
        sectionsPagerAdapter.setOnRefreshHomeListener(new SectionsPagerAdapter.OnRefreshHomeListener() {
            @Override public void onRefreshHome() {
                loadHomeTimeline();
            }
        });
        sectionsPagerAdapter.setOnRefreshExploreListener(new SectionsPagerAdapter.OnRefreshExploreListener() {
            @Override public void onRefreshExplore() {
                loadExplore();
            }
        });
        sectionsPagerAdapter.setOnViewTravelListener(new SectionsPagerAdapter.OnViewTravelListener() {
            @Override public void onViewTravel(int travelId) {
                Intent viewTravelIntent = new Intent(HomeActivity.this, TravelActivity.class);
                viewTravelIntent.putExtra("TRAVEL_ID", travelId);
                startActivity(viewTravelIntent);
            }
        });

        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));

        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    fab.show();
                } else {
                    fab.hide();
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {
                // Ignored
            }

            @Override public void onTabReselected(TabLayout.Tab tab) {
                // Ignored
            }
        });
    }

    private void initFab() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(HomeActivity.this, SendActivity.class));
            }
        });
    }

    private boolean checkAuthStatus() {
        if (!Auth.hasLoggedIn()) {
            Intent authIntent = new Intent(this, EntryActivity.class);
            startActivityForResult(authIntent, REQUEST_AUTH);
            return false;
        }

        return true;
    }

    private void loadLoggedInUser() {
        int loggedInUserId = Auth.getLoggedInUserId();
        JourneyApi api = Api.getApiInstance();
        Call<User> getUser = api.getUser(3);
        getUser.enqueue(new Callback<User>() {
            @Override public void onResponse(Call<User> call, Response<User> response) {
                User user = response.body();
                if (user == null) {
                    Auth.logout();
                    checkAuthStatus();
                } else {
                    profile.withName(user.username);
                    profile.withEmail(user.phone);
                    accountHeader.updateProfile(profile);
                }
            }

            @Override public void onFailure(Call<User> call, Throwable t) {

            }
        });
    }

    private void loadHomeTimeline() {
        JourneyApi api = Api.getApiInstance();
        Call<ArrayList<com.journey.app.model.Fragment>> getFragments = api.getHomeFragments();
        getFragments.enqueue(new Callback<ArrayList<com.journey.app.model.Fragment>>() {
            @Override public void onResponse(Call<ArrayList<Fragment>> call, Response<ArrayList<com.journey.app.model.Fragment>> response) {
                ArrayList<com.journey.app.model.Fragment> fragments = response.body();
                sectionsPagerAdapter.setHomeTimeline(fragments);
                sectionsPagerAdapter.stopHomeRefreshing();
            }

            @Override public void onFailure(Call<ArrayList<Fragment>> call, Throwable t) {
                //
            }
        });
    }

    private void loadExplore() {
        JourneyApi api = Api.getApiInstance();
        Call<ArrayList<Travel>> getTravels = api.getHomeTravels();
        getTravels.enqueue(new Callback<ArrayList<Travel>>() {
            @Override public void onResponse(Call<ArrayList<Travel>> call, Response<ArrayList<Travel>> response) {
                ArrayList<Travel> travels = response.body();
                sectionsPagerAdapter.setExplore(travels);
                sectionsPagerAdapter.stopExploreRefreshing();
            }

            @Override public void onFailure(Call<ArrayList<Travel>> call, Throwable t) {
                //
            }
        });
    }

    private void initShake() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        ShakeDetector shakeDetector = new ShakeDetector(new ShakeDetector.Listener() {
            @Override public void hearShake() {
                sectionsPagerAdapter.startHomeRefreshing();
                loadHomeTimeline();
            }
        });
        shakeDetector.start(sensorManager);
    }

    public static class SectionsPagerAdapter extends FragmentPagerAdapter {

        public interface OnRefreshHomeListener {
            void onRefreshHome();
        }

        public interface OnRefreshExploreListener {
            void onRefreshExplore();
        }

        public interface OnViewTravelListener {
            void onViewTravel(int travelId);
        }

        private OnRefreshHomeListener onRefreshHomeListener;
        private OnRefreshExploreListener onRefreshExploreListener;
        private OnViewTravelListener onViewTravelListener;

        public void setOnRefreshHomeListener(OnRefreshHomeListener listener) {
            this.onRefreshHomeListener = listener;
        }

        public void setOnRefreshExploreListener(OnRefreshExploreListener listener) {
            this.onRefreshExploreListener = listener;
        }

        public void setOnViewTravelListener(OnViewTravelListener listener) {
            onViewTravelListener = listener;
        }

        private CardListFragment homeFragment;
        private CardListFragment exploreFragment;
        private CardListFragment topicsFragment;

        public void startHomeRefreshing() {
            homeFragment.startRefreshing();
        }

        public void stopHomeRefreshing() {
            homeFragment.stopRefreshing();
        }

        public void stopExploreRefreshing() {
            exploreFragment.stopRefreshing();
        }

        public SectionsPagerAdapter(FragmentManager fm, Context context) {
            super(fm);

            homeFragment = new CardListFragment(context);
            exploreFragment = new CardListFragment(context);
            topicsFragment = new CardListFragment(context);

            homeFragment.setOnRefreshListener(new CardListFragment.OnRefreshListener() {
                @Override public void onRefresh() {
                    if (onRefreshHomeListener != null) {
                        onRefreshHomeListener.onRefreshHome();
                    }
                }
            });
            exploreFragment.setOnRefreshListener(new CardListFragment.OnRefreshListener() {
                @Override public void onRefresh() {
                    if (onRefreshExploreListener != null) {
                        onRefreshExploreListener.onRefreshExplore();
                    }
                }
            });
            exploreFragment.setOnViewTravelListener(new CardListFragment.OnViewTravelListener() {
                @Override public void onViewTravel(int travelId) {
                    if (onViewTravelListener != null) {
                        onViewTravelListener.onViewTravel(travelId);
                    }
                }
            });
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            switch (position) {
                case 0:
                default:
                    return homeFragment;
                case 1:
                    return exploreFragment;
                case 2:
                    return topicsFragment;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        public void setHomeTimeline(List<Fragment> fragments) {
            List<CardItem> items = new ArrayList<>();
            for (Fragment fragment : fragments) {
                FragmentCardItem item = new FragmentCardItem();
                item.fragment = fragment;
                items.add(item);
            }
            homeFragment.setItems(items);
        }

        public void setExplore(List<Travel> travels) {
            List<CardItem> items = new ArrayList<>();
            for (Travel travel : travels) {
                TravelCardItem item = new TravelCardItem(travel);
                items.add(item);
            }
            exploreFragment.setItems(items);
        }

    }
}
