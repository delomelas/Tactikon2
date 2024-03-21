package uk.co.eidolon.shared.utils;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class BannerViewPager extends ViewPager
{
    PagerAdapter mPagerAdapter;

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        if (mPagerAdapter != null)
        {
            super.setAdapter(mPagerAdapter);
            //mPageIndicator.setViewPager(this);
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter) {
    }

    public void storeAdapter(PagerAdapter pagerAdapter) {
        mPagerAdapter = pagerAdapter;
    }

    public BannerViewPager(Context context) {
        super(context);
    }

    public BannerViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

}