package com.passthejams.app;

import android.app.Activity;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

import static com.passthejams.app.R.id.text_network_response;

public class network_test extends Activity {
    int nExceptions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_test);

        Button button = (Button) findViewById(R.id.button_connect);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText edit_host = (EditText) findViewById(R.id.host);
                EditText edit_port = (EditText) findViewById(R.id.port);
                TextView response = (TextView) findViewById(R.id.text_network_response);
                response.setText(edit_host.getText() + "\n" + edit_port.getText());

                int port = 80;
                String host = edit_host.getText().toString();


                try {
                    port = Integer.parseInt(edit_port.getText().toString());
                } catch (NumberFormatException e) {}

                String line = "";
                try {
                    line = new getNetwork().execute(host, port).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    line = e.getMessage();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                    line = e.getMessage();
                }

                response.setText(line);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_network_test, menu);
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

    private class getNetwork extends AsyncTask<Object,Void,String>
    {
        protected String doInBackground(Object... params) {
            String host = (String) params[0];
            int port = (Integer) params[1];
            String line = "";

            try {
                Socket sock = new Socket(host, port);
                PrintWriter out =
                        new PrintWriter(sock.getOutputStream(), true);
                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(sock.getInputStream()));
                line = in.readLine();


                in.close();
                out.close();
                sock.close();

            } catch (UnknownHostException e) {
                line = ""+ ++nExceptions+e.getMessage();
                e.printStackTrace();
            } catch (IOException e) {
                line = ""+ ++nExceptions+e.getMessage();
                e.printStackTrace();
            }
            return line;
        }
    }
}
