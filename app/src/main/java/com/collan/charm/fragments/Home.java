package com.collan.charm.fragments;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.collan.charm.R;
import com.collan.charm.ReplacerActivity;
import com.collan.charm.adapter.HomeAdapter;
import com.collan.charm.adapter.StoriesAdapter;
import com.collan.charm.model.HomeModel;
import com.collan.charm.model.StoriesModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Home extends Fragment {

    HomeAdapter adapter;
    private RecyclerView recyclerView;
    private List<HomeModel> list;
    private FirebaseUser user;
    private final MutableLiveData<Integer> commentCount = new MutableLiveData<>();
    RecyclerView storiesRecyclerView;
    StoriesAdapter storiesAdapter;
    List<StoriesModel> storiesModelList;

    public Home() {
        // Required empty public constructor
    }
    

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        init(view);

        list = new ArrayList<>();
        adapter = new HomeAdapter(list, getActivity());
        recyclerView.setAdapter(adapter);

        loadDataFromFirestore();

        adapter.OnPressed(new HomeAdapter.OnPressed() {
            @Override
            public void onStarred(int position, String id, String uid, List<String> starList, boolean isChecked) {

                DocumentReference reference = FirebaseFirestore.getInstance().collection("Users")
                        .document(uid)
                        .collection("Post Images")
                        .document(id);

                if (starList.contains(user.getUid())) {
                    starList.remove(user.getUid()); //unstar
                }else{
                    starList.add(user.getUid()); //star
                }

                Map<String, Object> map = new HashMap<>();
                map.put("stars", starList);

                reference.update(map);

            }

            @Override
            public void setCommentCount(TextView textView) {

                Activity activity = getActivity();

                commentCount.observe((LifecycleOwner) activity, new Observer<Integer>() {
                    @Override
                    public void onChanged(Integer integer) {
                        if (commentCount.getValue() == 0) {
                            textView.setVisibility(View.GONE);
                        }else
                            textView.setVisibility(View.VISIBLE);

                        textView.setText("See all " + commentCount.getValue() + " comments");
                    }
                });




            }
        });

    }

    private void init(View view){

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (getActivity() != null)
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        storiesRecyclerView = view.findViewById(R.id.storiesRecyclerView);
        storiesRecyclerView.setHasFixedSize(true);
        storiesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));

        storiesModelList = new ArrayList<>();
        storiesModelList.add(new StoriesModel("","","",""));
        storiesAdapter = new StoriesAdapter(storiesModelList, getActivity());
        storiesRecyclerView.setAdapter(storiesAdapter);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        user = auth.getCurrentUser();

    }

    private void loadDataFromFirestore(){

        final DocumentReference reference = FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUid());

        final CollectionReference collectionReference = FirebaseFirestore.getInstance().collection("Users");

        reference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {

                if (error != null) {
                    Log.d("Error: ", error.getMessage());
                    return;
                }

                if (value == null)
                    return;

                List<String> uidList = (List<String>) value.get("following");

               if (uidList == null || uidList.isEmpty())
                   return;

                collectionReference.whereIn("uid", uidList)
                        .addSnapshotListener(new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                                if (error != null) {
                                    Log.d("Error: ", error.getMessage());
                                }

                                if (value == null)
                                    return;

                                for (QueryDocumentSnapshot snapshot : value) {

                                    snapshot.getReference().collection("Post Images")
                                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                                                    if (error != null) {
                                                        Log.d("Error: ", error.getMessage());
                                                    }

                                                    if (value == null)
                                                        return;

                                                    list.clear();

                                                    for (final QueryDocumentSnapshot snapshot : value) {

                                                        if (!snapshot.exists())
                                                            return;

                                                        HomeModel model = snapshot.toObject(HomeModel.class);

                                                        list.add(new HomeModel(
                                                                model.getName(),
                                                                model.getProfileImage(),
                                                                model.getImageUrl(),
                                                                model.getUid(),
                                                                model.getDescription(),
                                                                model.getId(),
                                                                model.getTimestamp(),
                                                                model.getStars()
                                                        ));

                                                        snapshot.getReference().collection("Comments").get()
                                                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                                        if (task.isSuccessful()){

                                                                            int count = 0;

                                                                            for (QueryDocumentSnapshot snapshot : task.getResult()) {
                                                                                count++;

                                                                            }
                                                                            commentCount.setValue(count);
                                                                            //comment count

                                                                        }
                                                                    }
                                                                });


                                                    }
                                                    adapter.notifyDataSetChanged();

                                                }
                                            });

                                    snapshot.getReference().collection("Stories")
                                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {

                                                    if (error != null) {
                                                        Log.d("Error: ", error.getMessage());
                                                    }

                                                    if (value == null)
                                                        return;

                                                    for (QueryDocumentSnapshot snapshot : value) {


                                                        if(!value.isEmpty()) {
                                                            StoriesModel model = snapshot.toObject(StoriesModel.class);
                                                            storiesModelList.add(model);
                                                        }
                                                    }
                                                    storiesAdapter.notifyDataSetChanged();



                                                }
                                            });

                                }

                            }
                        });

            }
        });

        }
    }