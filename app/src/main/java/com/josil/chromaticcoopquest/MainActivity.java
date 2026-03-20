package com.josil.chromaticcoopquest;

import android.app.*;
import android.os.*;

public class MainActivity extends Activity 
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setContentView(R.layout.main);
        FragmentManager fragmentManager = getFragmentManager();
        Fragment fragment = new Main();
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }
}
