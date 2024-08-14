package com.ziehro.hailstorm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class PortfolioActivityAll extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_portfolio_fragment);

        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        List<String> portfolioIds = new ArrayList<>();
        portfolioIds.add("portfolio1");
        portfolioIds.add("portfolio2");
        portfolioIds.add("portfolio3");
        portfolioIds.add("portfolio4");
        portfolioIds.add("portfolio5");
        portfolioIds.add("portfolio6");

        PortfolioPagerAdapter pagerAdapter = new PortfolioPagerAdapter(this, portfolioIds);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> tab.setText(portfolioIds.get(position))).attach();
    }

    private static class PortfolioPagerAdapter extends FragmentStateAdapter {

        private final List<String> portfolioIds;

        public PortfolioPagerAdapter(AppCompatActivity activity, List<String> portfolioIds) {
            super(activity);
            this.portfolioIds = portfolioIds;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return new PortfolioFragment(portfolioIds.get(position));
        }

        @Override
        public int getItemCount() {
            return portfolioIds.size();
        }
    }
}
