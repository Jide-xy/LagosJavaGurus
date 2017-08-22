package com.example.babajidemustapha.lagosjavagurus;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;


public class ProfilePage extends AppCompatActivity {
    TextView username;
    TextView url;
    ImageView img;
    String name;
    String link;
    String imgUrl;
    String imgPath;
    RoundedBitmapDrawable placeholder;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_page);
        Bundle extras = getIntent().getExtras();
        name = extras.getString("username");
        link = extras.getString("gitUrl");
        imgUrl = extras.getString("avatarUrl");
        imgPath = extras.getString("filePath");
        placeholder = RoundedBitmapDrawableFactory.create(getResources(),BitmapFactory.decodeResource(getResources(),R.drawable.github_mark));
        placeholder.setCircular(true);
        username = (TextView) findViewById(R.id.username);
        url = (TextView) findViewById(R.id.link);
        img = (ImageView) findViewById(R.id.avatar);
        username.setText("@"+name);
        SpannableString clickableLink = new SpannableString(link);
        clickableLink.setSpan(new URLSpan(link){
            @Override
            public void onClick(View view){
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(this.getURL()));
                if(intent.resolveActivity(getPackageManager()) != null){
                    startActivity(intent);
                }
                else{
                    Toast.makeText(getApplicationContext(),"Browser not found",Toast.LENGTH_SHORT).show();
                }
            }
        },0,clickableLink.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        url.setText(clickableLink);
        url.setMovementMethod(LinkMovementMethod.getInstance());
        RequestOptions settings = new RequestOptions()
                .transform(new CircleCrop())
                .override(1000,1000)
                .placeholder(placeholder)
                .error(placeholder);
        RequestOptions thumbNailSettings = new RequestOptions()
                .transform(new CircleCrop())
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE);
        Log.e("url", imgUrl);
        Glide.with(ProfilePage.this)
                .asBitmap()
                .transition(new BitmapTransitionOptions().crossFade())
                .load(imgUrl)
                .thumbnail(
                         Glide.with(ProfilePage.this)
                                 .asBitmap()
                               .load(imgPath)
                                .apply(thumbNailSettings))
                .apply(settings)
                .into(img);

    }
    public void share(View view){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT,"Check out this amazing developer @"+ name +", "+link);
        intent.setType("text/plain");
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        }
        else{
            Toast.makeText(ProfilePage.this,"Browser not found",Toast.LENGTH_SHORT).show();
        }
    }

}
