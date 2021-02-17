package com.sumeshpandit.audio;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button btnChoose, btnUpload;
    private Uri filePath;
    private final int PICK_AUD_REQUEST = 71;
    FirebaseStorage storage;
    StorageReference storageReference;
    DatabaseReference mDataReference;
    ArrayList<String> al = new ArrayList<>();
    ArrayList<String> al2 = new ArrayList<>();
    ListView songsList;
    MediaPlayer player;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        player = new MediaPlayer();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        songsList = (ListView)findViewById(R.id.songsList);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Opening Exploler", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                chooseAud();
            }
        });

        mDataReference = FirebaseDatabase.getInstance().getReference("audios");

        mDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                al = collectnms((Map<String,Object>) dataSnapshot.getValue());
                al2 = collecturls((Map<String,Object>) dataSnapshot.getValue());
                songsList.setVisibility(View.VISIBLE);

                songsList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, al));

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        songsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(MainActivity.this, "Playing wait seconds ...", Toast.LENGTH_LONG).show();
                try {
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    if (player.isPlaying()){
                        player.stop();
                        player.reset();
                    }
                    player.setDataSource(MainActivity.this, Uri.parse(al2.get(position)));
                    player.prepare();
                    player.start();
                } catch(Exception e) {
                    System.out.println(e.toString()+"aaas");
                }
            }
        });

    }

    private ArrayList<String> collecturls(Map<String, Object> value) {
        ArrayList<String> urls = new ArrayList<>();
        int i = 0;
        try {
            for (Map.Entry<String, Object> entry : value.entrySet()) {

                //Get user map
                Map singleUser = (Map) entry.getValue();
                urls.add((String) singleUser.get("url"));
                //Toast.makeText(MapsActivity.this, lats.get(i) +"|"+lngs.get(i), Toast.LENGTH_SHORT).show();

                i++;
            }
        }
        catch(NullPointerException e)
        {
            System.out.print("NullPointerException Caught");
        }

        return urls;

    }

    private ArrayList<String> collectnms(Map<String, Object> value) {
        ArrayList<String> nms = new ArrayList<>();
        int i = 0;
        try {
            for (Map.Entry<String, Object> entry : value.entrySet()) {

                //Get user map
                Map singleUser = (Map) entry.getValue();
                nms.add((String) singleUser.get("name"));
                //Toast.makeText(MapsActivity.this, lats.get(i) +"|"+lngs.get(i), Toast.LENGTH_SHORT).show();

                i++;
            }
        }
        catch(NullPointerException e)
        {
            System.out.print("NullPointerException Caught");
        }
        return nms;
    }


    private void chooseAud() {
        Intent intent = new Intent();
        intent.setType("audio/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Aud"), PICK_AUD_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_AUD_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null )
        {
            Toast.makeText(this, "Got data", Toast.LENGTH_SHORT).show();
            filePath = data.getData();
        }
        uploadAud();
    }

    public void uploadAud()
    {
        final ProgressDialog pd=new ProgressDialog(this);
        pd.setTitle("File Uploading....!!!");
        pd.show();

        final StorageReference reference=storageReference.child("uploads/"+System.currentTimeMillis()+".mp3");

        reference.putFile(filePath)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        reference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                String name = taskSnapshot.getMetadata().getName().toString();
                                String url = uri.toString();
                                writeNewAudInfoToDB(name, url);

                                pd.dismiss();
                                Toast.makeText(getApplicationContext(),"File Uploaded",Toast.LENGTH_LONG).show();

                                mDataReference.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        al = collectnms((Map<String,Object>) dataSnapshot.getValue());
                                        al2 = collecturls((Map<String,Object>) dataSnapshot.getValue());
                                        songsList.setVisibility(View.VISIBLE);

                                        songsList.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, al));
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });

                            }
                        });

                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        float percent=(100*taskSnapshot.getBytesTransferred())/taskSnapshot.getTotalByteCount();
                        pd.setMessage("Uploaded :"+(int)percent+"%");
                    }
                });


    }

    private void writeNewAudInfoToDB(String name, String url) {
        UploadInfo info = new UploadInfo(name, url);

        String key = mDataReference.push().getKey();
        mDataReference.child(key).setValue(info);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}