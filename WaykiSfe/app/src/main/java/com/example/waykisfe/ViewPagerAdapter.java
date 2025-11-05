package com.example.waykisfe;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // Devuelve el fragmento correspondiente según la posición
        switch (position) {
            case 0:
                return new FragmentFunciones();
            case 1:
                return new FragmentPermisos();
            case 2:
                return new FragmentConsejos();
            default:
                return new FragmentFunciones();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // Número total de fragmentos
    }
}
