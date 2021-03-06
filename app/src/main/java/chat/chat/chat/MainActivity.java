package chat.chat.chat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import chat.chat.ChatApp;
import chat.chat.R;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private DatabaseReference usersRef;
    private EditText user;
    private EditText password;
    private Button logIn,signUp;
    private TextView forgotPass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mAuth=FirebaseAuth.getInstance();
        user=(EditText)findViewById(R.id.user);
        password=(EditText)findViewById(R.id.password);
        dbRef = FirebaseDatabase.getInstance().getReference();
        usersRef= dbRef.child("Users");
        forgotPass= (TextView) findViewById(R.id.forgot_pass);
        forgotPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder dial=new AlertDialog.Builder(MainActivity.this);
                View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.email_input_layout,null);
                final EditText emailEdt= (EditText) dialogView.findViewById(R.id.email);
                dial.setView(dialogView);
                dial.setPositiveButton("Send", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        mAuth.sendPasswordResetEmail(emailEdt.getText().toString()).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                String message;
                                if(task.isSuccessful())
                                {
                                    message="Link sent to email";
                                }
                                else
                                {
                                    message="Some error occured";
                                }
                                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

                            }
                        });

                    }
                });
                dial.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
                dial.show();
            }
        });

        logIn=(Button)findViewById(R.id.logIn);
        logIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo info = cm.getActiveNetworkInfo();
                boolean isConnected = info != null && info.isConnectedOrConnecting();
                if (isConnected) {
                    String name, pass;
                    name = user.getText().toString();
                    pass = password.getText().toString();
                    final String pwd = pass;
                    if (TextUtils.isEmpty(name) || TextUtils.isEmpty(pass)) {
                        Toast.makeText(getApplicationContext(), "Empty Field!", Toast.LENGTH_LONG).show();
                    } else if (name.length() < 8) {
                        Toast.makeText(MainActivity.this, "Invalid Registration No.!", Toast.LENGTH_SHORT).show();
                    } else {
                        final ProgressDialog mProgress=new ProgressDialog(MainActivity.this);
                        mProgress.setTitle("Logging In");
                        mProgress.setMessage("Please Wait...");
                        mProgress.setCanceledOnTouchOutside(false);
                        mProgress.show();
//                      name = name + "@abc.com";

                        final String finalName = (name.indexOf("dir")==-1&&name.indexOf("fac")==-1)?name.toUpperCase():name;

                        Query q = usersRef.orderByChild("username").equalTo(finalName);

                        q.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                Users user=null;
                               for(DataSnapshot dataSnapshot1:dataSnapshot.getChildren())
                               {
                                   user=dataSnapshot1.getValue(Users.class);
                               }
                                if(user!=null) {
                                    final Users finalUser = user;
                                    mAuth.signInWithEmailAndPassword(user.getEmail(), pwd).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                        @Override
                                        public void onComplete(@NonNull Task<AuthResult> task) {

                                            mProgress.dismiss();
                                            if (task.isSuccessful()) {
                                                Intent i=new Intent(MainActivity.this, OptionsActivity.class);
                                                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                                startActivity(i);
                                                subscribe(finalUser);
                                                finish();
                                            } else {
                                                Toast.makeText(getApplicationContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                }
                                else
                                {
                                    mProgress.dismiss();
                                    Toast.makeText(MainActivity.this, "User Not Found!", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Not connected to the Internet!", Toast.LENGTH_SHORT).show();
                }
            }

        });
        signUp=(Button)findViewById(R.id.signUp);
        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i=new Intent(MainActivity.this,SignUpActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(i);
                finish();
            }
        });
    }

    private void subscribe(Users user) {
        if(user.getUsername().indexOf("fac")==-1&&user.getUsername().indexOf("dir")==-1) {
            FirebaseMessaging.getInstance().subscribeToTopic((user.getUsername().substring(0, 8)).toLowerCase());
            FirebaseMessaging.getInstance().subscribeToTopic("students");
        }
        else{
            if(user.getUsername().indexOf("fac")!=-1)
                FirebaseMessaging.getInstance().subscribeToTopic("faculty");
            else
                FirebaseMessaging.getInstance().subscribeToTopic("director");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
