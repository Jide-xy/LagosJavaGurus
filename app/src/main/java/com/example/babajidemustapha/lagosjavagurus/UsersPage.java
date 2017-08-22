package com.example.babajidemustapha.lagosjavagurus;

import android.app.AlertDialog;
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
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.TransitionOptions;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.transition.Transition;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UsersPage extends AppCompatActivity {

    String response;
    URL address;
    JSONObject result;
    //ListView listview;
    RecyclerView recyclerView;
    List<User> users;
    LinearLayout items_layout;
    int pages = 1;
    private final int per_page = 20;
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
        recyclerView.setVerticalScrollBarEnabled(true);
        placeholder = RoundedBitmapDrawableFactory.create(getResources(),BitmapFactory.decodeResource(getResources(),R.drawable.github_mark));
        placeholder.setCircular(true);
        items_layout = (LinearLayout) findViewById(R.id.items_layout);
        swipe = (SwipeRefreshLayout) findViewById(R.id.swipe);
        new LoadCachedUsers().execute();
       // new LoadUsers().execute(query);
        swipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                pages = 1;
                loading = true;
              //  doNext();
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

//        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Intent intent = new Intent(getApplicationContext(),ProfilePage.class);
////                intent.putExtra("username",users.get(position).username);
////                intent.putExtra("gitUrl",users.get(position).gitUrl);
//              //  intent.putExtra("avatarUrl",users.get(position).aviUrl);
//                 startActivity(intent);
//            }
//
//        });
//        listview.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                if(firstVisibleItem+visibleItemCount == totalItemCount && totalItemCount!=0)
//                {
//                    if(loading == false)
//                    {
//                        loading = true;
//                      doNext();
//                    }
//                }
//            }
//        });
    }
    public void doNext(){
        try {
            if (totalcount != 0) {
                if ((pages * per_page) <= totalcount) {
                    pages++;
                    new LoadUsers().execute(query + "&page=" + pages);
                } else {
                    Toast.makeText(getApplicationContext(), "No more users", Toast.LENGTH_SHORT).show();
                    loading = true;
                }
            }
            else {
                pages++;
                new LoadCachedUsers().execute();
               // Toast.makeText(getApplicationContext(), "Showing cached data. Refresh to load from internet", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    public class LoadUsers extends AsyncTask<String,Void,Void> {

        int count;
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
               //  Log.e("connection code: ", conn.getResponseMessage());
               // Log.e("connection code: ", "" +HttpURLConnection.HTTP_OK);
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(20000);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Log.e("value ", "here");
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
                 //       count = (per_page * (pages - 1)) + items.length();
                        totalcount = Integer.parseInt(result.getString("total_count"));
                        if(items.length() > 0) {
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject obj = items.getJSONObject(i);
                                String picUrl = obj.getString("avatar_url");
                                try {
//                                users.add(i, new User(obj.getString("login"), obj.getString("html_url"), obj.getString("avatar_url"),
//                                        BitmapFactory.decodeStream(picUrl.openConnection().getInputStream())));
//                                avatars.add(i, BitmapFactory.decodeStream(picUrl.openConnection().getInputStream()));                     Flie
                                    String imgName = new SimpleDateFormat("ddMMyyyyHHmmss:SSSS").format(new Date());
                                    saveImage(imgName,
                                            Glide.with(UsersPage.this).asBitmap().apply(saveSettings).load(picUrl).submit(320, 320).get());
                                    Log.e("path", picUrl);
                                    dbHelper.insertUser(obj.getString("login"), obj.getString("html_url"), picUrl, getImagePath(imgName));
                                } catch (Exception e) {
//                                users.add(i, new User(obj.getString("login"), obj.getString("html_url"), obj.getString("avatar_url"),
//                                        null));
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
           new LoadCachedUsers().execute();
            if (!failed) {
                if (!apiLimit) {

                } else {
                   Toast.makeText(UsersPage.this,"API Limit Exceeded. Please Try later",Toast.LENGTH_SHORT).show();
                }
                if(pages<=1) {
                    pDialog.dismiss();
                }
                else{
                    progressBar.setVisibility(View.GONE);
                }
            }
            else {
                if(pages <=1) {
                    pDialog.dismiss();
                }
                else{
                    progressBar.setVisibility(View.GONE);
                }
               Toast.makeText(UsersPage.this,"Failed to load new data",Toast.LENGTH_SHORT).show();
                pages--;
            }
            loading = false;
        }
    }
    private void saveImage(String imgname, Bitmap bitmap){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        // path to /data/data/yourapp/app_data/imageDir
        File directory = cw.getDir("images", Context.MODE_PRIVATE);
        // Create imageDir
        File mypath=new File(directory,imgname+".png");
        try {
            FileOutputStream  fos = new FileOutputStream(mypath);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void deleteImage(String imgname){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("images", Context.MODE_PRIVATE);
        File mypath=new File(directory,imgname+".png");
        mypath.delete();
    }
    private String getImagePath(String imgname){
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
       return new File(cw.getDir("images", Context.MODE_PRIVATE),imgname+".png").getAbsolutePath();
    }
    public  class LoadCachedUsers extends AsyncTask<Void,Void,Void>{

                 Cursor cursor;
        @Override
        protected Void doInBackground(Void... params) {
            dbHelper = new UsersDatabase();
            if(pages <= 1) {
                users = new ArrayList<User>();
                cursor = dbHelper.getAllUsers();
                while(cursor.moveToNext()){
                    users.add(new User(cursor.getString(cursor.getColumnIndex("NAME")),
                            cursor.getString(cursor.getColumnIndex("URL")),
                            cursor.getString(cursor.getColumnIndex("IMAGE")),
                            cursor.getString(cursor.getColumnIndex("FILE"))));
                }

            }
            else{
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
                    try {
                        new LoadUsers().execute(query);
                    }
                    catch (Exception e){
                        e.printStackTrace();
                    }
                }
                else {
                    adapter = new CustomAdapter1(UsersPage.this, users);
                    recyclerView.setAdapter(adapter);
                    loading = false;
                //    pages = cursor.getCount() / per_page;
                }
            }
            else{
                if(cursor.getCount() != 0 ) {
                    adapter.notifyDataSetChanged();
                    loading = false;
                }
                else{
                    if(!dataEnd) {
                        new LoadUsers().execute(query + "&page=" + pages);
                    }
                    else{
                        Toast.makeText(UsersPage.this,"data end",Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    public class UsersDatabase extends SQLiteOpenHelper{
        String createTable = "CREATE TABLE USER(_id INTEGER, NAME TEXT, URL TEXT, IMAGE TEXT, FILE TEXT)";
        public UsersDatabase(){
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
        public void emptyTable(){
            SQLiteDatabase db = this.getWritableDatabase();
            try {
                Cursor cursor = db.rawQuery("Select IMAGE from USER ", null);
                if(cursor != null && cursor.getCount() >0) {
                    while (cursor.moveToNext()) {
                        deleteImage(cursor.getColumnName(cursor.getColumnIndex("IMAGE")));
                    }
                }
            }
            catch (Exception e){
                e.printStackTrace();
            }
            db.execSQL("DROP TABLE IF EXISTS USER");
            onCreate(db);
        }
        public  void insertUser(String name, String url, String imgUrl, String imgPath){
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("NAME", name);
            values.put("URL", url);
            values.put("IMAGE", imgUrl);
            values.put("FILE", imgPath);
            db.insert("USER",null,values);
        }
        public Cursor getAllUsers(){
            SQLiteDatabase db = this.getReadableDatabase();
            return db.query("USER",null,null,null,null,null,null,"20");
        }
    }
//    public class CustomAdapter extends ArrayAdapter<User>{
//        Context context;
//        List<User> source;
//        public CustomAdapter(Context context, int layoutResource, List<User> source ){
//            super(context,layoutResource,source);
//            this.context = context;
//            this.source = source;
//        }
//        @Override
//        public int getCount() {
//            return super.getCount();
//        }
//        @Override
//        public void add(User user){
//            source.add(user);
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//            View view;
//            LayoutInflater inflater = getLayoutInflater();
//            view = inflater.inflate(R.layout.list_view_item_layout,items_layout, true);
//            TextView username = (TextView) view.findViewById(R.id.username);
//            ImageView avi = (ImageView) view.findViewById(R.id.avatar);
//            username.setText("@"+source.get(position).username);
//            if(source.get(position).avi != null) {
//              //  avi.setImageBitmap(source.get(position).aviUrl);
//            }
//            return view;
//        }
//    }

    public class CustomAdapter1 extends RecyclerView.Adapter<CustomAdapter1.ViewHolder>{
        Context context;
        List<User> source;

RequestOptions settings = new RequestOptions()
        .transform(new CircleCrop())
        .placeholder(placeholder)
        .error(placeholder);
        public  void add(User user){
            source.add(user);
        }
        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            TextView username;
            ImageView avatar;
            public ViewHolder(View itemView) {
                super(itemView);
                username = (TextView) itemView.findViewById(R.id.username);
                avatar = (ImageView) itemView.findViewById(R.id.avatar);
                itemView.setOnClickListener(this);
            }
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(UsersPage.this,ProfilePage.class);
                intent.putExtra("username",source.get(getAdapterPosition()).username);
              intent.putExtra("gitUrl",source.get(getAdapterPosition()).gitUrl);
                  intent.putExtra("avatarUrl",source.get(getAdapterPosition()).avi);
                intent.putExtra("filePath", source.get(getAdapterPosition()).file);
                startActivity(intent);
            }
        }
        public CustomAdapter1(Context context, List<User> source ){

            this.context = context;
            this.source = source;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(getLayoutInflater().inflate(R.layout.list_view_item_layout,parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.username.setText("@"+source.get(position).username);
                Log.e("on bind viewholder" ,new File(source.get(position).file).getPath());

            Glide.with(UsersPage.this)
                    .asBitmap()
                    .transition(new BitmapTransitionOptions().crossFade())
                    .load(source.get(position).file)
                    .apply(settings)
                    .into(holder.avatar);
        }

        @Override
        public int getItemCount() {
            return source.size();
        }
    }
}
