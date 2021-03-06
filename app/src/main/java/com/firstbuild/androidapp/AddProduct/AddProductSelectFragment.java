package com.firstbuild.androidapp.addproduct;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.firstbuild.androidapp.R;
import com.firstbuild.androidapp.productmanager.OpalInfo;
import com.firstbuild.androidapp.productmanager.ParagonInfo;
import com.firstbuild.androidapp.productmanager.ProductInfo;

public class AddProductSelectFragment extends Fragment {
    private String TAG = AddProductSelectFragment.class.getSimpleName();
    private AddProductActivity attached = null;

    public AddProductSelectFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof AddProductActivity) {
            attached = (AddProductActivity) context;
        }
        else {
            throw new ClassCastException(context + " must be an instance of "
                    + AddProductActivity.class.getSimpleName());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_add_product_paragon, container, false);

        view.findViewById(R.id.relative_layout_add_product_paragon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick pressed");

                attached.setNewProduct(new ParagonInfo(ProductInfo.PRODUCT_TYPE_PARAGON, "", ""));

                getFragmentManager().
                        beginTransaction().
                        replace(R.id.content_frame, new AddProductSearchParagonFragment()).
                        addToBackStack(null).
                        commit();
            }
        });

        view.findViewById(R.id.relative_layout_add_product_opal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick pressed");

                attached.setNewProduct(new OpalInfo(ProductInfo.PRODUCT_TYPE_OPAL, "", ""));

                getFragmentManager().
                        beginTransaction().
                        replace(R.id.content_frame, new AddProductSearchOpalFragment()).
                        addToBackStack(null).
                        commit();
            }
        });




        return view;
    }


}
