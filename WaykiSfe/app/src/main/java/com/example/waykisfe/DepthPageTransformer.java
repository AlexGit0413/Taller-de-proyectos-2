package com.example.waykisfe;

import android.view.View;
import androidx.viewpager2.widget.ViewPager2;

public class DepthPageTransformer implements ViewPager2.PageTransformer {
    @Override
    public void transformPage(View page, float position) {
        if (position < -1) {
            page.setAlpha(0f); // página fuera a la izquierda
        } else if (position <= 0) {
            page.setAlpha(1f);
            page.setTranslationX(0f);
            page.setScaleX(1f);
            page.setScaleY(1f);
        } else if (position <= 1) {
            page.setAlpha(1 - position);
            page.setTranslationX(page.getWidth() * -position);
            float scaleFactor = 0.75f + (1 - 0.75f) * (1 - Math.abs(position));
            page.setScaleX(scaleFactor);
            page.setScaleY(scaleFactor);
        } else {
            page.setAlpha(0f);
        }
    }
}
