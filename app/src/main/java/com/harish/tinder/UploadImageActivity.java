package com.harish.tinder;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.harish.tinder.material_ui.MainActivity;
import com.harish.tinder.utils.MyData;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import id.zelory.compressor.Compressor;

public class UploadImageActivity extends AppCompatActivity {
    private CircleImageView profileImageView;
    Button saveChanges;
    private FirebaseUser mCurrentUser;
    ImageButton addButton;
    LinearLayout rootLayout;
    private MyData myData;
    RelativeLayout loading;
    private FirebaseAuth mAuth;
    private DatabaseReference mUserRef;
    private ProgressDialog mProgressDialog;
    private StorageReference mImageStorage;
    private int GALLERY_PICK = 1;
    private DatabaseReference mUserDatabase;
    private ProgressDialog mRegProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image);

        myData = new MyData();
        rootLayout = findViewById(R.id.rootlayout);
        profileImageView = findViewById(R.id.profile_image);
        addButton = findViewById(R.id.add_image);
        saveChanges = findViewById(R.id.save_changes);

        mCurrentUser = FirebaseAuth.getInstance().getCurrentUser();
        mImageStorage = FirebaseStorage.getInstance().getReference();
        loading = findViewById(R.id.loadingPanel);

        mAuth = FirebaseAuth.getInstance();
        String current_uid = mCurrentUser.getUid();
        mUserDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child(current_uid);
        mUserDatabase.keepSynced(true);
        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addImage();
            }
        });
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addImage();
            }
        });
        saveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveChanges();
            }
        });
        mRegProgress = new ProgressDialog(UploadImageActivity.this,
                R.style.AppTheme);
        mRegProgress.setIndeterminate(true);
        mRegProgress.setCanceledOnTouchOutside(false);
        mRegProgress.setMessage("Uploading...");

    }


    private void saveChanges() {
        mRegProgress.show();
    }

    private void uploadStatus(String status) {
        mUserDatabase.child("status").setValue(status).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    mRegProgress.dismiss();

                    sendToStart();
                } else {
                    Snackbar.make(rootLayout, "Something Went Wrong!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendToStart() {
        //FirebaseAuth.getInstance().signOut();
        Intent startIntent = new Intent(UploadImageActivity.this, MainActivity.class);
        startActivity(startIntent);
        finish();
    }

    private void addImage() {
        if (myData.isInternetConnected(UploadImageActivity.this)) {
            chooseImage();
        } else {
            Snackbar.make(rootLayout, "No Internet Connection!", Snackbar.LENGTH_LONG).show();
        }
    }

    private void chooseImage() {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);


        startActivityForResult(Intent.createChooser(galleryIntent, "SELECT IMAGE"), GALLERY_PICK);


//                CropImage.activity()
//                        .setGuidelines(CropImageView.Guidelines.ON)
//                        .start(SettingsActivity.this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            CropImage.activity(imageUri).setAspectRatio(1, 1).start(this);
        }
        try {
            if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if (resultCode == RESULT_OK) {

                    mProgressDialog = new ProgressDialog(UploadImageActivity.this);
                    mProgressDialog.setTitle("Uploading Image....");
                    mProgressDialog.setMessage("Please wait while we upload and process the image");
                    mProgressDialog.setCanceledOnTouchOutside(false);
                    mProgressDialog.show();
                    Uri resultUri = result.getUri();
                    File thumb_filepath = new File(resultUri.getPath());

                    String current_user_id = mCurrentUser.getUid();
//                    Bitmap thumb_bitmap =  Compressor.INSTANCE.compress(this,thumb_filepath,)
//                        resolution(1280, 720)
//                        quality(80)
//                        format(Bitmap.CompressFormat.WEBP)
//                        size(2_097_152) // 2 MB
//                    }
//
//                    Bitmap thumb_bitmap = Compressor
//                            .setMaxWidth(200)
//                            .setMaxHeight(200)
//                            .setQuality(75)
//                            .compressToBitmap(thumb_filepath);

                    Bitmap thumb_bitmap = new Compressor(this)
                            .setMaxWidth(200)
                            .setMaxHeight(200)
                            .setQuality(75)
                            .compressToBitmap(thumb_filepath);

                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    thumb_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
                    final byte[] thumb_byte = byteArrayOutputStream.toByteArray();


                    final StorageReference filepath = mImageStorage.child("profile_images").child(current_user_id + ".jpg");
                    final StorageReference thumb_filepath_image = mImageStorage.child("profile_images").child("thumbs").child(current_user_id + ".jpg");

                    filepath.putFile(resultUri)
                            .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                @Override
                                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                    if (!task.isSuccessful()) {
                                        throw task.getException();
                                    }

                                    // Continue with the task to get the download URL
                                    return filepath.getDownloadUrl();
                                }
                            })
                            .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull final Task<Uri> task) {
                                    if (task.isSuccessful()) {
                                        final Uri downloadUri = task.getResult();
                                        //final String download_url = task.getResult().getDownloadUrl().toString();
                                        final String download_url = downloadUri.toString();
                                        UploadTask uploadTask = thumb_filepath_image.putBytes(thumb_byte);
                                        uploadTask
                                                .continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                                                    @Override
                                                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                                        if (!task.isSuccessful()) {
                                                            throw task.getException();
                                                        }

                                                        // Continue with the task to get the download URL
                                                        return thumb_filepath_image.getDownloadUrl();
                                                    }
                                                })
                                                .addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Uri> innertask) {
                                                        Uri thumbdownloadUri = innertask.getResult();
                                                        String thumb_downloadUrl = thumbdownloadUri.toString();
                                                        //String thumb_downloadUrl=thumb_task.getResult().getUploadSessionUri().toString();
                                                        if (task.isSuccessful()) {
                                                            Map update_hashmap = new HashMap();
                                                            update_hashmap.put("profileImageUrl", download_url);
                                                            update_hashmap.put("thumb_image", thumb_downloadUrl);

                                                            mUserDatabase.updateChildren(update_hashmap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                    if (task.isSuccessful()) {
                                                                        mProgressDialog.dismiss();
                                                                        loading.setVisibility(View.VISIBLE);
                                                                        Glide
                                                                                .with(getApplicationContext())
                                                                                .load(download_url)
                                                                                .into(profileImageView);
                                                                        loading.setVisibility(View.GONE);
                                                                    }
                                                                }
                                                            });
                                                        } else {
                                                            mProgressDialog.hide();
                                                            Snackbar.make(rootLayout, "error in uploading thumbnail..", Snackbar.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });

                                    } else {
                                        Snackbar.make(rootLayout, "error in uploading....", Snackbar.LENGTH_LONG).show();
                                    }
                                }
                            });
                } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                    Exception error = result.getError();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
