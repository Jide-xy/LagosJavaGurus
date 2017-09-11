package com.example.babajidemustapha.lagosjavagurus;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UsersPage extends AppCompatActivity {

    private final int per_page = 20;
    String response;
    URL address;
    JSONObject result;
    RecyclerView recyclerView;
    List<User> users;
    LinearLayout items_layout;
    int pages = 1;
    boolean loading = false;
    boolean dataEnd = false;
    int totalcount;
    UsersDatabase dbHelper;
    CustomAdapter1 adapter;
    RoundedBitmapDrawable placeholder;
    SwipeRefreshLayout swipe;
    LinearLayoutManager linearLayoutManager;
    ProgressBar progressBar;
    ProgressDialog pDialog;
    String query = "https://api.github.com/search/users?q=language:java+location:lagos&sort=followers&order=desc&per_page="+ per_page;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_page);
     //   listview = (ListView) findViewById(R.id.listview);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        linearLayoutManager = new LinearLayoutManager(UsersPage.this);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(UsersPage.this, R.drawable.divider));
        placeholder = RoundedBitmapDrawableFactory.create(getResources(),BitmapFactory.decodeResource(getResources(),R.drawable.github_mark));
        placeholder.setCircular(true);
        items_layout = (LinearLayout) findViewById(R.id.items_layout);
        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
        new LoadCachedUsers().execute();
        swipe.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pages = 1;
                loading = true;
                new LoadUsers().execute(query);
                swipe.setRefreshing(false);
            }
        });
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int totalItemCount = recyclerView.getLayoutManager().getItemCount();
                int visibleItemCount = recyclerView.getLayoutManager().getChildCount();
                int pastVisiblesItems = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();

                if (!loading && (visibleItemCount + pastVisiblesItems) == totalItemCount && !dataEnd) {
                    loading = true;
                    doNext();
                }
            }
        });

    }
    public void doNext(){
        try {
            //totalcount is not 0 if successfully visited internet to fetch data
            if (totalcount != 0) {
                //check if there's still more data to be fetched online
                if ((pages * per_page) <= totalcount) {
                    pages++;
                    new LoadUsers().execute(query + "&page=" + pages);
                } else {
                    Toast.makeText(getApplicationContext(), "No more users", Toast.LENGTH_SHORT).show();
                    loading = true;
                }
            }
            //if totalcount is 0 then we've not visited internet yet. Try to load offline
            else {
                pages++;
                new LoadCachedUsers().execute();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void saveImage(String imgname, Bitmap bitmap) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("images", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath = new File(directory, imgname + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean deleteImage(String imgPath) {
        File myPath = new File(imgPath);
        try {
            return myPath.delete();
        } catch (Exception e) {
            throw e;
        }
    }

    @NonNull
    private String getImagePath(String imgname) {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        return new File(cw.getDir("images", Context.MODE_PRIVATE), imgname + ".png").getAbsolutePath();
    }

    private class LoadUsers extends AsyncTask<String, Void, Void> {

        boolean apiLimit = false;
        boolean failed = false;
        RequestOptions saveSettings;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(pages <=1){
                pDialog = new ProgressDialog(UsersPage.this, R.style.MyTheme);
                pDialog.setMessage("Loading ...");
                pDialog.setIndeterminate(false);
                pDialog.setCancelable(false);
                pDialog.show();
            }
            else{
                progressBar.setVisibility(View.VISIBLE);
            }
            response = "";
            loading = true;
            saveSettings = new RequestOptions().skipMemoryCache(true).diskCacheStrategy(DiskCacheStrategy.NONE);
        }

        @Override
        protected Void doInBackground(String... url) {
          //  db = dbHelper.getWritableDatabase();
            try {
                address = new URL(url[0]);
                Log.e("connection : ", url[0]);
                HttpURLConnection conn = (HttpURLConnection) address.openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                conn.connect();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.e("page ", ""+ pages);
                    InputStream res = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(res));
                    String json = reader.readLine();
                    response += json;
                    Log.e("value ", response);
                    while (json != null) {
                        json = reader.readLine();
                        response += json;
                    }
                    if (pages <= 1) {
                        dbHelper.emptyTable();
                    }
                    if (!response.toLowerCase().contains("api limit exceeded")) {
                        apiLimit = false;
                        result = (JSONObject) new JSONTokener(response).nextValue();
                        JSONArray items = result.getJSONArray("items");
                        totalcount = Integer.parseInt(result.getString("total_count"));
                        if(items.length() > 0) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);
                                String picUrl = obj.getString("avatar_url");
                                try {
                                    String imgName = new SimpleDateFormat("ddMMyyyyHHmmss:SSSS").format(new Date());
                                    int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, getResources().getDisplayMetrics());
                                    //write image to disk
                                    saveImage(imgName,
                                            Glide.with(UsersPage.this).asBitmap().apply(saveSettings).load(picUrl).submit(px, px).get());
                                    Log.e("path", picUrl);
                                    //store user info and image path offline
                                    dbHelper.insertUser(obj.getString("login"), obj.getString("html_url"), picUrl, getImagePath(imgName));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    break;
                                }
                            }
                        }
                        else{
                            dataEnd = true;
                        }

                    } else {
                        apiLimit = true;
                    }
                } else {
                    failed = true;
                    Log.e("error", "No Internet");
                }
                conn.disconnect();
            }
            catch (Exception e) {
                e.printStackTrace();
                Log.e("Internet error", e.getMessage());
                failed = true;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void a) {
            //check if successfully visited and returned from internet
            if (!failed) {
                //if yes data is already stored offline, load from there
                new LoadCachedUsers().execute();
                if (apiLimit) {
                    Toast.makeText(UsersPage.this, "API Limit Exceeded. Please Try later", Toast.LENGTH_SHORT).show();
                }
                if(pages<=1) {
                    pDialog.dismiss();
                }
                else{
                    progressBar.setVisibility(View.GONE);
                }
                loading = false;
            }
            else {
                //internet not available or timeout error handling
                if(pages <=1) {
                    //if error occurred on trip to internet for the first time using app show error dialog
                    pDialog.dismiss();
                    AlertDialog.Builder builder;

                    builder = new AlertDialog.Builder(UsersPage.this);

                    builder.setTitle("Failed to connect")
                            .setMessage("Try again?")
                            .setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    new LoadUsers().execute(query + "&page=" + pages);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }
                else{
                    //else just hide loading footer and show a toast
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(UsersPage.this, "Failed to load new data", Toast.LENGTH_SHORT).show();
                    pages--;
                    loading = true;
                }

            }

        }
    }

    private class LoadCachedUsers extends AsyncTask<Void, Void, Void> {

                 Cursor cursor;
        @Override
        protected Void doInBackground(Void... params) {
            dbHelper = new UsersDatabase();
            if(pages <= 1) {
                //at first load initialize list and try to load fetch all cached user
                users = new ArrayList<>();
                cursor = dbHelper.getAllUsers();
                while(cursor.moveToNext()){
                    users.add(new User(cursor.getString(cursor.getColumnIndex("NAME")),
                            cursor.getString(cursor.getColumnIndex("URL")),
                            cursor.getString(cursor.getColumnIndex("IMAGE")),
                            cursor.getString(cursor.getColumnIndex("FILE"))));
                }

            }
            else{
                //subsequent loads adds freshly loaded data to former list
                    cursor = dbHelper.getReadableDatabase().rawQuery("SELECT * FROM USER LIMIT 20 OFFSET "+ ((pages-1) * per_page), null);
                    while(cursor.moveToNext()){
                        adapter.add(new User(cursor.getString(cursor.getColumnIndex("NAME")),
                                cursor.getString(cursor.getColumnIndex("URL")),
                                cursor.getString(cursor.getColumnIndex("IMAGE")),
                                cursor.getString(cursor.getColumnIndex("FILE"))));
                    }
            }

            return null;
        }
        @Override
        protected void onPostExecute(Void a){
            if(pages<=1) {
                if(cursor.getCount()==0){
                    //at first load if query turns up n result, attempt to visit internet
                    try {
                        new LoadUsers().execute(query);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else {
                    //else initialise and set adapter and calculate offset
                    adapter = new CustomAdapter1(UsersPage.this, users);
                    recyclerView.setAdapter(adapter);
                    loading = false;
                    pages = cursor.getCount() / per_page;
                }
            }
            else{
                if(cursor.getCount() != 0 ) {
                    //on subsequent loads if query isn't empty and data has been added to list, notify recyclerview of data change
                    adapter.notifyDataSetChanged();
                    loading = false;
                }
                else{
                    //else query turned up no result
                    if(!dataEnd) {
                        //if data from internet isn't exhausted, attempt to visit internet
                        new LoadUsers().execute(query + "&page=" + pages);
                    }
                    else{
                        Toast.makeText(UsersPage.this, "No more users", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private class UsersDatabase extends SQLiteOpenHelper {
        String createTable = "CREATE TABLE USER(_id INTEGER, NAME TEXT, URL TEXT, IMAGE TEXT, FILE TEXT)";

        private UsersDatabase() {
            super(getApplicationContext(),"UsersDB",null,2);
        }
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(createTable);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS USER");
            onCreate(db);
        }
        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion){

        }

        private void emptyTable() {
            SQLiteDatabase db = this.getWritableDatabase();
            try {
                Cursor cursor = db.rawQuery("Select FILE from USER", null);
                if(cursor != null && cursor.getCount() >0) {
                    while (cursor.moveToNext()) {
                        if (!deleteImage(cursor.getString(cursor.getColumnIndex("FILE")))) {
                           Toast.makeText(UsersPage.this,"Failed to delete image",Toast.LENGTH_SHORT).show();
                            break;
                        }

                    }
                }
                cursor.close();
            }
            catch (Exception e){
                e.printStackTrace();
            }
            db.execSQL("DROP TABLE IF EXISTS USER");
            onCreate(db);
        }

        private void insertUser(String name, String url, String imgUrl, String imgPath) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("NAME", name);
            values.put("URL", url);
            values.put("IMAGE", imgUrl);
            values.put("FILE", imgPath);
            db.insert("USER",null,values);
        }

        private Cursor getAllUsers() {
            SQLiteDatabase db = this.getReadableDatabase();
            return db.query("USER", null, null, null, null, null, null);
        }
    }

    private class CustomAdapter1 extends RecyclerView.Adapter<CustomAdapter1.ViewHolder> {
        Context context;
        List<User> source;

RequestOptions settings = new RequestOptions()
        .transform(new CircleCrop())
        .placeholder(placeholder)
        .error(placeholder);

        private CustomAdapter1(Context context, List<User> source) {

            this.context = context;
            this.source = source;
        }

        private void add(User user) {
            source.add(user);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.list_view_item_layout,parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.username.setText("@" + source.get(position).getUsername());

            Glide.with(UsersPage.this)
                    .asBitmap()
                    .transition(new BitmapTransitionOptions().crossFade())
                    .load(source.get(position).getFile())
                    .apply(settings)
                    .into(holder.avatar);
        }

        @Override
        public int getItemCount() {
            return source.size();
        }

        protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView username;
            ImageView avatar;

            private ViewHolder(View itemView) {
                super(itemView);
                username = (TextView) itemView.findViewById(R.id.username);
                avatar = (ImageView) itemView.findViewById(R.id.avatar);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UsersPage.this, ProfilePage.class);
                intent.putExtra("username", source.get(getAdapterPosition()).getUsername());
                intent.putExtra("gitUrl", source.get(getAdapterPosition()).getGitUrl());
                intent.putExtra("avatarUrl", source.get(getAdapterPosition()).getAvi());
                intent.putExtra("filePath", source.get(getAdapterPosition()).getFile());
                startActivity(intent);
            }
        }
    }

    public class DividerItemDecoration extends RecyclerView.ItemDecoration {

//        private final int[] ATTRS = new int[]{android.R.attr.listDivider};

        private Drawable divider;

//        /**
//         * Default divider will be used
//         */
//        public DividerItemDecoration(Context context) {
//            final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
//            divider = styledAttributes.getDrawable(0);
//            styledAttributes.recycle();
//        }

        /**
         * Custom divider will be used
         */
        public DividerItemDecoration(Context context, int resId) {
            divider = ContextCompat.getDrawable(context, resId);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + divider.getIntrinsicHeight();

                divider.setBounds(left, top, right, bottom);
                divider.draw(c);
            }
        }
    }
}
