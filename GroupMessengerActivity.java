package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StreamCorruptedException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.PriorityQueue;



import static android.content.ContentValues.TAG;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */

class PriorityMessage
{
    public float MsgID;
    public float seqnum;
    public boolean isPriorityFinal;
    public boolean isMessageDeliverable;
    public String Msg;


    PriorityMessage(float MsgID,float seqnum,String Msg) {
        this.MsgID = MsgID;
        this.seqnum = seqnum;
        this.isPriorityFinal = false;
        isMessageDeliverable = false;
        this.Msg = Msg;
    }
    public boolean equals(Object o)
    {
        PriorityMessage obj = (PriorityMessage)o;
        return (Float.compare(this.MsgID,obj.MsgID) == 0 );
    }

}

class PriorityComparator implements Comparator<PriorityMessage>
{
    @Override
    public int compare(PriorityMessage x, PriorityMessage y)
    {
        return (Float.compare(x.seqnum,y.seqnum));
    }
}
public class GroupMessengerActivity extends Activity
{

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final int REMOTE_PORT = 11108;
    static final int SERVER_PORT = 10000;

    float m_largestproposedseqno = 0.0f ;
    float m_largestagreedseqno = 0.0f;
    float m_processid = 0.0f;
    int m_insertseqno = 0 ;
    float m_messgeId = 0.0f;
    String m_failedprocessport ="";


    String [] m_RemotePort;
    PriorityQueue<PriorityMessage> pqueue ;


    private void Initialize()
    {
        pqueue = new PriorityQueue<PriorityMessage>(40,new PriorityComparator());
        m_RemotePort = new String[5];
        for(int i = 0 ;i<5;++i)
        {
            m_RemotePort[i] = String.valueOf(i * 4 + REMOTE_PORT);
            Log.e(TAG, "m_RemotePort = "+m_RemotePort[i]);
        }

    }


    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "On Create Enter");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        int port  =  Integer.parseInt(portStr);
        //Process Ids suffix range is[0.0,0.4]
        m_processid = (port - 5554)/20.0f;
        m_messgeId+=m_processid;


        Log.e(TAG, "SeqNUMatStart="+String.valueOf(m_largestproposedseqno));

        final String myPort = String.valueOf(port * 2);
        Initialize();

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */


        final StringBuilder msg = new StringBuilder();
        final TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText = (EditText) findViewById(R.id.editText1);

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        findViewById(R.id.button4).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String msg = editText.getText().toString() + "\n"  ;
                        editText.setText(""); // This is one way to reset the input box


                        Log.e(TAG, "On Click Check1");
                        new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
                        Log.e(TAG, "On Click Check2");

                    }
                }

        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {


        private Void DeliverMessagesFromQueue(PriorityMessage MsgObj)
        {

            Log.e(TAG, "Enter DeliverMessagesFromQueue-Server");

            String seqnostr = "";
            String datastr = "";
            Log.e(TAG, "Poll=" +String.valueOf(MsgObj.seqnum) + "  " + MsgObj.Msg + String.valueOf(MsgObj.isMessageDeliverable));
            seqnostr = String.valueOf(m_insertseqno);
            datastr = MsgObj.Msg;
            datastr = datastr.replace("\n", "");
            insert(seqnostr, datastr);
            //query(seqnostr, datastr);
            m_insertseqno++;

            return null;
        }

        private Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }

        private void insert(String Key, String Msg) {
            ContentResolver objContentResolver = getContentResolver();
            Uri objUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            try {
                Log.e(TAG, "Insert Method Enter");
                ContentValues cv = new ContentValues();
                cv.put("key", Key);
                cv.put("value", Msg);
                Log.e(TAG, "check1");
                objContentResolver.insert(objUri, cv);
                Log.e(TAG, "check2");
            } catch (Exception e) {
                Log.e(TAG, "Insert Method Exception");
            }

        }

        private void query(String key, String value) {
            ContentResolver objContentResolver = getContentResolver();
            Uri objUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
            Cursor c = objContentResolver.query(objUri, null, key, null, null);
            Log.e(TAG, "Query method check1");
            try {
                if (c == null) {
                    Log.e(TAG, "Cursor is NULL");
                }
                int keyIndex = c.getColumnIndex("key");
                int valueIndex = c.getColumnIndex("value");
                Log.e(TAG, String.valueOf(keyIndex));
                Log.e(TAG, String.valueOf(valueIndex));
                c.moveToFirst();
                Log.e(TAG, "Move to First");
                String returnKey = c.getString(keyIndex);
                String returnValue = c.getString(valueIndex);

                if (returnKey.equals(key) && returnValue.equals(value)) {
                    Log.e(TAG, "(key, value) pairs match\n");
                    c.close();
                } else {
                    Log.e(TAG, "(key, value) pairs don't match\n");
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        private  Void OnReceivingMsgFirstTime(BufferedWriter bw,float msgid,String msg)
        {

            try
            {
                m_largestproposedseqno = Math.max(m_largestproposedseqno,m_largestagreedseqno)+1;
                float seqnumsend = m_largestproposedseqno+m_processid;
                PriorityMessage msgobj = new PriorityMessage(msgid,seqnumsend,msg);
                pqueue.add(msgobj);
                String SendMsg =  msgid+":"+String.valueOf(seqnumsend);
                Log.e(TAG, "Enter OnReceivingMsgFirstTime " +SendMsg);
                bw.write(SendMsg);
                bw.flush();
            }
            catch (SocketTimeoutException e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime SocketTimedout");
            }
            catch (EOFException e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime EOFException ");
            }
            catch(StreamCorruptedException e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime StreamCorruptedException");
            }
            catch (UnknownHostException e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime  UnknownHostException" );
            }
            catch (FileNotFoundException e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime FileNotFoundException");
            }
            catch (IOException e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime IOException");
            }
            catch(Exception e)
            {
                Log.e(TAG, "OnReceivingMsgFirstTime Exception");
            }
            Log.e(TAG, "Exit OnReceivingMsgFirstTime -Server");
            return null;
        }

        private Void SetAgreedSeqNumForMessagesInQueue(BufferedWriter bw,float msgid,float agreedseqnum,String failedprocessport,String msg,boolean flag)
        {
            try
            {
                bw.write("PA-OK");
                bw.flush();
                PriorityMessage finalmsgobj = new PriorityMessage(msgid,agreedseqnum,msg);
                finalmsgobj.isPriorityFinal = true;

                Log.e(TAG, "MSG@=" +String.valueOf(finalmsgobj.MsgID)+":"+ finalmsgobj.Msg + ":"+String.valueOf(finalmsgobj.isPriorityFinal) + ":" + String.valueOf(finalmsgobj.seqnum)+"failedport---:"+failedprocessport);

                    Log.e(TAG, "entry");

                    if(flag) {
                        m_largestagreedseqno = Math.max(m_largestagreedseqno, (float) Math.floor((double) agreedseqnum));
                        boolean removeresult = pqueue.remove(finalmsgobj);
                        Log.e(TAG,"removeresult=" + String.valueOf(removeresult));
                        finalmsgobj.isMessageDeliverable = true;
                        pqueue.add(finalmsgobj);
                    }
                    while(!pqueue.isEmpty()) {
                        Log.e(TAG, "while");
                        PriorityMessage obj = pqueue.peek();

                        if(obj.isMessageDeliverable)
                        {
                            DeliverMessagesFromQueue(pqueue.poll());
                        }
                        else if(!obj.isMessageDeliverable && null != failedprocessport && !failedprocessport.isEmpty())
                        {

                            float msgnum = (float)Math.floor((double)obj.MsgID);
                            float failedprocessid = ((Float.parseFloat(failedprocessport)/2)-5554)/20.0f;
                            float msgidnum =  msgnum +failedprocessid;

                            Log.e(TAG, "enter elseif failed msg id="+ String.valueOf(msgidnum)+"process id:"+String.valueOf(failedprocessid));
                            if (Float.compare(obj.MsgID,msgidnum) == 0) {
                                Log.e(TAG, "enter remove");
                                pqueue.remove();
                                failedprocessport = "";
                            }
                            else
                            {
                                break;
                            }

                        }
                        else
                        {

                            Log.e(TAG, "enter else break;");
                            break;
                        }
                    }

            }

            catch (SocketTimeoutException e)
            {
                Log.e(TAG, "SocketTimeoutException SetFinalPriorityForMessagesInQueue ");
            }
            catch (EOFException e)
            {
                Log.e(TAG, "SetFinalPriorityForMessagesInQueue EOFException ");
            }
            catch(StreamCorruptedException e)
            {
                Log.e(TAG, "SetFinalPriorityForMessagesInQueue StreamCorruptedException");
            }
            catch (UnknownHostException e)
            {
                Log.e(TAG, "SetFinalPriorityForMessagesInQueue  UnknownHostException" );
            }
            catch (FileNotFoundException e)
            {
                Log.e(TAG, "SetFinalPriorityForMessagesInQueue FileNotFoundException");
            }
            catch (IOException e)
            {
                Log.e(TAG, "SetFinalPriorityForMessagesInQueue -Exception");
            }
            catch(Exception e)
            {
                Log.e(TAG,"Exception e");
            }


            return null;
        }

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            Log.e(TAG, "ServerTask Enter");
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            int cnt = 0 ;
            while (true)
            {
                cnt++;
                try {
                    Socket soc = serverSocket.accept();
                    soc.setSoTimeout(1500);
                    InputStreamReader isreader = new InputStreamReader(soc.getInputStream());
                    BufferedReader breader = new BufferedReader(isreader);
                    String datastr = breader.readLine();

                    OutputStreamWriter oswriter = new OutputStreamWriter(soc.getOutputStream());
                    BufferedWriter bwriter = new BufferedWriter(oswriter);

                    if(datastr != null) {
                        Log.e(TAG, "strrxed=" + datastr);

                        String[] arr = datastr.split(":");
                        boolean finalcheck = false;
                        float msgidrxed= 0 , seqnumrxed = 0;
                        String failedprocessport = null ;
                        boolean rxedIsPriorityFinal = false;
                        String msgrxed = null;
                        if (arr.length == 6)
                        {
                            finalcheck = Boolean.parseBoolean(arr[0]);
                            Log.e(TAG,"finalcheck="+String.valueOf(finalcheck));
                            msgidrxed = Float.parseFloat(arr[1]);
                            seqnumrxed = Float.parseFloat(arr[2]);
                            rxedIsPriorityFinal = Boolean.parseBoolean(arr[3]);
                            failedprocessport = (arr[4]);
                            msgrxed = arr[5];
                        }
                        if(msgrxed != null )
                        {
                            if(finalcheck)
                            {
                                SetAgreedSeqNumForMessagesInQueue(bwriter, msgidrxed, seqnumrxed, failedprocessport, msgrxed,false);
                            }
                            else
                            {
                                if (rxedIsPriorityFinal == false)
                                {
                                    OnReceivingMsgFirstTime(bwriter, msgidrxed, msgrxed);
                                } else
                                {
                                    SetAgreedSeqNumForMessagesInQueue(bwriter, msgidrxed, seqnumrxed, failedprocessport, msgrxed,true);
                                }

                            }
                        }
                    }
                    bwriter.close();
                    breader.close();
                    soc.close();
                }
                catch (SocketTimeoutException e)
                {
                    Log.e(TAG, "Server Task SocketTimedout");
                }
                catch (EOFException e)
                {
                    Log.e(TAG, "EOFException ");
                }
                catch(StreamCorruptedException e)
                {
                    Log.e(TAG, "StreamCorruptedException");
                }
                catch (UnknownHostException e)
                {
                    Log.e(TAG, "ServerTask UnknownHostException" );
                }
                catch (FileNotFoundException e)
                {
                    Log.e(TAG, "ServerTask FileNotFoundException");
                }
                catch (IOException e)
                {
                    Log.e(TAG, "ServerTask IOException");
                }

            }

        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            Log.e(TAG, "On Progress Update:");
            String strReceived = strings[0].trim();
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(strReceived + "\t\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */
            return;
        }

    }

    private class ClientTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... msgs) {
            Log.e(TAG, "ClientTask Enter");

            String msgToSend = msgs[0];

            float rxedMsgId = 0;
            float rxedSeqNum = 0;
            float maxSeqNum = 0;
            boolean isPriorityFinal = false;

            if(msgToSend != null) {

                m_messgeId = m_messgeId+1.0f;
                String SendMsg = String.valueOf(false)+":"+String.valueOf(m_messgeId) +":"+String.valueOf(0) + ":" + String.valueOf(isPriorityFinal) + ":" +m_failedprocessport+":"+msgToSend;

                Log.e(TAG, "ClientTask SendMsg =" + SendMsg);

                for (int i = 0; i < 5; ++i) {

                    try {
                        Log.e(TAG, "ClientTask check1-Multicast 1st time,Iteration = " + String.valueOf(i)+":"+m_RemotePort[i]);
                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                            Integer.parseInt(m_RemotePort[i]));
                        socket.setSoTimeout(1500);

                        OutputStreamWriter oswriter = new OutputStreamWriter(socket.getOutputStream());
                        BufferedWriter bwriter = new BufferedWriter(oswriter);
                        Log.e(TAG, SendMsg);
                        bwriter.write(SendMsg);
                        bwriter.flush();

                        InputStreamReader isreader = new InputStreamReader(socket.getInputStream());
                        BufferedReader breader = new BufferedReader(isreader);
                        String str = breader.readLine();

                        if(str != null) {
                            Log.e(TAG, "str Rxed at client = " + str);
                            String[] temp = str.split(":");
                            if(temp.length == 2) {
                                rxedMsgId = Float.parseFloat(temp[0]);
                                rxedSeqNum = Float.parseFloat(temp[1]);
                            }
                        }
                        else
                        {
                            Log.e(TAG, "NULL Exception avd ="+m_RemotePort[i]);
                            m_failedprocessport = m_RemotePort[i];
                        }

                        bwriter.close();
                        breader.close();
                        socket.close();

                        Log.e(TAG, "Priority Rxed = " + String.valueOf(rxedSeqNum));

                        if(rxedSeqNum > maxSeqNum)
                        {
                            maxSeqNum = rxedSeqNum;
                        }


                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "ClientTask SocketTimedout-Multicast 1st time");
                    } catch (EOFException e) {
                        Log.e(TAG, "EOFException -Multicast 1st time");
                    } catch (StreamCorruptedException e) {
                        Log.e(TAG, "StreamCorruptedException -Multicast 1st time");
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException -Multicast 1st time");
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "ClientTask FileNotFoundException -Multicast 1st time");
                    } catch (IOException e) {
                        m_failedprocessport = m_RemotePort[i];
                        Log.e(TAG, "ClientTask socket IOException- Multicast 1st time");
                    }
                }

                isPriorityFinal = true;
                SendMsg = String.valueOf(false)+":"+String.valueOf(rxedMsgId) +":"+String.valueOf(maxSeqNum) + ":" + String.valueOf(isPriorityFinal) + ":" + m_failedprocessport+":"+msgToSend;

                for (int i = 0; i < 5; ++i) {
                    try {
                        Log.e(TAG, "ClientTask check1-Multicast 2nd time,Iteration = " + String.valueOf(i)+":"+m_RemotePort[i]);

                        Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                Integer.parseInt(m_RemotePort[i]));
                        socket.setSoTimeout(1500);


                        OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
                        BufferedWriter bwriter = new BufferedWriter(out);
                        bwriter.write(SendMsg);
                        bwriter.flush();


                        Log.e(TAG, "Max Priority Sent = " + String.valueOf(maxSeqNum));


                        InputStreamReader in = new InputStreamReader(socket.getInputStream());
                        BufferedReader breader = new BufferedReader(in);
                        String ack = breader.readLine();

                        Log.e(TAG, "Check excep2");
                        if ( ack != null) {
                            Log.e(TAG, "PA-OK");
                        }
                        else
                        {
                            Log.e(TAG, "NULL Exception 2nd time avd="+m_RemotePort[i]);
                            m_failedprocessport = m_RemotePort[i];
                        }
                        bwriter.close();
                        breader.close();
                        socket.close();

                    } catch (SocketTimeoutException e) {
                        Log.e(TAG, "ClientTask SocketTimedout-Multicast 2nd time port");
                    } catch (EOFException e) {
                        Log.e(TAG, "EOFException -Multicast 2nd time");
                    } catch (StreamCorruptedException e) {
                        Log.e(TAG, "StreamCorruptedException -Multicast 2nd time");
                    } catch (UnknownHostException e) {
                        Log.e(TAG, "ClientTask UnknownHostException -Multicast 2nd time");
                    } catch (FileNotFoundException e) {
                        Log.e(TAG, "ClientTask FileNotFoundException -Multicast 2nd time");
                    } catch (IOException e) {
                        m_failedprocessport = m_RemotePort[i];
                        Log.e(TAG, "ClientTask socket IOException- Multicast 2nd time"+m_failedprocessport);
                    }

                }


                SendMsg = String.valueOf(true)+":"+String.valueOf(rxedMsgId) +":"+String.valueOf(maxSeqNum) + ":" + String.valueOf(isPriorityFinal) + ":" + m_failedprocessport+":"+msgToSend;

                    for (int i = 0; i < 5; ++i) {
                        try {
                            Log.e(TAG, "ClientTask check1-Multicast 3rd time,Iteration = " + String.valueOf(i) + ":" + m_RemotePort[i]);

                            Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(m_RemotePort[i]));
                            socket.setSoTimeout(500);

                            OutputStreamWriter out = new OutputStreamWriter(socket.getOutputStream());
                            BufferedWriter bwriter = new BufferedWriter(out);
                            bwriter.write(SendMsg);
                            bwriter.flush();


                            Log.e(TAG, "Max Priority Sent 3rd time = " + String.valueOf(maxSeqNum));

                            InputStreamReader in = new InputStreamReader(socket.getInputStream());
                            BufferedReader breader = new BufferedReader(in);
                            String ack = breader.readLine();

                            if (ack != null ) {
                                Log.e(TAG, "PA-OK");
                            }
                            else
                            {
                                Log.e(TAG, "NULL Exception 3rd time avd="+m_RemotePort[i]);
                                m_failedprocessport = m_RemotePort[i];
                            }
                            bwriter.close();
                            breader.close();
                            socket.close();

                        } catch (SocketTimeoutException e) {
                            Log.e(TAG, "ClientTask SocketTimedout-Multicast 3rd time port");
                        } catch (EOFException e) {
                            Log.e(TAG, "EOFException -Multicast 3rd time");
                        } catch (StreamCorruptedException e) {
                            Log.e(TAG, "StreamCorruptedException -Multicast 3rd time");
                        } catch (UnknownHostException e) {
                            Log.e(TAG, "ClientTask UnknownHostException -Multicast 3rd time");
                        } catch (FileNotFoundException e) {
                            Log.e(TAG, "ClientTask FileNotFoundException -Multicast 3rd time");
                        } catch (IOException e) {
                            m_failedprocessport = m_RemotePort[i];
                            Log.e(TAG, "ClientTask socket IOException- Multicast 3rd time" + m_failedprocessport);
                        }

                    }
            }
            return null;
        }

    }
}